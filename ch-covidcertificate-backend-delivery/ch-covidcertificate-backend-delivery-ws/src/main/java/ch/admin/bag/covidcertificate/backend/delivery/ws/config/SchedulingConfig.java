package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IOSHeartbeatSilentPush;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final IOSHeartbeatSilentPush iosHeartbeatSilentPush;

    public SchedulingConfig(IOSHeartbeatSilentPush iosHeartbeatSilentPush) {
        this.iosHeartbeatSilentPush = iosHeartbeatSilentPush;
    }

    @Scheduled(cron = "${push.ios.cron:0 0 0/2 ? * *}")
    public void iosHeartbeat() {
        iosHeartbeatSilentPush.sendHeartbeats();
    }
}
