package ch.admin.bag.covidcertificate.backend.delivery.ws.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IosHeartbeatSilentPush;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class IosHeartBeatSilentPushTest {
  @Test
  public void checkLoadFactorCalculation(){
    //our current schedule can handle 2.4mio registrations
    assertEquals(1.0, IosHeartbeatSilentPush.calculatePushLoadFactor(Duration.ofHours(2), Duration.ofMinutes(5), 100000, 2400000), 0.001);
    assertEquals(0.8, IosHeartbeatSilentPush.calculatePushLoadFactor(Duration.ofHours(2), Duration.ofMinutes(5), 100000, 1920000),0.001);
    assertEquals(2.0, IosHeartbeatSilentPush.calculatePushLoadFactor(Duration.ofHours(2), Duration.ofMinutes(5), 100000, 4800000), 0.001);
  }
}
