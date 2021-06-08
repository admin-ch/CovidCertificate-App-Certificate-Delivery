/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.ws.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.impl.JdbcDeliveryDataServiceImpl;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.AppController;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.CgsController;
import ch.admin.bag.covidcertificate.backend.delivery.ws.interceptor.HeaderInjector;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.SignatureValidator;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public abstract class WsBaseConfig implements WebMvcConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value(
            "#{${ws.security.headers: {'X-Content-Type-Options':'nosniff', 'X-Frame-Options':'DENY','X-Xss-Protection':'1; mode=block'}}}")
    Map<String, String> additionalHeaders;

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

    @Bean
    public HeaderInjector securityHeaderInjector() {
        return new HeaderInjector(additionalHeaders);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeaderInjector());
    }

    @Bean
    public SignatureValidator signatureValidator() {
        return new SignatureValidator();
    }

    @Bean
    public DeliveryDataService deliveryDataService(DataSource dataSource) {
        return new JdbcDeliveryDataServiceImpl(dataSource);
    }

    @Bean
    public AppController appController(
            DeliveryDataService deliveryDataService, SignatureValidator signatureValidator) {
        return new AppController(deliveryDataService, signatureValidator);
    }

    @Bean
    public CgsController cgsController() {
        return new CgsController();
    }
}
