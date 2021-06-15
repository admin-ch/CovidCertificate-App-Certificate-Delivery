package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public interface Crypto {

    /**
     * the given public key is used to encrypt (or derive) a secret key, which is used to encrypt
     * the data with AES/GCM. Afterwards the secret key for the AES-Encryption is encrypted
     * (wrapped) with the public key of the client
     *
     * @param toEncrypt (base64)
     * @param publicKey (base64)
     * @return
     */
    public String encrypt(String toEncrypt, String publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidParameterSpecException;
}
