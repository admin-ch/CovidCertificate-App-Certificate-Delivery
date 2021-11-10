package ch.admin.bag.covidcertificate.backend.delivery.ws.service;

import java.time.Duration;

public class MockIosHeartBeatSilentPush implements IosHeartbeatSilentPush {

    public MockIosHeartBeatSilentPush() {}

    @Override
    public void sendHeartbeats(Duration pushInterval, int pushLimit) {
        // do nothing
    }

    @Override
    public void checkPushSchedule(Duration pushInterval, Duration pushSchedule, int batchSize) {
        // do nothing
    }
}
