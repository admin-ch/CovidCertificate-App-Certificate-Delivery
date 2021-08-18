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
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.cgs.CgsCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.Crypto;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import ch.ubique.openapi.docannotations.Documentation;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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

/** Controller for the Certificate Generation Service */
@Controller
@RequestMapping("/cgs/delivery/v1")
public class CgsController {

    private static final Logger logger = LoggerFactory.getLogger(CgsController.class);

    private final DeliveryDataService deliveryDataService;
    private final Crypto ecCrypto;
    private final Crypto rsaCrypto;

    public CgsController(
            DeliveryDataService deliveryDataService, Crypto ecCrypto, Crypto rsaCrypto) {
        this.deliveryDataService = deliveryDataService;
        this.ecCrypto = ecCrypto;
        this.rsaCrypto = rsaCrypto;
    }

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
                "418 => code not found"
            })
    @CrossOrigin(origins = {"https://editor.swagger.io"})
    @PostMapping(value = "/covidcert")
    public ResponseEntity<Void> addCovidCert(@Valid @RequestBody CgsCovidCert covidCert)
            throws CodeNotFoundException, InvalidPublicKeyException,
                    InvalidAlgorithmParameterException, NoSuchPaddingException,
                    IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
                    InvalidKeySpecException, InvalidParameterSpecException, InvalidKeyException {
        String code = covidCert.getCode();
        logger.info("received covid cert for transfer code {}", code);
        deliveryDataService.insertCovidCert(mapAndEncrypt(covidCert));
        logger.info("encrypted and inserted covid cert for transfer code {}", code);
        return ResponseEntity.ok().build();
    }

    private DbCovidCert mapAndEncrypt(CgsCovidCert covidCert)
            throws CodeNotFoundException, InvalidPublicKeyException,
                    InvalidAlgorithmParameterException, NoSuchPaddingException,
                    IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
                    InvalidKeySpecException, InvalidParameterSpecException, InvalidKeyException {
        DbCovidCert dbCovidCert = new DbCovidCert();
        DbTransfer transfer = deliveryDataService.findTransfer(covidCert.getCode());
        dbCovidCert.setFkTransfer(transfer.getPk());
        String publicKey = transfer.getPublicKey();
        Algorithm algorithm = transfer.getAlgorithm();
        dbCovidCert.setEncryptedHcert(encrypt(covidCert.getHcert(), publicKey, algorithm));
        dbCovidCert.setEncryptedPdf(encrypt(covidCert.getPdf(), publicKey, algorithm));
        return dbCovidCert;
    }

    private String encrypt(String toEncrypt, String publicKey, Algorithm algorithm)
            throws InvalidPublicKeyException, InvalidAlgorithmParameterException,
                    NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
                    BadPaddingException, InvalidKeySpecException, InvalidParameterSpecException,
                    InvalidKeyException {
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
        return crypto.encrypt(toEncrypt, publicKey);
    }

    @ExceptionHandler({CodeNotFoundException.class})
    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    public ResponseEntity<String> codeNotFound(CodeNotFoundException e) {
        logger.info("cgs sent non-existent transfer code {}", e.getCode());
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("code not found");
    }
}
