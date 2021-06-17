package ch.admin.bag.covidcertificate.backend.delivery.ws.controller.appcontroller;

import static ch.admin.bag.covidcertificate.backend.delivery.data.util.PostgresDbCleaner.cleanDatabase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.BaseControllerTest;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.CryptoHelper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.sql.DataSource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class AppControllerTest extends BaseControllerTest {
    @Autowired private DataSource dataSource;

    protected MediaType acceptMediaType;
    protected Algorithm algorithm;

    private static final String UNREGISTERED_CODE = "NWIKX22";

    private static final String BASE_URL = "/app/delivery/v1";

    private static final String INIT_ENDPOINT = BASE_URL + "/covidcert/register";
    private static final String GET_COVID_CERT_ENDPOINT = BASE_URL + "/covidcert";
    private static final String COMPLETE_ENDPOINT = BASE_URL + "/covidcert/complete";
    private static final String PUSH_REGISTER_ENDPOINT = BASE_URL + "/push/register"; // TODO
    private static final String PUSH_DEREGISTER_ENDPOINT = BASE_URL + "/push/deregister"; // TODO

    protected KeyPair ecKeyPair;
    protected KeyPair rsaKeyPair;

    @BeforeAll
    public void setup() throws NoSuchAlgorithmException, SQLException {
        this.ecKeyPair = CryptoHelper.createEcKeyPair();
        this.rsaKeyPair = CryptoHelper.createRsaKeyPair();
        cleanDatabase(dataSource.getConnection());
    }

    @Test
    public void testHello() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(get(BASE_URL).accept(MediaType.TEXT_PLAIN))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        assertNotNull(response);
        assertEquals(
                "Hello from CH Covidcertificate Delivery App WS", response.getContentAsString());
    }

    @Test
    public void registrationCodeReleasedTest() throws Exception {
        final String code = "ABCDEFGHI";

        DeliveryRegistration registration =
                getDeliveryRegistration(Action.REGISTER, code, Instant.now(), this.algorithm);

        // successful register
        registerForDelivery(registration);

        // code collision
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.CONFLICT.value()));

        // close transfer
        closeTransfer(registration);

        // code released
        registerForDelivery(registration);
    }

    private void registerForDelivery(DeliveryRegistration registration) throws Exception {
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void registrationInvalidActionTest() throws Exception {
        final String code = "A1B2C3D4E";

        // invalid action
        for (Action action : Action.values()) {
            if (!Action.REGISTER.equals(action)) {
                DeliveryRegistration getRegistration =
                        getDeliveryRegistration(action, code, Instant.now(), this.algorithm);
                mockMvc.perform(
                                post(INIT_ENDPOINT)
                                        .content(asJsonString(getRegistration))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(acceptMediaType))
                        .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()));
            }
        }
    }

    @Test
    public void registrationInvalidSignatureTest() throws Exception {
        final String code = "Z19NILH";

        DeliveryRegistration registration =
                getDeliveryRegistration(Action.GET, code, Instant.now(), this.algorithm);
        registration.setSignaturePayload(
                Base64.getEncoder()
                        .encodeToString("wrong signature".getBytes(StandardCharsets.UTF_8)));

        // invalid signature
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void registrationInvalidPublicKey() throws Exception {
        final String code = "Q58RTS";

        // invalid public key
        DeliveryRegistration registration =
                getDeliveryRegistration(Action.GET, code, Instant.now(), this.algorithm);
        registration.setPublicKey(
                Base64.getEncoder()
                        .encodeToString("invalid public key".getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        // invalid public key (algorithm, key mismatch)
        registration.setPublicKey(getPublicKey(getWrongAlgorithm()));
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void forbiddenTest() throws Exception {
        final String code = "PP726Q";

        // register
        registerForDelivery(
                getDeliveryRegistration(Action.REGISTER, code, Instant.now(), this.algorithm));

        // forbidden tests for get
        internalForbiddenTest(code, Action.GET, GET_COVID_CERT_ENDPOINT, HttpStatus.FORBIDDEN);

        // forbidden tests for delete (since best effort, it always returns OK)
        internalForbiddenTest(code, Action.DELETE, COMPLETE_ENDPOINT, HttpStatus.OK);
    }

    private void internalForbiddenTest(
            String code, Action expectedAction, String endpoint, HttpStatus expectedStatusCode)
            throws Exception {
        // invalid signature payload (action)
        for (Action action : Action.values()) {
            if (!expectedAction.equals(action)) {
                mockMvc.perform(
                                post(endpoint)
                                        .content(
                                                asJsonString(
                                                        getRequestDeliveryPayload(
                                                                action,
                                                                code,
                                                                Instant.now(),
                                                                this.algorithm)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(acceptMediaType))
                        .andExpect(status().is(expectedStatusCode.value()));
            }
        }

        // invalid signature payload (code)
        RequestDeliveryPayload payload =
                getRequestDeliveryPayload(expectedAction, code, Instant.now(), this.algorithm);
        // swap signature and signature payload with wrong code
        payload.setSignaturePayload(payload.getSignaturePayload().replace(code, UNREGISTERED_CODE));
        payload.setSignature(
                getSignatureForPayload(
                        payload.getSignaturePayload().getBytes(StandardCharsets.UTF_8),
                        this.algorithm));
        mockMvc.perform(
                        post(endpoint)
                                .content(asJsonString(payload))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(expectedStatusCode.value()));

        // invalid signature payload (timestamp)
        mockMvc.perform(
                        post(endpoint)
                                .content(
                                        asJsonString(
                                                getRequestDeliveryPayload(
                                                        expectedAction,
                                                        code,
                                                        Instant.now().minus(6, ChronoUnit.MINUTES),
                                                        this.algorithm)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(expectedStatusCode.value()));

        // invalid signature (encrypted with wrong private key)
        mockMvc.perform(
                        post(endpoint)
                                .content(
                                        asJsonString(
                                                getRequestDeliveryPayload(
                                                        expectedAction,
                                                        code,
                                                        Instant.now().minus(6, ChronoUnit.MINUTES),
                                                        getWrongAlgorithm())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(expectedStatusCode.value()));
    }

    private Algorithm getWrongAlgorithm() {
        return Algorithm.EC256.equals(this.algorithm) ? Algorithm.RSA2048 : Algorithm.EC256;
    }

    private void closeTransfer(DeliveryRegistration registration) throws Exception {
        RequestDeliveryPayload payload = getDeliveryCompletePayload(registration);
        mockMvc.perform(
                        post(COMPLETE_ENDPOINT)
                                .content(asJsonString(payload))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse();
    }

    private RequestDeliveryPayload getDeliveryCompletePayload(DeliveryRegistration registration)
            throws Exception {
        RequestDeliveryPayload payload = new RequestDeliveryPayload();
        payload.setCode(registration.getCode());
        String signaturePayload =
                getSignaturePayload(Action.DELETE, registration.getCode(), Instant.now());
        payload.setSignaturePayload(signaturePayload);
        payload.setSignature(
                getSignatureForPayload(
                        signaturePayload.getBytes(StandardCharsets.UTF_8),
                        registration.getAlgorithm()));
        return payload;
    }

    private RequestDeliveryPayload getRequestDeliveryPayload(
            Action action, String code, Instant instant, Algorithm algorithm) throws Exception {
        RequestDeliveryPayload payload = new RequestDeliveryPayload();
        payload.setCode(code);
        String signaturePayload = getSignaturePayload(action, code, instant);
        payload.setSignaturePayload(signaturePayload);
        payload.setSignature(
                getSignatureForPayload(
                        signaturePayload.getBytes(StandardCharsets.UTF_8), algorithm));
        return payload;
    }

    private DeliveryRegistration getDeliveryRegistration(
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

    private String getPublicKey(Algorithm algorithm) {
        switch (algorithm) {
            case EC256:
                return getEcPubKey();
            case RSA2048:
                return getRsaPubKey();
            default:
                throw new RuntimeException("unexpected algorithm");
        }
    }

    private String getSignaturePayload(Action action, String code, Instant instant) {
        return action.name() + ":" + code + ":" + instant.toEpochMilli();
    }

    private String getSignatureForPayload(byte[] toSign, Algorithm algorithm) throws Exception {
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

    private String getEcPubKey() {
        return CryptoHelper.getEcPubKeyUncompressedOctal(ecKeyPair.getPublic());
    }

    private String getRsaPubKey() {
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return BASE_URL;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
