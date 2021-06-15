package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class CryptoTest {

    private static final String EC_TEST_PUB_KEY =
            "BIECvEpBWglf3LU/Bk9sybpPBorlknUF5j4QptPdAqDauKo0DWT+cZuRpx0hz2W3E9YeeNpcZ5LTfqpKsJcav+A=";

    private static final String RSA_TEST_PUB_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqzlKop9TittYJTTerkmq88oEJuXl9dwMpeUerPRfKkhdpHDdPGFqRH0Z0FG0BC/UKqhvda86TJp/jzg0Jf4Zxque3DTkZJuK/iYBZ6fE95PXuZ9qyq1HFDwtyo2mhn8O3EgQItruXB8zyrFDdlLg4B9xy7BKNNAULhUT9zj4QsjCsNejWLHOgXDl8HVr45Y/urF9QoXW3LaD4iFPtlf0c5gcHWj0JbY1O9C+Rg5jv9vSLoErL6A8wHybb0nnTeckOobou8ZjlI+RN5sP/YTJubGOh4qaDg8IxVQM3eVKXFSEd6BkDV8b81Ia4DW643ZwmEJGVqFAU4Wi2cOt7ZD6FwIDAQAB";

    @Test
    public void ecEncryptorTest() throws Exception {
        EcCrypto encrypter = new EcCrypto();
        System.out.println(encrypter.encrypt("this is a test (ec)", EC_TEST_PUB_KEY));
    }

    @Test
    public void rsaEncryptorTest() throws Exception {
        RsaCrypto encrypter = new RsaCrypto();
        System.out.println(encrypter.encrypt("this is a test (rsa)", RSA_TEST_PUB_KEY));
    }

    private String getTestRsaPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        var modulos =
                "AKs5SqKfU4rbWCU03q5JqvPKBCbl5fXcDKXlHqz0XypIXaRw3TxhakR9GdBRtAQv1Cqob3WvOkyaf484NCX+Gcarntw05GSbiv4mAWenxPeT17mfasqtRxQ8LcqNpoZ/DtxIECLa7lwfM8qxQ3ZS4OAfccuwSjTQFC4VE/c4+ELIwrDXo1ixzoFw5fB1a+OWP7qxfUKF1ty2g+IhT7ZX9HOYHB1o9CW2NTvQvkYOY7/b0i6BKy+gPMB8m29J503nJDqG6LvGY5SPkTebD/2EybmxjoeKmg4PCMVUDN3lSlxUhHegZA1fG/NSGuA1uuN2cJhCRlahQFOFotnDre2Q+hc=";
        var exponent = "AQAB";

        var publicKeySpec =
                new RSAPublicKeySpec(
                        new BigInteger(Base64.getDecoder().decode(modulos)),
                        new BigInteger(Base64.getDecoder().decode(exponent)));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return Base64.getEncoder().encodeToString(kf.generatePublic(publicKeySpec).getEncoded());
    }
}
