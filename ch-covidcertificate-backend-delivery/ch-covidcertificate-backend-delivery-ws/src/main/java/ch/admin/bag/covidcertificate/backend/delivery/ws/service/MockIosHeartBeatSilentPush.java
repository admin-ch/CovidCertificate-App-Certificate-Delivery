package ch.admin.bag.covidcertificate.backend.delivery.ws.service;

public class MockIosHeartBeatSilentPush implements IosHeartbeatSilentPush {

    public MockIosHeartBeatSilentPush() {}

    @Override
    public void sendHeartbeats() {
        // do nothing
    }
}
