package ch.admin.bag.covidcertificate.backend.delivery.ws.service;

import java.time.Duration;

public interface IosHeartbeatSilentPush {
    public void sendHeartbeats(Duration pushInterval, int pushLimit);

    public void checkPushSchedule(Duration pushInterval, Duration pushSchedule, int batchSize);

    static float calculatePushLoadFactor(Duration pushInterval, Duration pushSchedule, int batchSize, int numRegistrations){
        float pushesPerPeriod = pushInterval.dividedBy(pushSchedule) * (float) batchSize;
        float loadFactor = numRegistrations/pushesPerPeriod;
        return loadFactor;
    }
}
