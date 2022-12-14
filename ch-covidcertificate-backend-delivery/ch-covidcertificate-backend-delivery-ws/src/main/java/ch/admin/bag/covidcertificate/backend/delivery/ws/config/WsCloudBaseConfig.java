package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IosHeartbeatSilentPush;
import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IosHeartbeatSilentPushImpl;
import java.util.Base64;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class WsCloudBaseConfig extends WsBaseConfig {

    @Value("${datasource.maximumPoolSize:5}")
    int dataSourceMaximumPoolSize;

    @Value("${datasource.connectionTimeout:30000}")
    int dataSourceConnectionTimeout;

    @Value("${datasource.leakDetectionThreshold:0}")
    int dataSourceLeakDetectionThreshold;

    @Value("${push.ios.signingkey}") // base64 encoded p8 file
    private String iosPushSigningKey;

    @Value("${push.ios.teamid}")
    private String iosPushTeamId;

    @Value("${push.ios.keyid}")
    private String iosPushKeyId;

    @Value("${push.ios.topic}")
    private String iosPushTopic;


    @Bean
    @Override
    public Flyway flyway(DataSource dataSource) {
        Flyway flyWay =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:/db/migration/pgsql_cluster")
                        .load();
        flyWay.migrate();
        return flyWay;
    }

    @Bean
    public IosHeartbeatSilentPush iosHeartbeatSilentPush(
            DeliveryDataService pushRegistrationDataService) {
        byte[] pushSigningKey = Base64.getDecoder().decode(iosPushSigningKey);
        return new IosHeartbeatSilentPushImpl(
                pushRegistrationDataService,
                pushSigningKey,
                iosPushTeamId,
                iosPushKeyId,
                iosPushTopic);
    }
}
