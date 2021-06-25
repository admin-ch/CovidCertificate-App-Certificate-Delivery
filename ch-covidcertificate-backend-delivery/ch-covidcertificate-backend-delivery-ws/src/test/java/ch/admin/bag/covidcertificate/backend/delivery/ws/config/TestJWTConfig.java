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
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableWebSecurity
@Profile(value = "jwt-test")
public class TestJWTConfig extends WebSecurityConfigurerAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ws.jwt.openid-configuration-url}")
    private String url;

    @Value("${ws.jwt.jwks-json-key:jwks_uri}")
    private String jwksUriJsonKey;

    @Value("${ws.jwt.verification.resource-access-path:resource_access}")
    private String resourceAccessPath;

    @Value("${ws.jwt.verification.certificate-creator-role:certificatecreator}")
    private String certificateCreatorRole;

    @Value("${ws.jwt.verification.role-path:/ch-covidcertificate-backend-delivery-ws/roles}")
    private String rolePath;

    @Value("${ws.jwt.defaultValidator:false}")
    private Boolean useDefaultValidator;

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
        return new DeliveryJWTValidator(resourceAccessPath, rolePath, certificateCreatorRole);
    }

    @Bean
    public JwtDecoder jwtDecoder() throws IOException {
        var jsonurl = new URL(url);
        final String jswUrl = objectMapper.readTree(jsonurl).get("jwks_uri").asText();
        final var nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jswUrl).build();
        if (useDefaultValidator) {
            nimbusJwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(), jwtValidator()));
        } else {
            nimbusJwtDecoder.setJwtValidator(jwtValidator());
        }
        return nimbusJwtDecoder;
    }
}
