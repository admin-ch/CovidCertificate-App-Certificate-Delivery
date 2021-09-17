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
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidTimestampException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.utils.HashUtil;
import ch.ubique.openapi.docannotations.Documentation;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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
    protected final Crypto ecCrypto;
    protected final Crypto rsaCrypto;
    private final SignaturePayloadValidator signaturePayloadValidator;

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
                    InvalidSignaturePayloadException, InvalidPublicKeyException,
                    NoSuchAlgorithmException, InvalidTimestampException {
        String code = registration.getCode();
        logger.info("registration for transfer code {} requested", code);
        validateSignature(
                registration.getPublicKey(),
                registration.getAlgorithm(),
                registration.getSignaturePayload(),
                registration.getSignature());
        signaturePayloadValidator.validate(
                registration.getSignaturePayload(), Action.REGISTER, code);
        deliveryDataService.initTransfer(registration);
        logger.info(
                "registration for transfer code {} successful. publicKey sha256 hash: {}",
                code,
                HashUtil.getSha256Hash(registration.getPublicKey()));
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
                    InvalidSignaturePayloadException, InvalidPublicKeyException,
                    InvalidTimestampException {
        String code = payload.getCode();
        validateSignature(code, payload.getSignaturePayload(), payload.getSignature());
        signaturePayloadValidator.validate(payload.getSignaturePayload(), Action.GET, code);
        List<CovidCert> covidCerts = deliveryDataService.findCovidCerts(code);
        if (!covidCerts.isEmpty()) {
            logger.info("delivering {} covid certs for transfer code {}", covidCerts.size(), code);
        }
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
            @Valid @RequestBody RequestDeliveryPayload payload)
            throws InvalidPublicKeyException, InvalidSignatureException, CodeNotFoundException,
                    InvalidActionException, InvalidSignaturePayloadException,
                    InvalidTimestampException {
        String code = payload.getCode();
        validateSignature(code, payload.getSignaturePayload(), payload.getSignature());
        signaturePayloadValidator.validate(payload.getSignaturePayload(), Action.DELETE, code);
        deliveryDataService.closeTransfer(code);
        logger.info("transfer complete. removed transfer code {}", code);
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
            description =
                    "push de/registration endpoint. (if no pushToken is sent, it is a deregistration)",
            responses = {"200 => de/registration for push successful"})
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/push/register")
    public ResponseEntity<Void> registerForPush(@Valid @RequestBody PushRegistration registration) {
        deliveryDataService.upsertPushRegistration(registration);
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

    @ExceptionHandler({InvalidSignatureException.class, InvalidSignaturePayloadException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<String> invalidSignature() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
    }

    @ExceptionHandler({InvalidTimestampException.class})
    @ResponseStatus(HttpStatus.TOO_EARLY)
    public ResponseEntity<String> invalidTimestamp(InvalidTimestampException e) {
        logger.error("received invalid timestamp. {}", e.toString());
        return ResponseEntity.status(HttpStatus.TOO_EARLY).body("I | TIME");
    }

    @ExceptionHandler({InvalidPublicKeyException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> invalidPublicKey() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("I | KEY");
    }

    @ExceptionHandler({InvalidActionException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<String> invalidAction() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("invalid action");
    }
}
