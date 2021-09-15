package ch.admin.bag.covidcertificate.backend.delivery.ws.security.encryption;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class CryptoHelper {

    public static KeyPair createEcKeyPair() throws NoSuchAlgorithmException {
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    public static KeyPair createRsaKeyPair() throws NoSuchAlgorithmException {
        return createRsaKeyPair(2048);
    }

    public static KeyPair createRsaKeyPair(int keyLength) throws NoSuchAlgorithmException {
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keyLength);
        return kpg.generateKeyPair();
    }

    public static String getEcPubKeyUncompressedOctal(PublicKey publicKey) {
        // convert them to uncompressed point form
        byte[] xArray = ((ECPublicKey) publicKey).getW().getAffineX().toByteArray();
        byte[] yArray = ((ECPublicKey) publicKey).getW().getAffineY().toByteArray();

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

        return Base64.getEncoder().encodeToString(bobPublic);
    }
}
