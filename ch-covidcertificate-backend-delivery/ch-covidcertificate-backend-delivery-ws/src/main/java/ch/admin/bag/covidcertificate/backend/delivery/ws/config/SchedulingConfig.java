package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IOSHeartbeatSilentPush;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class SchedulingConfig {

    private final IOSHeartbeatSilentPush iosHeartbeatSilentPush;

    public SchedulingConfig(IOSHeartbeatSilentPush iosHeartbeatSilentPush) {
        this.iosHeartbeatSilentPush = iosHeartbeatSilentPush;
    }

    @Scheduled(cron = "${push.ios.cron:0 0 0/2 ? * *}")
    @SchedulerLock(name = "silent_push", lockAtLeastFor = "PT15S")
    public void iosHeartbeat() {
        iosHeartbeatSilentPush.sendHeartbeats();
    }
}
