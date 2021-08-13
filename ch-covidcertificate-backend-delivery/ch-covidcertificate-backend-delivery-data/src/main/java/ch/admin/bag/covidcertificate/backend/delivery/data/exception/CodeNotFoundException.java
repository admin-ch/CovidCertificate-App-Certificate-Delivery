package ch.admin.bag.covidcertificate.backend.delivery.data.exception;

public class CodeNotFoundException extends Exception {
    private String code;

    public CodeNotFoundException(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
