package ch.admin.bag.covidcertificate.backend.delivery.data.exception;

public class PublicKeyAlreadyExistsException extends Exception {
    private final String publicKey;
    private final String code;

    public PublicKeyAlreadyExistsException(String publicKey, String code) {
        this.publicKey = publicKey;
        this.code = code;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getCode() {
        return code;
    }
}
