package ch.admin.bag.covidcertificate.backend.delivery.ws.security.signature;

public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException(Throwable cause) {
        super(cause);
    }
}
