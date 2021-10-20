package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IosHeartbeatSilentPush;
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
    private final IosHeartbeatSilentPush iosHeartbeatSilentPush;
    private final DeliveryDataService deliveryDataService;

    @Value("${db.retentionPeriod:P30D}")
    private Duration retentionPeriod;

    @Value("${push.pushInterval:PT2h}")
    private Duration pushInterval;

    @Value("${push.schedulerInterval:PT10m}")
    private Duration pushScheduleInterval;

    @Value("${push.batchSize:100000}")
    private int pushBatchSize;


    public SchedulingConfig(
            IosHeartbeatSilentPush iosHeartbeatSilentPush,
            DeliveryDataService deliveryDataService) {
        this.iosHeartbeatSilentPush = iosHeartbeatSilentPush;
        this.deliveryDataService = deliveryDataService;
    }


    @Scheduled(fixedRateString = "${push.schedulerInterval:PT10m}")
    @SchedulerLock(name = "silent_push", lockAtLeastFor = "PT15S")
    public void iosHeartbeat() {
        iosHeartbeatSilentPush.sendHeartbeats(pushInterval, pushBatchSize);
    }

    // Runs the method every day at 00:00:00am
    @Scheduled(cron = "${db.cleanCron:0 0 0 ? * *}")
    @SchedulerLock(name = "db_cleanup", lockAtLeastFor = "PT10S")
    public void cleanCodes() {
        iosHeartbeatSilentPush.checkPushSchedule(pushInterval, pushScheduleInterval, pushBatchSize);
        try {
            logger.info("Removing expired transfer codes and associated covid certs");
            deliveryDataService.cleanDB();
        } catch (Exception e) {
            logger.error("Exception removing old transfer codes and covid certs", e);
        }
    }
}
