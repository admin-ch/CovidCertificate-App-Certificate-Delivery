package ch.admin.bag.covidcertificate.backend.delivery.ws.service;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.impl.PushRegistrationWrapper;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.proxy.HttpProxyHandlerFactory;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends periodic silent pushes to all iOS devices. This should wake up the app and let the app sync
 * problematic events. (Note: Only EN disabled apps register with our server.)
 *
 * @author meinen
 */
public class IOSHeartbeatSilentPush {

    private static final Logger logger = LoggerFactory.getLogger(IOSHeartbeatSilentPush.class);

    private final DeliveryDataService pushRegistrationDataService;
    private final String topic;
    private final byte[] signingKey;
    private final String teamId;
    private final String keyId;
    private ApnsClient apnsClient;
    private ApnsClient apnsClientSandbox;

    public IOSHeartbeatSilentPush(
            final DeliveryDataService pushRegistrationDataService,
            byte[] signingKey,
            String teamId,
            String keyId,
            String topic) {
        this.pushRegistrationDataService = pushRegistrationDataService;
        this.teamId = teamId;
        this.signingKey = signingKey;
        this.keyId = keyId;
        this.topic = topic;
    }

    @PostConstruct
    private void initApnsClients() {
        try {
            final var inputStream = new ByteArrayInputStream(signingKey);
            var key = ApnsSigningKey.loadFromInputStream(inputStream, teamId, keyId);
            inputStream.close();

            this.apnsClient =
                    new ApnsClientBuilder()
                            .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
                            .setSigningKey(key)
                            .setProxyHandlerFactory(
                                    HttpProxyHandlerFactory.fromSystemProxies(
                                            ApnsClientBuilder.PRODUCTION_APNS_HOST))
                            .build();

            this.apnsClientSandbox =
                    new ApnsClientBuilder()
                            .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                            .setSigningKey(key)
                            .setProxyHandlerFactory(
                                    HttpProxyHandlerFactory.fromSystemProxies(
                                            ApnsClientBuilder.DEVELOPMENT_APNS_HOST))
                            .build();
        } catch (InvalidKeyException
                | NoSuchAlgorithmException
                | IOException
                | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendHeartbeats() {
        logger.info("Send iOS heartbeat push");
        logger.info("Load tokens from database batch-wise.");
        var maxId = 0;

        while (true) {
            final var iodRegistrationWrappers =
                    pushRegistrationDataService.getPushRegistrationByType(PushType.IOD, maxId);
            final var iosRegistrationWrappers =
                    pushRegistrationDataService.getPushRegistrationByType(PushType.IOS, maxId);
            logger.info(
                    "Found {} iOD and {} iOS push tokens.",
                    iodRegistrationWrappers.size(),
                    iosRegistrationWrappers.size());
            if (!iodRegistrationWrappers.isEmpty() || !iosRegistrationWrappers.isEmpty()) {
                Set<String> iodPushTokens = new HashSet<>();
                maxId = getTokens(maxId, iodRegistrationWrappers, iodPushTokens);
                Set<String> iosPushTokens = new HashSet<>();
                maxId = getTokens(maxId, iosRegistrationWrappers, iosPushTokens);
                sendPushNotificationsBatch(iodPushTokens, iosPushTokens);
            } else {
                break;
            }
        }
        logger.info("iOS hearbeat push done");
    }

    private int getTokens(
            int maxId,
            List<PushRegistrationWrapper> iodRegistrationWrappers,
            Set<String> iodPushTokens) {
        for (PushRegistrationWrapper pushRegistrationWrapper : iodRegistrationWrappers) {
            iodPushTokens.add(pushRegistrationWrapper.getPushRegistration().getPushToken());
            final var id = pushRegistrationWrapper.getId();
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId;
    }

    private void sendPushNotificationsBatch(Set<String> iodPushTokens, Set<String> iosPushTokens) {
        final var appleIodPushData = createAppleSilentPushNotifications(iodPushTokens);
        final var appleIosPushData = createAppleSilentPushNotifications(iosPushTokens);

        List<
                        PushNotificationFuture<
                                ApnsPushNotification,
                                PushNotificationResponse<ApnsPushNotification>>>
                responseList = new ArrayList<>();
        for (ApnsPushNotification notification : appleIosPushData) {
            PushNotificationFuture<
                            ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>
                    f = apnsClient.sendNotification(notification);
            responseList.add(f);
        }
        for (ApnsPushNotification notification : appleIodPushData) {
            PushNotificationFuture<
                            ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>
                    f = apnsClientSandbox.sendNotification(notification);
            responseList.add(f);
        }

        List<String> tokensToRemove = new ArrayList<>();
        for (PushNotificationFuture<
                        ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>
                f : responseList) {
            try {
                PushNotificationResponse<ApnsPushNotification> r = f.get();
                if (!r.isAccepted()
                        && r.getRejectionReason() != null
                        && (r.getRejectionReason().equals("BadDeviceToken")
                                || r.getRejectionReason().equals("Unregistered")
                                || r.getRejectionReason().equals("DeviceTokenNotForTopic"))) {
                    // collect bad/unregistered device tokens and remove from database
                    tokensToRemove.add(r.getPushNotification().getToken());
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.info("Exception waiting for response. Continue", e);
            }
        }

        logger.info(
                "All notification sent. Got {} invalid tokens to remove", tokensToRemove.size());
        pushRegistrationDataService.removeRegistrations(tokensToRemove);
    }

    private List<ApnsPushNotification> createAppleSilentPushNotifications(
            final Set<String> applePushTokens) {
        List<ApnsPushNotification> result = new ArrayList<>();
        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setContentAvailable(true);
        String payload = payloadBuilder.build();
        for (String token : applePushTokens) {
            var notification =
                    new SimpleApnsPushNotification(
                            token,
                            this.topic,
                            payload,
                            null,
                            DeliveryPriority.CONSERVE_POWER,
                            com.eatthepath.pushy.apns.PushType.BACKGROUND);
            result.add(notification);
        }
        return result;
    }
}
