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

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCertDelivery;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.SignatureValidator;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignatureException;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/app/delivery/v1")
public class AppController {

    private final DeliveryDataService deliveryDataService;
    private final SignatureValidator signatureValidator;

    public AppController(
            DeliveryDataService deliveryDataService, SignatureValidator signatureValidator) {
        this.deliveryDataService = deliveryDataService;
        this.signatureValidator = signatureValidator;
    }

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
                "400 => invalid public key",
                "403 => invalid signature",
                "406 => invalid action",
                "409 => code collision. retry register with different code",
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/covidcert/register")
    public ResponseEntity<Void> registerForDelivery(
            @Valid @RequestBody DeliveryRegistration registration)
            throws CodeAlreadyExistsException, InvalidSignatureException {
        signatureValidator.validate(
                registration.getSignaturePayload(), registration.getSignature());
        deliveryDataService.initTransfer(registration);
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
    @PostMapping(value = "/covidcert")
    public ResponseEntity<CovidCertDelivery> getCovidCertDelivery(
            @Valid @RequestBody RequestDeliveryPayload payload)
            throws CodeNotFoundException, InvalidSignatureException {
        signatureValidator.validate(payload.getSignaturePayload(), payload.getSignature());
        deliveryDataService.findCovidCerts(payload.getCode());
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
    @PostMapping(value = "/covidcert/complete")
    public ResponseEntity<Void> covidCertDeliveryComplete(
            @Valid @RequestBody RequestDeliveryPayload payload)
            throws CodeNotFoundException, InvalidSignatureException {
        try {
            signatureValidator.validate(payload.getSignaturePayload(), payload.getSignature());
            deliveryDataService.closeTransfer(payload.getCode());
        } catch (Exception e) {
            // do nothing. best effort only.
        }
        return ResponseEntity.ok().build();
    }

    @Documentation(
            description = "push registration endpoint",
            responses = {"200 => registration for push successful"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/push/register")
    public ResponseEntity<Void> registerForPush(@Valid @RequestBody PushRegistration registration) {
        deliveryDataService.insertPushRegistration(registration);
        return ResponseEntity.ok().build();
    }

    @Documentation(
            description = "push deregistration endpoint",
            responses = {"200 => push registration removed successfully"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/push/deregister")
    public ResponseEntity<Void> deregisterForPush(
            @Valid @RequestBody PushRegistration registration) {
        deliveryDataService.removePushRegistration(registration);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({CodeAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<String> codeAlreadyExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("code already in use");
    }

    @ExceptionHandler({CodeNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> codeNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("code not found");
    }

    @ExceptionHandler({InvalidSignatureException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<String> invalidSignature() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
    }
}
