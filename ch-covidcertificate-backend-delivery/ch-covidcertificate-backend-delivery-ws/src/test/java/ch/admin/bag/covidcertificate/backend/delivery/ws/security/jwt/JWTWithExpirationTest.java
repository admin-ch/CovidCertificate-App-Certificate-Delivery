package ch.admin.bag.covidcertificate.backend.delivery.ws.security.jwt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"ws.jwt.defaultValidator=true"})
class JWTWithExpirationTest extends JWTTestBase {

    @Test
    void testHelloExpiredToken() throws Exception {
        final MockHttpServletResponse response =
                mockMvc.perform(
                                get(BASE_URL)
                                        .accept(MediaType.TEXT_PLAIN)
                                        .header("Authorization", "Bearer " + EXPIRED_TOKEN))
                        .andExpect(status().is(401))
                        .andReturn()
                        .getResponse();

        final var authenticationHeader = response.getHeader("www-authenticate");
        assertNotNull(authenticationHeader);
        assertTrue(authenticationHeader.contains("Bearer"));
        assertTrue(authenticationHeader.contains("Jwt expired at"));
    }
}
