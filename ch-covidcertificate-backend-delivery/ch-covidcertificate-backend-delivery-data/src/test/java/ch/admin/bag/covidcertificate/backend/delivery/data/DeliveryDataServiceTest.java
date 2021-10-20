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
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.PublicKeyAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.CodeGenerator;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.PostgresDbCleaner;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.RandomGenerator;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import java.time.Duration;
import java.time.Instant;
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

    public static final String CODE = CodeGenerator.generateCode();
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired private DeliveryDataService deliveryDataService;

    @Value("${push.batchsize}")
    private int batchsize;

    @Test
    void testInitTransfer() throws Exception {
        final String publicKey = "public_key";
        // init transfer
        DeliveryRegistration registration = getDeliveryRegistration(CODE, publicKey);
        Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
        Instant failsAt = Instant.now().plus(Duration.ofDays(33));
        deliveryDataService.initTransfer(registration, expiresAt, failsAt);

        // attempt to init transfer with existing code
        assertThrows(
                CodeAlreadyExistsException.class,
                () -> deliveryDataService.initTransfer(registration, expiresAt, failsAt));

        // test public key uniqueness
        assertThrows(
                PublicKeyAlreadyExistsException.class,
                () ->
                        deliveryDataService.initTransfer(
                                getDeliveryRegistration(CodeGenerator.generateCode(), publicKey), expiresAt, failsAt));

        // init transfer with different code
        DeliveryRegistration otherRegistration =
                getDeliveryRegistration("OTHER", "other_public_key");
        deliveryDataService.initTransfer(otherRegistration, expiresAt, failsAt);
    }

    private DeliveryRegistration getDeliveryRegistration(String code, String publicKey) {
        DeliveryRegistration registration = new DeliveryRegistration();
        registration.setCode(code);
        registration.setPublicKey(publicKey);
        registration.setAlgorithm(Algorithm.EC256);
        return registration;
    }

    private DeliveryRegistration getDeliveryRegistration(String code) {
        return getDeliveryRegistration(code, RandomGenerator.randomAlphaNumericString());
    }

    @Test
    void testFindCovidCode() throws Exception {
        // init transfer
        Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
        Instant failsAt = Instant.now().plus(Duration.ofDays(30));
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration, expiresAt, failsAt);

        // find covid certs
        List<CovidCert> covidCerts = deliveryDataService.findCovidCerts(CODE);
        assertTrue(covidCerts.isEmpty());

        // find covid certs unknown code
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts("XYZ"));
    }

    @Test
    void testCloseTransfer() throws Exception {
        // init transfer
        Instant expiresAt = Instant.now().plus(Duration.ofDays(30));
        Instant failsAt = Instant.now().plus(Duration.ofDays(30));
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration, expiresAt, failsAt);

        // close transfer
        deliveryDataService.closeTransfer(CODE);

        // check transfer is closed
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts(CODE));
        // code is available again
        deliveryDataService.initTransfer(registration, expiresAt, failsAt);
    }

    @Test
    void testPushRegistration() throws Exception {
        // insert push registration
        PushRegistration pushRegistration = new PushRegistration();
        pushRegistration.setPushToken("push_token");
        pushRegistration.setPushType(PushType.IOS);
        pushRegistration.setRegisterId("register_id");
        deliveryDataService.upsertPushRegistration(pushRegistration);

        // check push registration added
        List<PushRegistration> pushRegistrations =
                deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert same push registration again
        deliveryDataService.upsertPushRegistration(pushRegistration);

        // check no change
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertEquals(1, pushRegistrations.size());
        assertPushRegistration(pushRegistration, pushRegistrations.get(0));

        // insert another push registration
        PushRegistration anotherPushRegistration = new PushRegistration();
        anotherPushRegistration.setPushToken("another_push_token");
        anotherPushRegistration.setPushType(PushType.IOS);
        anotherPushRegistration.setRegisterId("another_register_id");
        deliveryDataService.upsertPushRegistration(anotherPushRegistration);

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

        // remove another push registration
        anotherPushRegistration.setPushToken("");
        deliveryDataService.upsertPushRegistration(anotherPushRegistration);

        // check push registration removed
        pushRegistrations = deliveryDataService.getPushRegistrationByType(PushType.IOS, 0);
        assertTrue(pushRegistrations.isEmpty());
    }

    @Test
    void testPushRegistrationOrdering() {
        for (var i = 0; i < 20; i++) {
            PushRegistration pushRegistration = new PushRegistration();
            String pushToken = "push_token_" + i;
            String registerId = "register_id_" + i;
            pushRegistration.setPushToken(pushToken);
            pushRegistration.setPushType(PushType.IOS);
            pushRegistration.setRegisterId(registerId);
            deliveryDataService.upsertPushRegistration(pushRegistration);
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

    @Test
    void testBatchedPushRegistrations(){

        for (var i = 0; i < 20; i++) {
            PushRegistration pushRegistration = new PushRegistration();
            String pushToken = "push_token_" + i;
            String registerId = "register_id_" + i;
            pushRegistration.setPushToken(pushToken);
            pushRegistration.setPushType(PushType.IOS);
            pushRegistration.setRegisterId(registerId);
            deliveryDataService.upsertPushRegistration(pushRegistration);
        }
        List<PushRegistration> max_10_registrations = deliveryDataService.getDuePushRegistrations(PushType.IOS, Duration.ofHours(1), 10);
        assertEquals(10, max_10_registrations.size());
        deliveryDataService.updateLastPushTImes(max_10_registrations);
        List<PushRegistration> not_recently_pushed = deliveryDataService.getDuePushRegistrations(PushType.IOS, Duration.ofHours(1), 1000);
        assertEquals(10, not_recently_pushed.size());
        deliveryDataService.updateLastPushTImes(not_recently_pushed);
        List<PushRegistration> none_left = deliveryDataService.getDuePushRegistrations(PushType.IOS, Duration.ofHours(1), 1000);
        assertEquals(0, none_left.size());
    }

    private void assertPushRegistration(PushRegistration expected, PushRegistration actual) {
        assertEquals(expected.getPushToken(), actual.getPushToken());
        assertEquals(expected.getPushType(), actual.getPushType());
        assertEquals(expected.getRegisterId(), actual.getRegisterId());
    }

    @Test
    void testCleanDB() throws Exception {
        // init transfer
        Instant expiresAt = Instant.now().minus(Duration.ofDays(4));
        Instant failsAt = Instant.now().minus(Duration.ofDays(1));
        DeliveryRegistration registration = getDeliveryRegistration(CODE);
        deliveryDataService.initTransfer(registration, expiresAt, failsAt);
        // insert covid cert
        var dbCovidCert = new DbCovidCert();
        dbCovidCert.setFkTransfer(deliveryDataService.findPkTransferId(CODE));
        dbCovidCert.setEncryptedHcert("hcert");
        dbCovidCert.setEncryptedPdf("pdf");
        deliveryDataService.insertCovidCert(dbCovidCert);
        assertEquals(1, deliveryDataService.findCovidCerts(CODE).size());
        // delete everything
        deliveryDataService.cleanDB();
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findTransfer(CODE));
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts(CODE));
    }

    @Test
    void testRegistrationCount() throws Exception{
        for (var i = 0; i < 20; i++) {
            PushRegistration pushRegistration = new PushRegistration();
            String pushToken = "push_token_" + i;
            String registerId = "register_id_" + i;
            pushRegistration.setPushToken(pushToken);
            pushRegistration.setPushType(PushType.IOS);
            pushRegistration.setRegisterId(registerId);
            deliveryDataService.upsertPushRegistration(pushRegistration);
        }
        int count = deliveryDataService.countRegistrations();
        assertEquals(20, count);
    }
}
