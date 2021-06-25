package ch.admin.bag.covidcertificate.backend.delivery.ws.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JWTNoExpirationTest extends JWTTestBase {

    @Test
    void testHelloValidToken() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(
                                get(BASE_URL)
                                        .accept(MediaType.TEXT_PLAIN)
                                        .header("Authorization", "Bearer " + EXPIRED_TOKEN))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse();

        assertNotNull(response);
        assertEquals(
                "Hello from CH Covidcertificate Delivery CGS WS", response.getContentAsString());
    }

    @Test
    void testHelloNoToken() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(get(BASE_URL).accept(MediaType.TEXT_PLAIN))
                        .andExpect(status().is(401))
                        .andReturn()
                        .getResponse();
        assertNotNull(response.getHeader("www-authenticate"));
        assertTrue(response.getHeader("www-authenticate").contains("Bearer"));
    }
}
