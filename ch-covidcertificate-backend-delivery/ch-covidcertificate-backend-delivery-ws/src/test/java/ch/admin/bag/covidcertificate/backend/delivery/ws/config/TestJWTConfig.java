package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.DeliveryJWTValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableWebSecurity
@Profile(value = "jwt-test")
public class TestJWTConfig extends WebSecurityConfigurerAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value(
            "${ws.jws.url:https://identity-r.bit.admin.ch/realms/BAG-CovidCertificate/.well-known/openid-configuration}")
    private String url;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers("/cgs/delivery/v1/**")
                .authenticated()
                .anyRequest()
                .permitAll()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .decoder(jwtDecoder());
    }

    @Bean
    public DeliveryJWTValidator jwtValidator() {
        return new DeliveryJWTValidator();
    }

    @Bean
    public JwtDecoder jwtDecoder() throws IOException {
        var jsonurl = new URL(url);
        final String jswUrl = objectMapper.readTree(jsonurl).get("jwks_uri").asText();
        final var nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jswUrl).build();
        nimbusJwtDecoder.setJwtValidator(jwtValidator());
        return nimbusJwtDecoder;
    }
}
