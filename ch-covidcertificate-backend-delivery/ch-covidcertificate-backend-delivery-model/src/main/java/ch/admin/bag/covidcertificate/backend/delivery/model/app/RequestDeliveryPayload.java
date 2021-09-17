package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.admin.bag.covidcertificate.backend.delivery.model.util.CodeHelper;
import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class RequestDeliveryPayload {
    @Documentation(
            description =
                    "client generated alpha numeric code used as identifier during the entire transfer",
            example = "A7KBZ91XL")
    @NotNull
    @Size(min = 9, max = 9)
    private String code;

    @Documentation(
            description =
                    "payload which was signed with format ${action}:${code}:${unix_timestamp_in_ms}, where action is one of delete|get|register",
            example = "get:A7KBZ91XL:1623051081000")
    @NotNull
    private String signaturePayload;

    @Documentation(description = "the signature calculated over signaturePayload. base64 encoded")
    @NotNull
    private String signature;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = CodeHelper.getSanitizedCode(code);
    }

    public String getSignaturePayload() {
        return signaturePayload;
    }

    public void setSignaturePayload(String signaturePayload) {
        this.signaturePayload = signaturePayload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
