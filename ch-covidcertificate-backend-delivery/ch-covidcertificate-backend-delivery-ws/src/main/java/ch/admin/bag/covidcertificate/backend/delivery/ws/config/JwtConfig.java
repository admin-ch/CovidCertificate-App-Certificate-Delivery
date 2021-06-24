/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.DeliveryJWTValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
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
@Profile(value = "jwt")
public class JwtConfig extends WebSecurityConfigurerAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ws.jws.url}")
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
                .antMatchers(HttpMethod.POST, "/cgs/delivery/v1/**")
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
        nimbusJwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefault(), jwtValidator()));
        return nimbusJwtDecoder;
    }
}
