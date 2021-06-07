/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.delivery.ws.controller;

import ch.admin.bag.covidcertificate.backend.delivery.model.cgs.CgsCovidCert;
import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Controller for the Certificate Generation Service */
@Controller
@RequestMapping("/v1/cgs")
public class CgsController {

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from CH Covidcertificate Delivery App WS"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from CH Covidcertificate Delivery CGS WS";
    }

    @Documentation(
            description = "covidcert delivery endpoint",
            responses = {
                "200 => delivery successful",
                "403 => invalid jwt",
                "404 => code not found"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/covidcert")
    public ResponseEntity<Void> addCovidCert(@Valid @RequestBody CgsCovidCert covidCert) {
        // TODO
        return ResponseEntity.ok().build();
    }
}
