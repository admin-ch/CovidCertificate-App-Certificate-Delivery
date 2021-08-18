package ch.admin.bag.covidcertificate.backend.delivery.ws.security.jwt;

import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.BaseControllerTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "jwt-test"})
@TestPropertySources({
    @TestPropertySource("classpath:application-test.properties"),
    @TestPropertySource("classpath:application-local.properties")
})
@ContextConfiguration(initializers = BaseControllerTest.DockerPostgresDataSourceInitializer.class)
@Testcontainers
abstract class JWTTestBase {

    protected static final String EXPIRED_TOKEN =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ4VmVKOGxMcHNvSkRSS3hKV21ZbXlnVFJIWEVZT1JLRmdaMFkwRE5lZmt3In0.eyJleHAiOjE2MjQzNTU3MjIsImlhdCI6MTYyNDM1NTQyMiwianRpIjoiNWIzZjY0MjMtNjFmMC00ZjdkLTg1ZGEtNjA5NDRjMjg3OWM4IiwiaXNzIjoiaHR0cHM6Ly9pZGVudGl0eS1yLmJpdC5hZG1pbi5jaC9yZWFsbXMvQkFHLUNvdmlkQ2VydGlmaWNhdGUiLCJhdWQiOlsiY2MtcHJpbnRpbmctc2VydmljZSIsImNoLWNvdmlkY2VydGlmaWNhdGUtYmFja2VuZC1kZWxpdmVyeS13cyJdLCJzdWIiOiIwMWI0MjI4OC0wODhlLTQ3NDgtYjU3NC02MmJiNzdiNWIxMGEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjYy1tYW5hZ2VtZW50LXNlcnZpY2UiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJiYWctY2MtY2VydGlmaWNhdGVjcmVhdG9yIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiY2MtbWFuYWdlbWVudC1zZXJ2aWNlIjp7InJvbGVzIjpbImNlcnRpZmljYXRlY3JlYXRvciJdfSwiY2MtcHJpbnRpbmctc2VydmljZSI6eyJyb2xlcyI6WyJhbGxvdyIsImNlcnRpZmljYXRlY3JlYXRvciJdfSwiY2gtY292aWRjZXJ0aWZpY2F0ZS1iYWNrZW5kLWRlbGl2ZXJ5LXdzIjp7InJvbGVzIjpbImNlcnRpZmljYXRlY3JlYXRvciJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwgVXNlcnJvbGVzX01hcHBlciIsImNsaWVudEhvc3QiOiIxMC4yNDQuNS40MSIsImNsaWVudElkIjoiY2MtbWFuYWdlbWVudC1zZXJ2aWNlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJ1c2Vycm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsImJhZy1jYy1jZXJ0aWZpY2F0ZWNyZWF0b3IiXSwiY3R4IjoiVVNFUiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1jYy1tYW5hZ2VtZW50LXNlcnZpY2UiLCJjbGllbnRBZGRyZXNzIjoiMTAuMjQ0LjUuNDEifQ.M7vRrv666nm3NrD1kIi1SAm8kOBa_dSvdPURyZ-EqEL-_tysj_no8JUjBE-EiedTeA40UKp-VoWXnPpjrPEWEi6jgOAETEl2BMgTzBHdZM1vo5e9XmWVyOFicciYukDOZpoSa-m-gnVmCqFEWicCO32AQ-H8J6HpEfWah8LIyO2BKy1MSTMZYcsGXd6N9nGJFddUBgDcFSFw46ruQL8DjDXFUN4JXlORI26EKr-k7XdGPBCAEdFy6uDbkSko3RBq2EwuCTS88D8gy1AUCDokPUzcT_bFNltuPGIaH5ihLCT7SpMdTU-YfCu7ML6v4-HwT0BxzeUQdriYbkzpzIn2Ig";

    protected static final String BASE_URL = "/cgs/delivery/v1";

    public static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:latest")
                            .asCompatibleSubstituteFor("postgres"));

    static {
        postgreSQLContainer.start();
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtDecoder jwtDecoder;

    @Value("${ws.jwt.verification.resource-access-path:resource_access}")
    private String resourceAccessPath;

    /** Creates an unsigned JWT token with the given expiration date */
    protected String createMaliciousToken(Instant expiresAt) {
        final var claim = jwtDecoder.decode(EXPIRED_TOKEN).getClaimAsString(resourceAccessPath);
        Claims claims = Jwts.claims();
        claims.put(resourceAccessPath, claim);
        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setSubject(
                        "test-subject"
                                + OffsetDateTime.now()
                                        .withOffsetSameInstant(ZoneOffset.UTC)
                                        .toString())
                .setExpiration(Date.from(expiresAt))
                .setIssuedAt(Date.from(Instant.now()))
                .compact();
    }
}
