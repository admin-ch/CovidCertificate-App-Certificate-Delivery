package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CryptoTest {

    private static final String EC_TEST_PUB_KEY =
            "BIECvEpBWglf3LU/Bk9sybpPBorlknUF5j4QptPdAqDauKo0DWT+cZuRpx0hz2W3E9YeeNpcZ5LTfqpKsJcav+A=";

    private static final String RSA_TEST_PUB_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArHM4MhgnmIOohkkXSI7KWBnXraSte3BiyFeJyhOICSa35JgH8JUsHxxL9Dn1lgOFqniHiDbC3m8AchuotTjKqgEyYr0FRMblWU1xzfLfY5eXCIeZUnvFVA+zBih6F7NHk3WKBn0nlo7urIbm7bObqjMN1vPP7g5kAhJv95t9kQ946q0nhC0ehF+AbrcCvI61VLFyuhuEncMeGF05UsFbtBJGfNrQvP1eEjGCbhPq1IxMqc0WAYcutDGwTs4GCYX+ID5YilPR0YTybgMXd8vMGgFlr2NV22ruZ7kZb1S/jgFdL0F26Hh0o8rUr3z2wAD6LHeJLrm10w6BiYBMyiAU2QIDAQAB";

    @Test
    public void testEcSignature() throws Exception {
        String signature =
                "MEYCIQCYg7o306qjiVHeEi/tRE/wIgOjds04t/8lDdp5/kEDpAIhAJAbd6hJbzVfovxtuMAewWDZErcGthwhERDrYDx4cP7E";
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(getEcPublicKey(EC_TEST_PUB_KEY));
        sig.update("hallo2".getBytes(StandardCharsets.UTF_8));
        assertFalse(sig.verify(Base64.getDecoder().decode(signature)));
        sig.update("hallo".getBytes(StandardCharsets.UTF_8));
        assertTrue(sig.verify(Base64.getDecoder().decode(signature)));
    }

    @Test
    public void testRsaSignature() throws Exception {
        String signature =
                "KyQukylFdiLLvXGN2LB2hFKXdt2OMeEMMDhbnKzL0xZsk/orZBuTEqKeCvZXtQtlWkcoUZCLflaVofWKlLNc6lKfaTncIJVgN7ZOuTyk0xblM2Z8TxGx3QCoFPxA/A6f9hB6YoreLCwxiAJL+Z4m08LJEA1J1+mz1aGP7mBfk2JeD3OXGcVhRUQWwdSY0XSweRYN7Ejb8hO93gjT/0ezaXU7R04biVYm8VrnLqnYAVjPcEW0CE2HJYoBO8umUy8yv7D4zWFN7Rb31nQjy1q0n4ABH6bvbFX7MYmBsvyJzePbHwxMcaP4YkhraU+56nLp4EW6EDzaoojwpknIwmtybw==";
        Signature sig = Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
        sig.initVerify(getRsaPublicKey(RSA_TEST_PUB_KEY));
        sig.update("hallo2".getBytes(StandardCharsets.UTF_8));
        assertFalse(sig.verify(Base64.getDecoder().decode(signature)));
        sig.update("hallo".getBytes(StandardCharsets.UTF_8));
        assertTrue(sig.verify(Base64.getDecoder().decode(signature)));
    }

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

    private PublicKey getEcPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidParameterSpecException,
                    InvalidKeySpecException {
        // the ios public key...
        var publicKeyBytes = Base64.getDecoder().decode(publicKey);

        // ... is in uncompressed octal represenation (0x04 | X | Y)
        var x = Arrays.copyOfRange(publicKeyBytes, 1, 33);
        var y = Arrays.copyOfRange(publicKeyBytes, 33, publicKeyBytes.length);

        KeyFactory kf = KeyFactory.getInstance("EC");
        var ecKeySpec =
                new ECPublicKeySpec(
                        new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                        EcCrypto.ecParameterSpecForCurve(EcCrypto.SECP256R1));
        return kf.generatePublic(ecKeySpec);
    }

    private PublicKey getRsaPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
