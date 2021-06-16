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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@ExtendWith({SpringExtension.class, PostgresDbCleaner.class})
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {PostgresDataConfig.class, FlyWayConfig.class, DeliveryDataServiceConfig.class})
@ActiveProfiles("postgres")
public class DeliveryDataServiceTest {

    @Autowired private DeliveryDataService deliveryDataService;

    public static final String CODE = "1A2B3C4D5";
    public static final String PUBLIC_KEY = "public_key";

    @Test
    public void testInitTransfer() throws Exception {
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
    public void testFindCovidCode() throws Exception {
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
    public void testCloseTransfer() throws Exception {
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
    public void testPushRegistration() throws Exception {
        // insert push registration
        PushRegistration pushRegistration = new PushRegistration();
        String pushToken = "push_token";
        pushRegistration.setPushToken(pushToken);
        pushRegistration.setPushType(PushType.IOS);
        deliveryDataService.insertPushRegistration(pushRegistration);

        // check push registration added
        List<PushRegistration> pushRegistrations = deliveryDataService.findAllPushRegistrations();
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert same push registration again
        deliveryDataService.insertPushRegistration(pushRegistration);

        // check no change
        pushRegistrations = deliveryDataService.findAllPushRegistrations();
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert another push registration
        PushRegistration anotherPushRegistration = new PushRegistration();
        anotherPushRegistration.setPushToken("another_push_token");
        anotherPushRegistration.setPushType(PushType.IOS);
        deliveryDataService.insertPushRegistration(anotherPushRegistration);

        // check push registration added
        pushRegistrations = deliveryDataService.findAllPushRegistrations();
        assertEquals(2, pushRegistrations.size());

        // remove push registration
        deliveryDataService.removePushRegistration(pushRegistration);

        // check push registration removed
        pushRegistrations = deliveryDataService.findAllPushRegistrations();
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(anotherPushRegistration, pushRegistrations.get(0));
    }

    private void assertPushRegistration(PushRegistration expected, PushRegistration actual) {
        assertEquals(expected.getPushToken(), actual.getPushToken());
        assertEquals(expected.getPushType(), actual.getPushType());
    }
}
