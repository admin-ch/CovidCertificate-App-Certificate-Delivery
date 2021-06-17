package ch.admin.bag.covidcertificate.backend.delivery.ws.config.dev;

import ch.admin.bag.covidcertificate.backend.delivery.data.DeliveryDataService;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbCovidCert;
import ch.admin.bag.covidcertificate.backend.delivery.model.db.DbTransfer;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption.Crypto;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Profile("dev-encryption")
@EnableScheduling
public class DevEncryptionConfig {

    private static final Logger logger = LoggerFactory.getLogger(DevEncryptionConfig.class);

    @Value("${dev.cgs.insert.after:PT5M}")
    private Duration insertAfter;

    private static final String TEST_HCERT =
            "HC1:6BFMY3ZX73PO%203CCBR4OF7NI2*RLLTIKQDKYMW03%MG:GKCDKP-38E9/:N2QIL44RC5WA9+-0*Q3Q56CE0WC6W77K66U$QO37HMG.MO+FLAHV9LP9GK0JL2I989BBCL$G4.R3ITA6URNWLWMLW7H+SSOI8YF5MIP8 6VWK*96PYJ:D3:T0-Y5DLITLUM5K $25QHGJEQ85B54W7B8JCM40-D2R+8T1O2SI2DPYRJO9C5Q1693$58EFQ/%IH*O7JGS+GAV2PYFGYHXC707CGU8/4S5ART-45GHCCRI-9%MH 0BB%4U7VUONPWPBAG4-SDC6T3D 50E+CU+GCTIL64HEHAGUBJD9A3:72S471JOJQBMLPWDI910RH0IUG53SUFBK7RRJH9IC%NRC:AT15OC4%CM19DQZ33APNY9/P9DBWNCC5M6E9I6-0N6M-VR$7P+DQEXOUKMAW8I4VX19VLV6S3JZBJ7P:*I 392*TPPAQ1GGQV61Q:8R1OLE14W6PZLOQFERKJJ9NCMD55DVVF";
    private static final String PATH_TO_TEST_PDF = "pdf/test_cert.pdf";
    private final DeliveryDataService deliveryDataService;
    private final Crypto ecCrypto;
    private final Crypto rsaCrypto;

    public DevEncryptionConfig(
            DeliveryDataService deliveryDataService, Crypto ecCrypto, Crypto rsaCrypto) {
        this.deliveryDataService = deliveryDataService;
        this.ecCrypto = ecCrypto;
        this.rsaCrypto = rsaCrypto;
    }

    @Scheduled(cron = "${dev.cgs.insert.cron:0 * * ? * *}")
    public void insertCovidCerts() {
        List<DbTransfer> transfers =
                deliveryDataService.findTransferWithoutCovidCert(
                        Instant.now().minusMillis(insertAfter.toMillis()));

        logger.info("found {} transfers to insert dev covidcerts for", transfers.size());
        int insertCount = 0;
        for (DbTransfer transfer : transfers) {
            DbCovidCert dbCovidCert = null;
            try {
                dbCovidCert = mapAndEncrypt(transfer);
                insertCount++;
            } catch (Exception e) {
                logger.error("failed to map and encrypt debug covidcert", e);
            }
            deliveryDataService.insertCovidCert(dbCovidCert);
        }
        logger.info("inserted {} dev covidcerts", insertCount);
    }

    private DbCovidCert mapAndEncrypt(DbTransfer transfer)
            throws InvalidPublicKeyException, InvalidAlgorithmParameterException,
                    NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
                    BadPaddingException, InvalidKeySpecException, InvalidParameterSpecException,
                    InvalidKeyException, IOException {
        DbCovidCert dbCovidCert = new DbCovidCert();
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
                .encodeToString(Files.readAllBytes(new ClassPathResource(path).getFile().toPath()));
    }
}
