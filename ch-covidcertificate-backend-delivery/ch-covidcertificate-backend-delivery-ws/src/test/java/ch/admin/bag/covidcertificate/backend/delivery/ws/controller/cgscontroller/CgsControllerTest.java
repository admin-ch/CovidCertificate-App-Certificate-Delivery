package ch.admin.bag.covidcertificate.backend.delivery.ws.controller.cgscontroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.data.util.CodeGenerator;
import ch.admin.bag.covidcertificate.backend.delivery.model.cgs.CgsCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.BaseControllerTest;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class CgsControllerTest extends BaseControllerTest {
    private static final String BASE_URL = "/cgs/delivery/v1";

    private static final String COVID_CERT_UPLOAD_ENDPOINT = BASE_URL + "/covidcert";

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
                "Hello from CH Covidcertificate Delivery CGS WS", response.getContentAsString());
    }

    @Test
    public void testUpload() throws Exception {
        final String code = CodeGenerator.generateCode();

        // verify no covid cert uploaded
        assertThrows(CodeNotFoundException.class, () -> deliveryDataService.findCovidCerts(code));

        // register client for covidcert delivery
        registerForDelivery(
                getDeliveryRegistration(Action.REGISTER, code, Instant.now(), this.algorithm));

        mockMvc.perform(
                        post(COVID_CERT_UPLOAD_ENDPOINT)
                                .content(asJsonString(getCgsCovidCert(code)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is2xxSuccessful());

        // verify covid cert uploaded
        assertEquals(1, deliveryDataService.findCovidCerts(code).size());
    }

    @Test
    public void testCodeNotFound() throws Exception {
        mockMvc.perform(
                        post(COVID_CERT_UPLOAD_ENDPOINT)
                                .content(asJsonString(getCgsCovidCert(UNREGISTERED_CODE)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(acceptMediaType))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    private CgsCovidCert getCgsCovidCert(String code) {
        CgsCovidCert cgsCovidCert = new CgsCovidCert();
        cgsCovidCert.setCode(code);
        cgsCovidCert.setHcert(DUMMY_HCERT);
        cgsCovidCert.setPdf(DUMMY_PDF);
        return cgsCovidCert;
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
