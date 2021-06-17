package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.ws.service.IOSHeartbeatSilentPush;
import java.util.Base64;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
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

    @Bean
    @Override
    public DataSource dataSource() {
        PoolConfig poolConfig =
                new PoolConfig(dataSourceMaximumPoolSize, dataSourceConnectionTimeout);
        DataSourceConfig dbConfig =
                new DataSourceConfig(
                        poolConfig,
                        null,
                        null,
                        Map.of("leakDetectionThreshold", dataSourceLeakDetectionThreshold));
        CloudFactory factory = new CloudFactory();
        return factory.getCloud().getSingletonServiceConnector(DataSource.class, dbConfig);
    }

    @Bean
    @Override
    public Flyway flyway() {
        Flyway flyWay =
                Flyway.configure()
                        .dataSource(dataSource())
                        .locations("classpath:/db/migration/pgsql_cluster")
                        .load();
        flyWay.migrate();
        return flyWay;
    }

    @Bean
    public IOSHeartbeatSilentPush iosHeartbeatSilentPush(
            DeliveryDataService pushRegistrationDataService) {
        byte[] pushSigningKey = Base64.getDecoder().decode(iosPushSigningKey);
        return new IOSHeartbeatSilentPush(
                pushRegistrationDataService,
                pushSigningKey,
                iosPushTeamId,
                iosPushKeyId,
                iosPushTopic);
    }
}
