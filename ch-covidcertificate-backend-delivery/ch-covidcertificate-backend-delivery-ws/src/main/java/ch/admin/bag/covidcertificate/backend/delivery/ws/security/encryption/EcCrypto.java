package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EcCrypto extends Crypto {

    public static final String SECP256R1 = "secp256r1";
    private static final String EC = "EC";
    private static final String AES = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final String SHA256_WITH_ECDSA = "SHA256withECDSA";

    @Override
    public String encrypt(String toEncrypt, String publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
                    InvalidAlgorithmParameterException, InvalidKeyException,
                    IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException,
                    InvalidParameterSpecException {
        // the ios public key...
        var publicKeyBytes = Base64.getDecoder().decode(publicKey);

        // ... is in uncompressed octal represenation (0x04 | X | Y)
        var x = Arrays.copyOfRange(publicKeyBytes, 1, 33);
        var y = Arrays.copyOfRange(publicKeyBytes, 33, publicKeyBytes.length);

        // ephemeral keys
        var kpg = KeyPairGenerator.getInstance(EC);
        kpg.initialize(256);
        var kp = kpg.generateKeyPair();
        // convert them to uncompressed point form
        byte[] xArray = ((ECPublicKey) kp.getPublic()).getW().getAffineX().toByteArray();
        byte[] yArray = ((ECPublicKey) kp.getPublic()).getW().getAffineY().toByteArray();

        // normalize ec curve point to always be 32 bytes (we always have positive sign, so the
        // leading 00 can be omitted)
        byte[] bobPublic = new byte[65];
        bobPublic[0] = 0x4;
        if (xArray.length == 33) {
            System.arraycopy(xArray, 1, bobPublic, 1, xArray.length - 1);
        } else {
            System.arraycopy(xArray, 0, bobPublic, 1, xArray.length);
        }
        if (yArray.length == 33) {
            System.arraycopy(yArray, 1, bobPublic, 33, yArray.length - 1);
        } else {
            System.arraycopy(yArray, 0, bobPublic, 33, yArray.length);
        }

        // generate a publickey from ios publickeydata
        var kf = KeyFactory.getInstance(EC);
        var ecKeySpec =
                new ECPublicKeySpec(
                        new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                        ecParameterSpecForCurve(SECP256R1));
        var otherPublicKey = kf.generatePublic(ecKeySpec);

        // do ECDH for symmetric key derivation
        var ka = KeyAgreement.getInstance("ECDH");
        ka.init(kp.getPrivate());
        ka.doPhase(otherPublicKey, true);
        var secret = ka.generateSecret();

        // we do a ansi x963 key derivation, as the ecdh secret could sometimes leak infos about the
        // private key
        var derivedSecret = x963KDF(secret, bobPublic);
        // first 16 bytes are aes secret
        var aesSecret = Arrays.copyOfRange(derivedSecret, 0, 16);
        // second 16 bytes are IV
        var iv = Arrays.copyOfRange(derivedSecret, 16, derivedSecret.length);
        var ivSpec = new GCMParameterSpec(128, iv);

        // now we are ready. We can initialize the AES cipher
        var secretKey = new SecretKeySpec(aesSecret, AES);
        var cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        var cipherText = cipher.doFinal(toEncrypt.getBytes(StandardCharsets.UTF_8));

        // in order for SecKeyCreateDecryptedData to be able to decrypt the data, we have to concat
        // the ephemeral public key and the cipher text
        // NOTE: the ios documentation mentions the authenticated date to be the ios public key, but
        // this is WRONG c.f. (https://developer.apple.com/forums/thread/114066,
        // https://darthnull.org/secure-enclave-ecies/)
        byte[] bytes = concatWithArrayCopy(bobPublic, cipherText);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ECParameterSpec ecParameterSpecForCurve(String curveName)
            throws NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameters params = AlgorithmParameters.getInstance(EC);
        params.init(new ECGenParameterSpec(curveName));
        return params.getParameterSpec(ECParameterSpec.class);
    }

    // Simple key derivation c.f https://darthnull.org/secure-enclave-ecies/
    private byte[] x963KDF(byte[] secret, byte[] sharedInfo) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(secret);
        digest.update((byte) 0);
        digest.update((byte) 0);
        digest.update((byte) 0);
        digest.update((byte) 1);
        digest.update(sharedInfo);
        return digest.digest();
    }

    private byte[] concatWithArrayCopy(byte[] array1, byte[] array2) {
        byte[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    @Override
    protected PublicKey getPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidParameterSpecException,
                    InvalidKeySpecException {
        // the ios public key...
        var publicKeyBytes = Base64.getDecoder().decode(publicKey);

        // ... is in uncompressed octal represenation (0x04 | X | Y)
        var x = Arrays.copyOfRange(publicKeyBytes, 1, 33);
        var y = Arrays.copyOfRange(publicKeyBytes, 33, publicKeyBytes.length);

        KeyFactory kf = KeyFactory.getInstance(EC);
        var ecKeySpec =
                new ECPublicKeySpec(
                        new ECPoint(new BigInteger(1, x), new BigInteger(1, y)),
                        EcCrypto.ecParameterSpecForCurve(EcCrypto.SECP256R1));
        return kf.generatePublic(ecKeySpec);
    }

    @Override
    protected Signature getSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance(SHA256_WITH_ECDSA);
    }
}
