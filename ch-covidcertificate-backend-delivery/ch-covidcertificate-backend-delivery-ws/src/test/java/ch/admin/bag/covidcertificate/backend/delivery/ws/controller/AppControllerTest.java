package ch.admin.bag.covidcertificate.backend.delivery.ws.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.CryptoHelper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class AppControllerTest extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private static final String BASE_URL = "/app/delivery/v1";

    private static final String INIT_ENDPOINT = BASE_URL + "/covidcert/register";
    private static final String GET_COVID_CERT = BASE_URL + "/covidcert";
    private static final String COMPLETE_ENDPOINT = BASE_URL + "/covidcert/complete";
    private static final String PUSH_REGISTER = BASE_URL + "/push/register"; // TODO
    private static final String PUSH_DEREGISTER = BASE_URL + "/push/deregister"; // TODO

    protected KeyPair ecKeyPair;
    protected KeyPair rsaKeyPair;

    @BeforeAll
    public void setup() throws NoSuchAlgorithmException {
        this.ecKeyPair = CryptoHelper.createEcKeyPair();
        this.rsaKeyPair = CryptoHelper.createRsaKeyPair();
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
                getDeliveryRegistration(Action.REGISTER, code, Instant.now(), Algorithm.EC256);

        // successful register
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful());

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

        DeliveryRegistration getRegistration =
                getDeliveryRegistration(Action.GET, code, Instant.now(), Algorithm.EC256);

        // invalid action
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(getRegistration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()));

        DeliveryRegistration deleteRegistration =
                getDeliveryRegistration(Action.DELETE, code, Instant.now(), Algorithm.EC256);
        // invalid action
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(deleteRegistration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    @Test
    public void registrationInvalidSignatureTest() throws Exception {
        final String code = "Z19NILH";

        DeliveryRegistration registration =
                getDeliveryRegistration(Action.GET, code, Instant.now(), Algorithm.EC256);
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

        DeliveryRegistration registration =
                getDeliveryRegistration(Action.GET, code, Instant.now(), Algorithm.EC256);
        registration.setPublicKey(
                Base64.getEncoder()
                        .encodeToString("invalid public key".getBytes(StandardCharsets.UTF_8)));

        // invalid signature
        mockMvc.perform(
                        post(INIT_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    private void closeTransfer(DeliveryRegistration registration) throws Exception {
        RequestDeliveryPayload payload = new RequestDeliveryPayload();
        payload.setCode(registration.getCode());
        String signaturePayload =
                getSignaturePayload(Action.DELETE, registration.getCode(), Instant.now());
        payload.setSignaturePayload(signaturePayload);
        payload.setSignature(
                getSignatureForPayload(
                        signaturePayload.getBytes(StandardCharsets.UTF_8),
                        registration.getAlgorithm()));
        mockMvc.perform(
                        post(COMPLETE_ENDPOINT)
                                .content(asJsonString(payload))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse();
    }

    private DeliveryRegistration getDeliveryRegistration(
            Action action, String code, Instant instant, Algorithm algorithm) throws Exception {
        DeliveryRegistration registration = new DeliveryRegistration();
        registration.setCode(code);
        registration.setPublicKey(
                Algorithm.EC256.equals(algorithm) ? getEcPubKey() : getRsaPubKey());
        registration.setAlgorithm(algorithm);
        String signaturePayload = getSignaturePayload(action, code, instant);
        registration.setSignaturePayload(signaturePayload);
        registration.setSignature(
                getSignatureForPayload(
                        signaturePayload.getBytes(StandardCharsets.UTF_8), algorithm));
        return registration;
    }

    private String getSignaturePayload(Action action, String code, Instant instant) {
        String signaturePayload = action.name() + ":" + code + ":" + instant.toEpochMilli();
        return signaturePayload;
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
