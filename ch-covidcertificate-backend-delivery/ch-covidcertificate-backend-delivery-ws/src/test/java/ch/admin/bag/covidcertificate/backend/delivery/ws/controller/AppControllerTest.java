package ch.admin.bag.covidcertificate.backend.delivery.ws.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public abstract class AppControllerTest extends BaseControllerTest {

    protected MediaType acceptMediaType;

    private String baseUrl = "/app/delivery/v1";

    @Test
    public void testTest() throws Exception {
        // TODO
    }

    @Override
    protected String getUrlForSecurityHeadersTest() {
        return baseUrl;
    }

    @Override
    protected MediaType getSecurityHeadersRequestMediaType() {
        return this.acceptMediaType;
    }
}
