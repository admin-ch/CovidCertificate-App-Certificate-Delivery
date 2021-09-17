package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidPublicKeyException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignatureException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Crypto {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * the given public key is used to encrypt (or derive) a secret key, which is used to encrypt
     * the data with AES/GCM. Afterwards the secret key for the AES-Encryption is encrypted
     * (wrapped) with the public key of the client
     *
     * @param toEncrypt (base64)
     * @param publicKey (base64)
     * @return
     */
    public abstract String encrypt(String toEncrypt, String publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
                    InvalidAlgorithmParameterException, InvalidKeyException,
                    IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException,
                    InvalidParameterSpecException, InvalidPublicKeyException;

    /**
     * validates the signature and signature payload with the given public key
     *
     * @param signaturePayload
     * @param signature
     * @param publicKey
     * @throws InvalidSignatureException
     * @throws InvalidPublicKeyException
     */
    public void validateSignature(String signaturePayload, String signature, String publicKey)
            throws InvalidSignatureException, InvalidPublicKeyException {
        Signature sig = null;
        PublicKey pubKey = null;
        try {
            sig = getSignature();
            pubKey = getPublicKey(publicKey);
        } catch (Exception e) {
            logger.warn("invalid public key", e);
            throw new InvalidPublicKeyException();
        }

        boolean isValid = false;
        try {
            sig.initVerify(pubKey);
            sig.update(signaturePayload.getBytes(StandardCharsets.UTF_8));
            isValid = sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            throw new InvalidSignatureException();
        }
        if (!isValid) {
            throw new InvalidSignatureException();
        }
    }

    protected abstract Signature getSignature() throws NoSuchAlgorithmException;

    protected abstract PublicKey getPublicKey(String publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidParameterSpecException,
                    InvalidPublicKeyException;
}
