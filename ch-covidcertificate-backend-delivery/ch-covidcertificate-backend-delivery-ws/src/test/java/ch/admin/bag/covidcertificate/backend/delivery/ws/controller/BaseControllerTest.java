package ch.admin.bag.covidcertificate.backend.delivery.ws.controller;

import static ch.admin.bag.covidcertificate.backend.delivery.data.util.PostgresDbCleaner.cleanDatabase;
import static ch.admin.bag.covidcertificate.backend.delivery.ws.util.TestHelper.SECURITY_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.CryptoHelper;
import ch.admin.bag.covidcertificate.backend.delivery.ws.util.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"test", "local"})
@AutoConfigureMockMvc
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(initializers = BaseControllerTest.DockerPostgresDataSourceInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseControllerTest {

    @Autowired protected DataSource dataSource;
    @Autowired protected DeliveryDataService deliveryDataService;

    public static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:latest")
                            .asCompatibleSubstituteFor("postgres"));

    static {
        postgreSQLContainer.start();
    }

    @Autowired protected MockMvc mockMvc;
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected TestHelper testHelper = new TestHelper(objectMapper);

    protected KeyPair ecKeyPair;
    protected KeyPair rsaKeyPair;

    protected MediaType acceptMediaType;
    protected Algorithm algorithm;

    protected static final String UNREGISTERED_CODE = "NWIKX22";
    protected static final String DUMMY_HCERT = "dummyhcert";
    protected static final String DUMMY_PDF = "dummypdf";

    @BeforeAll
    public void setup() throws NoSuchAlgorithmException, SQLException {
        this.ecKeyPair = CryptoHelper.createEcKeyPair();
        this.rsaKeyPair = CryptoHelper.createRsaKeyPair();
        cleanDatabase(dataSource.getConnection());
    }

    public static class DockerPostgresDataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(@NotNull final ConfigurableApplicationContext applicationContext) {

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword());
        }
    }

    public String asJsonString(Object obj) {
        try {
            return this.objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSecurityHeaders() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(
                                get(getUrlForSecurityHeadersTest())
                                        .accept(getSecurityHeadersRequestMediaType()))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();
        for (final var header : SECURITY_HEADERS.keySet()) {
            assertTrue(response.containsHeader(header));
            assertEquals(SECURITY_HEADERS.get(header), response.getHeader(header));
        }
    }

    protected abstract String getUrlForSecurityHeadersTest();

    protected MediaType getSecurityHeadersRequestMediaType() {
        return MediaType.TEXT_PLAIN;
    }

    protected void registerForDelivery(DeliveryRegistration registration) throws Exception {
        mockMvc.perform(
                        post(getInitEndpoint())
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful());
    }

    private String getInitEndpoint() {
        return "/app/delivery/v1/covidcert/register";
    }

    protected DeliveryRegistration getDeliveryRegistration(
            Action action, String code, Instant instant, Algorithm algorithm) throws Exception {
        DeliveryRegistration registration = new DeliveryRegistration();
        registration.setCode(code);
        registration.setPublicKey(getPublicKey(algorithm));
        registration.setAlgorithm(algorithm);
        String signaturePayload = getSignaturePayload(action, code, instant);
        registration.setSignaturePayload(signaturePayload);
        registration.setSignature(
                getSignatureForPayload(
                        signaturePayload.getBytes(StandardCharsets.UTF_8), algorithm));
        return registration;
    }

    protected String getPublicKey(Algorithm algorithm) {
        switch (algorithm) {
            case EC256:
                return getEcPubKey();
            case RSA2048:
                return getRsaPubKey();
            default:
                throw new RuntimeException("unexpected algorithm");
        }
    }

    protected String getSignatureForPayload(byte[] toSign, Algorithm algorithm) throws Exception {
        Signature sig;
        switch (algorithm) {
            case EC256:
                sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(ecKeyPair.getPrivate());
                break;
            case RSA2048:
                sig = Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
                sig.initSign(rsaKeyPair.getPrivate());
                break;
            default:
                throw new RuntimeException("unexpected algorithm");
        }
        sig.update(toSign);
        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    protected String getSignaturePayload(Action action, String code, Instant instant) {
        return action.name() + ":" + code + ":" + instant.toEpochMilli();
    }

    protected String getEcPubKey() {
        return CryptoHelper.getEcPubKeyUncompressedOctal(ecKeyPair.getPublic());
    }

    protected String getRsaPubKey() {
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }
}
