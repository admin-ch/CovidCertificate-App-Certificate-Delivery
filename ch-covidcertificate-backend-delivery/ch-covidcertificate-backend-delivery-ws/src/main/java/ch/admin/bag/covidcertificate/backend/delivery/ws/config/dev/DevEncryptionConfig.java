package ch.admin.bag.covidcertificate.backend.delivery.ws.config.dev;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeAlreadyExistsException;
import ch.admin.bag.covidcertificate.backend.delivery.data.exception.CodeNotFoundException;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.DeliveryRegistration;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.ws.controller.AppController;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.SignaturePayloadValidator;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.Crypto;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidActionException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignatureException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignaturePayloadException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Configuration
@Profile("dev-encryption")
public class DevEncryptionConfig {

    private static final Logger logger = LoggerFactory.getLogger(DevAppController.class);

    @Bean
    @Primary
    public AppController appController(
            DeliveryDataService deliveryDataService,
            SignaturePayloadValidator signaturePayloadValidator,
            Crypto ecCrypto,
            Crypto rsaCrypto) {
        return new DevAppController(
                deliveryDataService, signaturePayloadValidator, ecCrypto, rsaCrypto);
    }

    public class DevAppController extends AppController {

        private static final String TEST_HCERT =
                "HC1:6BFMY3ZX73PO%203CCBR4OF7NI2*RLLTIKQDKYMW03%MG:GKCDKP-38E9/:N2QIL44RC5WA9+-0*Q3Q56CE0WC6W77K66U$QO37HMG.MO+FLAHV9LP9GK0JL2I989BBCL$G4.R3ITA6URNWLWMLW7H+SSOI8YF5MIP8 6VWK*96PYJ:D3:T0-Y5DLITLUM5K $25QHGJEQ85B54W7B8JCM40-D2R+8T1O2SI2DPYRJO9C5Q1693$58EFQ/%IH*O7JGS+GAV2PYFGYHXC707CGU8/4S5ART-45GHCCRI-9%MH 0BB%4U7VUONPWPBAG4-SDC6T3D 50E+CU+GCTIL64HEHAGUBJD9A3:72S471JOJQBMLPWDI910RH0IUG53SUFBK7RRJH9IC%NRC:AT15OC4%CM19DQZ33APNY9/P9DBWNCC5M6E9I6-0N6M-VR$7P+DQEXOUKMAW8I4VX19VLV6S3JZBJ7P:*I 392*TPPAQ1GGQV61Q:8R1OLE14W6PZLOQFERKJJ9NCMD55DVVF";
        private static final String PATH_TO_TEST_PDF = "pdf/test_cert.pdf";

        public DevAppController(
                DeliveryDataService deliveryDataService,
                SignaturePayloadValidator signaturePayloadValidator,
                Crypto ecCrypto,
                Crypto rsaCrypto) {
            super(deliveryDataService, signaturePayloadValidator, ecCrypto, rsaCrypto);
        }

        @Override
        public String hello() {
            return super.hello() + " (dev-encryption)";
        }

        @Override
        public ResponseEntity<Void> registerForDelivery(
                @Valid @RequestBody DeliveryRegistration registration)
                throws CodeAlreadyExistsException, InvalidSignatureException,
                        InvalidActionException, InvalidSignaturePayloadException,
                        InvalidPublicKeyException {
            ResponseEntity<Void> response = super.registerForDelivery(registration);

            // add covidcert to db
            DbCovidCert dbCovidCert = null;
            try {
                dbCovidCert = mapAndEncrypt(registration);
            } catch (Exception e) {
                logger.error("failed to map and encrypt debug covidcert", e);
            }
            deliveryDataService.insertCovidCert(dbCovidCert);

            return response;
        }

        private DbCovidCert mapAndEncrypt(DeliveryRegistration registration)
                throws CodeNotFoundException, InvalidPublicKeyException,
                        InvalidAlgorithmParameterException, NoSuchPaddingException,
                        IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
                        InvalidKeySpecException, InvalidParameterSpecException, InvalidKeyException,
                        IOException {
            DbCovidCert dbCovidCert = new DbCovidCert();
            DbTransfer transfer = deliveryDataService.findTransfer(registration.getCode());
            dbCovidCert.setFkTransfer(transfer.getPk());
            String publicKey = transfer.getPublicKey();
            Algorithm algorithm = transfer.getAlgorithm();
            dbCovidCert.setEncryptedHcert(encrypt(TEST_HCERT, publicKey, algorithm));
            dbCovidCert.setEncryptedPdf(
                    encrypt(encodeFileToBase64(PATH_TO_TEST_PDF), publicKey, algorithm));
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

        private String encodeFileToBase64(String path) throws IOException {
            return Base64.getEncoder()
                    .encodeToString(
                            Files.readAllBytes(new ClassPathResource(path).getFile().toPath()));
        }
    }
}
