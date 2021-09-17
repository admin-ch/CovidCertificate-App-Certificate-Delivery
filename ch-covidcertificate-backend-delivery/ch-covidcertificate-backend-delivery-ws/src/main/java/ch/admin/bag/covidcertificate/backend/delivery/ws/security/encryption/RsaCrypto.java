package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class RsaCrypto extends Crypto {

    private final SecureRandom secureRandom;
    private static final int MIN_KEY_LENGTH = 2048;

    public RsaCrypto() {
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String toEncrypt, String publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
                    InvalidAlgorithmParameterException, InvalidKeyException,
                    IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException,
                    InvalidPublicKeyException {
        RSAPublicKey rsaPubKey = (RSAPublicKey) getPublicKey(publicKey);

        // generate random secret and random IV
        byte[] keyBytes = new byte[32];
        byte[] customIV = new byte[12];
        secureRandom.nextBytes(keyBytes);
        secureRandom.nextBytes(customIV);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // initialize AES engine
        var gcmSpec = new GCMParameterSpec(128, customIV);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        // encrypt data with block cipher
        var encryptedData = cipher.doFinal(toEncrypt.getBytes(StandardCharsets.UTF_8));

        var secretBytes = secretKey.getEncoded();

        // initialize RSA cipher for secretkey and iv encryption
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPwithSHA-256andMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, rsaPubKey);

        // combine key and IV
        var ivAndBytes = new byte[customIV.length + secretBytes.length];
        System.arraycopy(customIV, 0, ivAndBytes, 0, customIV.length);
        System.arraycopy(secretBytes, 0, ivAndBytes, customIV.length, secretBytes.length);

        // encrypt secretkey with public key
        var encryptedKey = rsa.doFinal(ivAndBytes);

        // copy all data into one array
        var all = new byte[encryptedData.length + encryptedKey.length];
        System.arraycopy(encryptedKey, 0, all, 0, encryptedKey.length);

        System.arraycopy(encryptedData, 0, all, encryptedKey.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(all);
    }

    @Override
    protected PublicKey getPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidPublicKeyException {
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pk = kf.generatePublic(spec);
        validateKeyLength((RSAPublicKey) pk);
        return pk;
    }

    private void validateKeyLength(RSAPublicKey pk) throws InvalidPublicKeyException {
        int keyLength = pk.getModulus().bitLength();
        if (keyLength < MIN_KEY_LENGTH) {
            throw new InvalidPublicKeyException();
        }
    }

    @Override
    protected Signature getSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
    }
}
