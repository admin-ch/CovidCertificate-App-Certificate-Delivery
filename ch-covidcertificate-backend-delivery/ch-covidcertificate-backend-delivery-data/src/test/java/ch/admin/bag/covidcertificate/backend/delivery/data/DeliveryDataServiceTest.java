/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.covidcertificate.backend.delivery.data.config.DeliveryDataServiceConfig;
import ch.admin.bag.covidcertificate.backend.delivery.data.config.FlyWayConfig;
import ch.admin.bag.covidcertificate.backend.delivery.data.config.PostgresDataConfig;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.PostgresDbCleaner;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@ExtendWith({SpringExtension.class, PostgresDbCleaner.class})
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {PostgresDataConfig.class, FlyWayConfig.class, DeliveryDataServiceConfig.class})
@ActiveProfiles("postgres")
@TestPropertySource(properties = {"push.batchsize=3"})
class DeliveryDataServiceTest {

    public static final String CODE = "1A2B3C4D5";
    public static final String PUBLIC_KEY = "public_key";
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired private DeliveryDataService deliveryDataService;

    @Value("${push.batchsize}")
    private int batchsize;

    @Test
    void testInitTransfer() throws Exception {
        // init transfer
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration);

        // attempt to init transfer with existing code
        assertThrows(
                CodeAlreadyExistsException.class,
                () -> deliveryDataService.initTransfer(registration));

        // init transfer with different code
        DeliveryRegistration otherRegistration = getDeliveryRegistration("OTHER");
        deliveryDataService.initTransfer(otherRegistration);
    }

    private DeliveryRegistration getDeliveryRegistration(String code) {
        DeliveryRegistration registration = new DeliveryRegistration();
        registration.setCode(code);
        registration.setPublicKey(PUBLIC_KEY);
        registration.setAlgorithm(Algorithm.EC256);
        return registration;
    }

    @Test
    void testFindCovidCode() throws Exception {
        // init transfer
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration);

        // find covid certs
        List<CovidCert> covidCerts = deliveryDataService.findCovidCerts(CODE);
        assertTrue(covidCerts.isEmpty());

        // find covid certs unknown code
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts("XYZ"));
    }

    @Test
    void testCloseTransfer() throws Exception {
        // init transfer
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration);

        // close transfer
        deliveryDataService.closeTransfer(CODE);

        // check transfer is closed
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts(CODE));
        // code is available again
        deliveryDataService.initTransfer(registration);
    }

    @Test
    void testPushRegistration() throws Exception {
        // insert push registration
        PushRegistration pushRegistration = new PushRegistration();
        String pushToken = "push_token";
        pushRegistration.setPushToken(pushToken);
        pushRegistration.setPushType(PushType.IOS);
        deliveryDataService.insertPushRegistration(pushRegistration);

        // check push registration added
        List<PushRegistration> pushRegistrations =
                deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert same push registration again
        deliveryDataService.insertPushRegistration(pushRegistration);

        // check no change
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert another push registration
        PushRegistration anotherPushRegistration = new PushRegistration();
        anotherPushRegistration.setPushToken("another_push_token");
        anotherPushRegistration.setPushType(PushType.IOS);
        deliveryDataService.insertPushRegistration(anotherPushRegistration);

        // check push registration added
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(2, pushRegistrations.size());

        // remove push registration
        deliveryDataService.removeRegistrations(List.of(pushRegistration.getPushToken()));

        // check push registration removed
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(anotherPushRegistration, pushRegistrations.get(0));

        // check push registration pk_id
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 100);
        assertTrue(pushRegistrations.isEmpty());
    }

    @Test
    void testPushRegistrationOrdering() {
        // TODO Write similar to silent push
        for (var i = 0; i < 20; i++) {
            PushRegistration pushRegistration = new PushRegistration();
            String pushToken = "push_token_" + i;
            pushRegistration.setPushToken(pushToken);
            pushRegistration.setPushType(PushType.IOS);
            deliveryDataService.insertPushRegistration(pushRegistration);
        }
        int prevMaxId = 0, nextMaxId = 0;
        List<PushRegistration> registrationList;
        do {
            registrationList =
                    deliveryDataService.getPushRegistrationByType(PushType.IOS, prevMaxId);
            assertTrue(registrationList.size() <= batchsize);
            for (PushRegistration pushRegistration : registrationList) {
                final var id = pushRegistration.getId();
                assertTrue(
                        prevMaxId < id,
                        String.format(
                                "prevMaxId: %s, nextMaxId: %s, batchsize: %s, but id: %s",
                                prevMaxId, nextMaxId, batchsize, id));
                nextMaxId = Math.max(nextMaxId, id);
            }
            prevMaxId = nextMaxId;
        } while (!registrationList.isEmpty());
    }

    private void assertPushRegistration(PushRegistration expected, PushRegistration actual) {
        assertEquals(expected.getPushToken(), actual.getPushToken());
        assertEquals(expected.getPushType(), actual.getPushType());
    }
}
