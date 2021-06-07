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

import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCertDelivery;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/app")
public class AppController {

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from CH Covidcertificate Delivery App WS"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from CH Covidcertificate Delivery App WS";
    }

    @Documentation(
            description = "delivery registration endpoint",
            responses = {
                "200 => registration for delivery successful",
                "409 => code collision. retry register with different code"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/covidcert/register")
    public ResponseEntity<Void> registerForDelivery(
            @Valid @RequestBody DeliveryRegistration registration) {
        // TODO
        return ResponseEntity.ok().build();
    }

    @Documentation(
            description = "covidcert delivery endpoint",
            responses = {
                "200 => list of covidcerts (empty list if not ready or already delivered)",
                "403 => invalid signature",
                "404 => code not found"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/covidcert")
    public ResponseEntity<CovidCertDelivery> getCovidCertDelivery(
            @Valid @RequestBody RequestDeliveryPayload payload) {
        // TODO
        return ResponseEntity.ok(new CovidCertDelivery(new ArrayList<>()));
    }

    @Documentation(
            description = "delete covid cert. to be used after successful delivery",
            responses = {
                "200 => covid certs deleted. covid certificate transfer is complete",
                "403 => invalid signature",
                "404 => code not found"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/covidcert/delete")
    public ResponseEntity<Void> deleteCovidCert(
            @Valid @RequestBody RequestDeliveryPayload payload) {
        // TODO
        return ResponseEntity.ok().build();
    }

    @Documentation(
            description = "push registration endpoint",
            responses = {"200 => registration for push successful"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/push/register")
    public ResponseEntity<Void> registerForPush(@Valid @RequestBody PushRegistration registration) {
        // TODO
        return ResponseEntity.ok().build();
    }

    @Documentation(
            description = "push deregistration endpoint",
            responses = {"200 => push registration removed successfully"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/delivery/push/deregister")
    public ResponseEntity<Void> deregisterForPush(
            @Valid @RequestBody PushRegistration registration) {
        // TODO
        return ResponseEntity.ok().build();
    }
}
