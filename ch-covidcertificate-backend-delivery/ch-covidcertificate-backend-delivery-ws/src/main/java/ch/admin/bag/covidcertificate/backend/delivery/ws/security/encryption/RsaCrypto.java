package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RsaCrypto extends Crypto {

    private static final Logger logger = LoggerFactory.getLogger(RsaCrypto.class);

    @Override
    public String encrypt(String toEncrypt, String publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
                    InvalidAlgorithmParameterException, InvalidKeyException,
                    IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        RSAPublicKey rsaPubKey = (RSAPublicKey) getPublicKey(publicKey);

        // generate random secret and random IV
        SecureRandom secureRandom = new SecureRandom();
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
        var bytesAndIv = new byte[secretBytes.length + customIV.length];
        System.arraycopy(secretBytes, 0, bytesAndIv, 0, secretBytes.length);
        System.arraycopy(customIV, 0, bytesAndIv, secretBytes.length, customIV.length);

        // encrypt secretkey with public key
        var encryptedKey = rsa.doFinal(bytesAndIv);

        // copy all data into one array
        var all = new byte[encryptedData.length + encryptedKey.length];
        System.arraycopy(encryptedKey, 0, all, 0, encryptedKey.length);

        System.arraycopy(encryptedData, 0, all, encryptedKey.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(all);
    }

    @Override
    protected PublicKey getPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    @Override
    protected Signature getSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance("SHA256withRSA/PSS", new BouncyCastleProvider());
    }
}
