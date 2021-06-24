package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IOSHeartbeatSilentPush;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);
    private final IOSHeartbeatSilentPush iosHeartbeatSilentPush;
    private final DeliveryDataService deliveryDataService;

    @Value("${db.retentionPeriod:7}")
    private Integer retentionPeriod;

    public SchedulingConfig(
            IOSHeartbeatSilentPush iosHeartbeatSilentPush,
            DeliveryDataService deliveryDataService) {
        this.iosHeartbeatSilentPush = iosHeartbeatSilentPush;
        this.deliveryDataService = deliveryDataService;
    }

    @Scheduled(cron = "${push.ios.cron:0 0 0/2 ? * *}")
    @SchedulerLock(name = "silent_push", lockAtLeastFor = "PT15S")
    public void iosHeartbeat() {
        iosHeartbeatSilentPush.sendHeartbeats();
    }

    @Scheduled(cron = "${db.cleanCron:0 0 0 ? * *}")
    @SchedulerLock(name = "db_cleanup", lockAtLeastFor = "PT10S")
    public void cleanCodes() {
        try {
            logger.info(
                    "Removing transfer codes and related covid certs older than {} days",
                    retentionPeriod);
            deliveryDataService.cleanDB(Duration.ofDays(retentionPeriod));
        } catch (Exception e) {
            logger.error("Exception removing old transfer codes and covid certs", e);
        }
    }
}
