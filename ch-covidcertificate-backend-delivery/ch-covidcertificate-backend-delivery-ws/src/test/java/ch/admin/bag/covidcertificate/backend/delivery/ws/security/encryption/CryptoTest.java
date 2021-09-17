package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.admin.bag.covidcertificate.backend.delivery.data.util.CodeGenerator;
import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.Action;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignatureException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

public class CryptoTest {

    private static final String EC_TEST_PUB_KEY =
            "BIECvEpBWglf3LU/Bk9sybpPBorlknUF5j4QptPdAqDauKo0DWT+cZuRpx0hz2W3E9YeeNpcZ5LTfqpKsJcav+A=";

    private static final String RSA_TEST_PUB_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArHM4MhgnmIOohkkXSI7KWBnXraSte3BiyFeJyhOICSa35JgH8JUsHxxL9Dn1lgOFqniHiDbC3m8AchuotTjKqgEyYr0FRMblWU1xzfLfY5eXCIeZUnvFVA+zBih6F7NHk3WKBn0nlo7urIbm7bObqjMN1vPP7g5kAhJv95t9kQ946q0nhC0ehF+AbrcCvI61VLFyuhuEncMeGF05UsFbtBJGfNrQvP1eEjGCbhPq1IxMqc0WAYcutDGwTs4GCYX+ID5YilPR0YTybgMXd8vMGgFlr2NV22ruZ7kZb1S/jgFdL0F26Hh0o8rUr3z2wAD6LHeJLrm10w6BiYBMyiAU2QIDAQAB";

    @Test
    public void ecSignatureValidationTest() throws Exception {
        String signature =
                "MEYCIQCYg7o306qjiVHeEi/tRE/wIgOjds04t/8lDdp5/kEDpAIhAJAbd6hJbzVfovxtuMAewWDZErcGthwhERDrYDx4cP7E";
        EcCrypto crypto = new EcCrypto();

        // valid
        crypto.validateSignature("hallo", signature, EC_TEST_PUB_KEY);

        // invalid payload
        assertThrows(
                InvalidSignatureException.class,
                () -> crypto.validateSignature("hallo2", signature, EC_TEST_PUB_KEY));
    }

    @Test
    public void rsaSignatureValidationTest() throws Exception {
        String signature =
                "KyQukylFdiLLvXGN2LB2hFKXdt2OMeEMMDhbnKzL0xZsk/orZBuTEqKeCvZXtQtlWkcoUZCLflaVofWKlLNc6lKfaTncIJVgN7ZOuTyk0xblM2Z8TxGx3QCoFPxA/A6f9hB6YoreLCwxiAJL+Z4m08LJEA1J1+mz1aGP7mBfk2JeD3OXGcVhRUQWwdSY0XSweRYN7Ejb8hO93gjT/0ezaXU7R04biVYm8VrnLqnYAVjPcEW0CE2HJYoBO8umUy8yv7D4zWFN7Rb31nQjy1q0n4ABH6bvbFX7MYmBsvyJzePbHwxMcaP4YkhraU+56nLp4EW6EDzaoojwpknIwmtybw==";
        RsaCrypto crypto = new RsaCrypto();

        // valid
        crypto.validateSignature("hallo", signature, RSA_TEST_PUB_KEY);

        // invalid payload
        assertThrows(
                InvalidSignatureException.class,
                () -> crypto.validateSignature("hallo2", signature, RSA_TEST_PUB_KEY));
    }

    @Test
    public void ecEncryptTest() throws Exception {
        EcCrypto crypto = new EcCrypto();
        System.out.println(crypto.encrypt("this is a test (ec)", EC_TEST_PUB_KEY));
    }

    @Test
    public void rsaEncryptTest() throws Exception {
        RsaCrypto crypto = new RsaCrypto();
        System.out.println(crypto.encrypt("this is a test (rsa)", RSA_TEST_PUB_KEY));
    }

    private static final String CODE = CodeGenerator.generateCode();

    @Test
    public void signEcTest() throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        KeyPair ecKeyPair = CryptoHelper.createEcKeyPair();
        sig.initSign(ecKeyPair.getPrivate());

        String pubKey = CryptoHelper.getEcPubKeyUncompressedOctal(ecKeyPair.getPublic());
        printPayloads(sig, CODE, pubKey, Algorithm.EC256);
    }

    @Test
    public void signRsaTest() throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
        KeyPair rsaKeyPair = CryptoHelper.createRsaKeyPair();
        sig.initSign(rsaKeyPair.getPrivate());

        String pubKey = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
        printPayloads(sig, CODE, pubKey, Algorithm.RSA2048);
    }

    @Test
    public void shortRsaKey() throws Exception {
        KeyPair rsaKeyPair = CryptoHelper.createRsaKeyPair(2000);
        String pubKey = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
        assertThrows(InvalidPublicKeyException.class, () -> new RsaCrypto().getPublicKey(pubKey));
    }

    private void printPayloads(Signature sig, String code, String pubKey, Algorithm algorithm)
            throws SignatureException {
        for (Action action : Action.values()) {
            String signaturePayload =
                    (action.name() + ":" + code + ":" + Instant.now().toEpochMilli());
            sig.update(signaturePayload.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            String signature = Base64.getEncoder().encodeToString(signatureBytes);

            if (Action.REGISTER.equals(action)) {
                System.out.println(
                        String.format(
                                "{\n"
                                        + "  \"code\": \"%s\",\n"
                                        + "  \"publicKey\": \"%s\",\n"
                                        + "  \"algorithm\": \"%s\",\n"
                                        + "  \"signature\": \"%s\",\n"
                                        + "  \"signaturePayload\": \"%s\"\n"
                                        + "}",
                                code, pubKey, algorithm, signature, signaturePayload));
            } else {
                System.out.println(
                        String.format(
                                "{"
                                        + "  \"code\": \"%s\",\n"
                                        + "  \"signature\": \"%s\",\n"
                                        + "  \"signaturePayload\": \"%s\"\n"
                                        + "}",
                                code, signature, signaturePayload));
            }
            System.out.println("---------------------------------------------------------");
        }
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
