/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.ws;

import ch.admin.bag.covidcertificate.backend.delivery.ws.config.SchedulingConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(
        basePackages = {
            "ch.admin.bag.covidcertificate.backend.delivery.ws.config",
            "ch.admin.bag.covidcertificate.log",
            "ch.admin.bag.covidcertificate.rest"
        },
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = SchedulingConfig.class)
        })
@SpringBootApplication
public class TestApplication {}
