/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.ws.controller.cgscontroller;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"actuator-security"})
@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
public class CgsControllerJsonRsaTest extends CgsControllerTest {

    @BeforeAll
    public void setup() throws NoSuchAlgorithmException, SQLException {
        super.setup();
        this.acceptMediaType = MediaType.APPLICATION_JSON;
        this.algorithm = Algorithm.RSA2048;
    }
}
