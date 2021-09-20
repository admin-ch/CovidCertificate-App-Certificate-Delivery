package ch.admin.bag.covidcertificate.backend.delivery.data.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtil {
    private static final String SHA_256 = "SHA-256";

    private HashUtil() {}

    public static String getSha256Hash(String toHash) throws NoSuchAlgorithmException {
        final var digest = MessageDigest.getInstance(SHA_256);
        return Base64.getEncoder().encodeToString(digest.digest(toHash.getBytes()));
    }
}
