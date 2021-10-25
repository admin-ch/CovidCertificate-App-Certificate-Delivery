package ch.admin.bag.covidcertificate.backend.delivery.ws.service;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
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
import java.time.Duration;
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
public class IosHeartbeatSilentPushImpl implements IosHeartbeatSilentPush {

    private static final Logger logger = LoggerFactory.getLogger(IosHeartbeatSilentPushImpl.class);

    private final DeliveryDataService pushRegistrationDataService;
    private final String topic;
    private final byte[] signingKey;
    private final String teamId;
    private final String keyId;
    private ApnsClient apnsClient;
    private ApnsClient apnsClientSandbox;

    public IosHeartbeatSilentPushImpl(
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
        try (final var inputStream = new ByteArrayInputStream(signingKey)) {
            var key = ApnsSigningKey.loadFromInputStream(inputStream, teamId, keyId);

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

    public void sendHeartbeats(Duration pushInterval, int pushLimit) {
        logger.info("Send iOS heartbeat push");
        logger.info("Load batch of tokens from database");
        for (PushType pushType : PushType.values()) {
            List <PushRegistration> registrationList =
                pushRegistrationDataService.getDuePushRegistrations(pushType, pushInterval, pushLimit);//TODO make configurable
            logger.info("Retrieved {} {} push tokens", registrationList.size(), pushType);
            Set<String> pushTokens = new HashSet<>();
            registrationsToTokens(registrationList, pushTokens);
            sendPushNotificationsBatch(pushTokens, pushType);
            pushRegistrationDataService.updateLastPushTImes(registrationList);
        }
        logger.info("iOS hearbeat push done");
    }

    @Override
    public void checkPushSchedule(Duration pushInterval, Duration pushSchedule, int batchSize) {
        float loadFactor = IosHeartbeatSilentPush.calculatePushLoadFactor(pushInterval, pushSchedule, batchSize, pushRegistrationDataService.countRegistrations());
        if(loadFactor > 0.8){
            logger.warn("iOS silent push schedule is about to overflow. Consider increasing batch sie or scheduler frequency. Load factor (overflow at 1.0): {}", loadFactor);
        }else if(loadFactor >= 1.0){
            logger.error("iOS silent pushes can no longer be delivered with this schedule. Increase batch size or frequency. Load factor: {}", loadFactor);
        }else{
            logger.info("iOS silent push schedule checked successfully. Load factor: {}", loadFactor);
        }
    }


    /**
     * Helper function to fill the push tokens from the list of push registrations into a separate
     * set
     *
     * @return maximum id contained in the list of push registrations
     */
    private int registrationsToTokens(
            List<PushRegistration> pushRegistrations, Set<String> pushTokens) {
        var maxId = 0;
        for (PushRegistration pushRegistration : pushRegistrations) {
            pushTokens.add(pushRegistration.getPushToken());
            final var id = pushRegistration.getId();
            if (id > maxId) {
                maxId = id;
            }
        }
        return maxId;
    }

    private void sendPushNotificationsBatch(Set<String> iosPushTokens, PushType pushType) {
        final var appleIosPushData = createAppleSilentPushNotifications(iosPushTokens);

        List<
                        PushNotificationFuture<
                                ApnsPushNotification,
                                PushNotificationResponse<ApnsPushNotification>>>
                responseList = new ArrayList<>();
        ApnsClient client;
        if (PushType.IOS.equals(pushType)) {
            client = apnsClient;
        } else {
            client = apnsClientSandbox;
        }
        for (ApnsPushNotification notification : appleIosPushData) {
            PushNotificationFuture<
                            ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>
                    f = client.sendNotification(notification);
            responseList.add(f);
        }

        List<String> tokensToRemove = new ArrayList<>();
        for (PushNotificationFuture<
                        ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>
                f : responseList) {
            try {
                PushNotificationResponse<ApnsPushNotification> r = f.get();
                if (!r.isAccepted()
                        && List.of("BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic")
                                .contains(r.getRejectionReason())) {
                    // collect bad/unregistered device tokens and remove from database
                    tokensToRemove.add(r.getPushNotification().getToken());
                }
            } catch (ExecutionException e) {
                logger.info("Exception waiting for response. Continue", e);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.error("Current thread was interrrupted", ex);
                break;
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
