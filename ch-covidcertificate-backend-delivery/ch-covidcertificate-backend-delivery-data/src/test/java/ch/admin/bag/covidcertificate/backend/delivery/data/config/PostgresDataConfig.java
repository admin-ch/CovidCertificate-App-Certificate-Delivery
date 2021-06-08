/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.data.config;

import ch.admin.bag.covidcertificate.backend.delivery.data.util.SingletonPostgresContainer;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("postgres")
public class PostgresDataConfig {

    @Bean
    public DataSource dataSource() {
        final SingletonPostgresContainer instance = SingletonPostgresContainer.getInstance();
        instance.start();

        return DataSourceBuilder.create()
                .driverClassName(instance.getDriverClassName())
                .url(instance.getJdbcUrl())
                .username(instance.getUsername())
                .password(instance.getPassword())
                .build();
    }
}
