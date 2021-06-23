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
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.CovidCertDelivery;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.RequestDeliveryPayload;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.SignaturePayloadValidator;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.Crypto;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidActionException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignatureException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignaturePayloadException;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    protected final DeliveryDataService deliveryDataService;
    private final SignaturePayloadValidator signaturePayloadValidator;
    protected final Crypto ecCrypto;
    protected final Crypto rsaCrypto;

    public AppController(
            DeliveryDataService deliveryDataService,
            SignaturePayloadValidator signaturePayloadValidator,
            Crypto ecCrypto,
            Crypto rsaCrypto) {
        this.deliveryDataService = deliveryDataService;
        this.signaturePayloadValidator = signaturePayloadValidator;
        this.ecCrypto = ecCrypto;
        this.rsaCrypto = rsaCrypto;
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
            throws CodeAlreadyExistsException, InvalidSignatureException, InvalidActionException,
                    InvalidSignaturePayloadException, InvalidPublicKeyException {
        validateSignature(
                registration.getPublicKey(),
                registration.getAlgorithm(),
                registration.getSignaturePayload(),
                registration.getSignature());
        signaturePayloadValidator.validate(
                registration.getSignaturePayload(), Action.REGISTER, registration.getCode());
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
            throws CodeNotFoundException, InvalidSignatureException, InvalidActionException,
                    InvalidSignaturePayloadException, InvalidPublicKeyException {
        validateSignature(payload.getCode(), payload.getSignaturePayload(), payload.getSignature());
        signaturePayloadValidator.validate(
                payload.getSignaturePayload(), Action.GET, payload.getCode());
        List<CovidCert> covidCerts = deliveryDataService.findCovidCerts(payload.getCode());
        return ResponseEntity.ok(new CovidCertDelivery(covidCerts));
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
            @Valid @RequestBody RequestDeliveryPayload payload) {
        try {
            validateSignature(
                    payload.getCode(), payload.getSignaturePayload(), payload.getSignature());
            signaturePayloadValidator.validate(
                    payload.getSignaturePayload(), Action.DELETE, payload.getCode());
            deliveryDataService.closeTransfer(payload.getCode());
        } catch (Exception e) {
            // do nothing. best effort only.
            logger.info("failed to delete/clean transfer", e);
        }
        return ResponseEntity.ok().build();
    }

    private void validateSignature(
            String publicKey, Algorithm algorithm, String signaturePayload, String signature)
            throws InvalidPublicKeyException, InvalidSignatureException {
        Crypto crypto;
        switch (algorithm) {
            case EC256:
                crypto = ecCrypto;
                break;
            case RSA2048:
                crypto = rsaCrypto;
                break;
            default:
                logger.error("unexpected algorithm: {}", algorithm);
                throw new InvalidPublicKeyException();
        }
        crypto.validateSignature(signaturePayload, signature, publicKey);
    }

    private void validateSignature(String code, String signaturePayload, String signature)
            throws CodeNotFoundException, InvalidPublicKeyException, InvalidSignatureException {
        DbTransfer transfer = deliveryDataService.findTransfer(code);
        validateSignature(
                transfer.getPublicKey(), transfer.getAlgorithm(), signaturePayload, signature);
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

    private boolean isRegisterRequest(HttpServletRequest req) {
        return req.getRequestURL().toString().endsWith("/app/delivery/v1/covidcert/register");
    }

    @ExceptionHandler({CodeAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<String> codeAlreadyExists(HttpServletRequest req) {
        if (isRegisterRequest(req)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("code already in use");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("access denied");
        }
    }

    @ExceptionHandler({CodeNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> codeNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("code not found");
    }

    @ExceptionHandler({InvalidSignatureException.class, InvalidSignaturePayloadException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<String> invalidSignature() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
    }

    @ExceptionHandler({InvalidPublicKeyException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> invalidPublicKey(HttpServletRequest req) {
        if (isRegisterRequest(req)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid public key");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("access denied");
        }
    }

    @ExceptionHandler({InvalidActionException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<String> invalidAction(HttpServletRequest req) {
        if (isRegisterRequest(req)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("invalid action");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("access denied");
        }
    }
}
