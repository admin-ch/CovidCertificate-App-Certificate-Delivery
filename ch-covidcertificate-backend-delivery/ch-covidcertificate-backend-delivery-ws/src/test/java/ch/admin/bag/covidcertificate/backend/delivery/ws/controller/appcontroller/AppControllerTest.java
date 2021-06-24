package ch.admin.bag.covidcertificate.backend.delivery.ws.controller.appcontroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.CodeGenerator;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCertDelivery;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushType;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.BaseControllerTest;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.util.TestHelper;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class AppControllerTest extends BaseControllerTest {
    private static final String BASE_URL = "/app/delivery/v1";

    private static final String INIT_ENDPOINT = BASE_URL + "/covidcert/register";
    private static final String GET_COVID_CERT_ENDPOINT = BASE_URL + "/covidcert";
    private static final String COMPLETE_ENDPOINT = BASE_URL + "/covidcert/complete";
    private static final String PUSH_REGISTER_ENDPOINT = BASE_URL + "/push/register"; // TODO

    @BeforeAll
    public void setup() throws NoSuchAlgorithmException, SQLException {
        super.setup();
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
        final String code = CodeGenerator.generateCode();

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

        // complete transfer
        completeTransfer(registration);

        // code released
        registerForDelivery(registration);
    }

    @Test
    public void registrationInvalidActionTest() throws Exception {
        final String code = CodeGenerator.generateCode();

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
        final String code = CodeGenerator.generateCode();

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
        final String code = CodeGenerator.generateCode();

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
    public void deliveryFlowTest() throws Exception {
        final String code = CodeGenerator.generateCode();

        // register
        registerForDelivery(
                getDeliveryRegistration(Action.REGISTER, code, Instant.now(), this.algorithm));

        // get covid cert (not ready)
        MockHttpServletResponse response = getCovidCert(code);

        // verify response
        CovidCertDelivery emptyDelivery =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        CovidCertDelivery.class);
        assertTrue(emptyDelivery.getCovidCerts().isEmpty());

        // upsert dummy covid certificate
        upsertDummyCovidCert(code);

        // get covid cert (ready)
        response = getCovidCert(code);

        // verify response
        CovidCertDelivery delivery =
                testHelper.verifyAndReadValue(
                        response,
                        acceptMediaType,
                        TestHelper.PATH_TO_CA_PEM,
                        CovidCertDelivery.class);
        assertEquals(1, delivery.getCovidCerts().size());
        CovidCert covidCert = delivery.getCovidCerts().get(0);
        assertEquals(DUMMY_HCERT, covidCert.getEncryptedHcert());
        assertEquals(DUMMY_PDF, covidCert.getEncryptedPdf());

        // verify covid cert is still in db
        assertEquals(1, deliveryDataService.findCovidCerts(code).size());

        // bad complete transfer request
        completeTransfer(
                getRequestDeliveryPayload(
                        Action.DELETE,
                        code,
                        Instant.now().minus(6, ChronoUnit.MINUTES),
                        this.algorithm));

        // verify covid cert is still in db
        assertEquals(1, deliveryDataService.findCovidCerts(code).size());

        // complete transfer
        completeTransfer(
                getRequestDeliveryPayload(Action.DELETE, code, Instant.now(), this.algorithm));

        // verify transfer is closed
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts(code));

        // verify code has been released
        assertFalse(deliveryDataService.transferCodeExists(code));
    }

    @Test
    public void invalidCodeLengthTest() throws Exception {
        for (String invalidCode : List.of("TOOSHORT", "TOOLONG000")) {
            DeliveryRegistration registration =
                    getDeliveryRegistration(
                            Action.REGISTER, invalidCode, Instant.now(), this.algorithm);
            // code collision
            mockMvc.perform(
                            post(INIT_ENDPOINT)
                                    .content(asJsonString(registration))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(acceptMediaType))
                    .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
        }
    }

    private MockHttpServletResponse getCovidCert(String code) throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(
                                post(GET_COVID_CERT_ENDPOINT)
                                        .content(
                                                asJsonString(
                                                        getRequestDeliveryPayload(
                                                                Action.GET,
                                                                code,
                                                                Instant.now(),
                                                                this.algorithm)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(acceptMediaType))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();
        return response;
    }

    @Test
    public void forbiddenTest() throws Exception {
        final String code = CodeGenerator.generateCode();

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

    private void completeTransfer(DeliveryRegistration registration) throws Exception {
        completeTransfer(getDeliveryCompletePayload(registration));
    }

    private void completeTransfer(RequestDeliveryPayload payload) throws Exception {
        mockMvc.perform(
                        post(COMPLETE_ENDPOINT)
                                .content(asJsonString(payload))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful());
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

    private void upsertDummyCovidCert(String code) throws CodeNotFoundException {
        DbCovidCert dbCovidCert = new DbCovidCert();
        DbTransfer transfer = deliveryDataService.findTransfer(code);
        dbCovidCert.setFkTransfer(transfer.getPk());
        dbCovidCert.setEncryptedHcert(DUMMY_HCERT);
        dbCovidCert.setEncryptedPdf(DUMMY_PDF);
        deliveryDataService.insertCovidCert(dbCovidCert);
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return BASE_URL;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }

    @Test
    public void registerTest() throws Exception {
        var registration = new PushRegistration();
        registration.setPushToken("pushtoken");
        registration.setPushType(PushType.AND);
        registration.setRegisterId("registration_id");
        mockMvc.perform(
                        post(PUSH_REGISTER_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        final var pushRegistrations =
                deliveryDataService.getPushRegistrationByType(PushType.AND, 0);
        assertEquals(1, pushRegistrations.size());
        assertEquals("registration_id", pushRegistrations.get(0).getRegisterId());
        registration.setPushToken("");
        mockMvc.perform(
                        post(PUSH_REGISTER_ENDPOINT)
                                .content(asJsonString(registration))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertTrue(deliveryDataService.getPushRegistrationByType(PushType.AND, 0).isEmpty());
    }
}
