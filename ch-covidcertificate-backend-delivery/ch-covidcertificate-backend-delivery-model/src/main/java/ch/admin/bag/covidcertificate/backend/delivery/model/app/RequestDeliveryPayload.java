package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;

public class RequestDeliveryPayload {
    @Documentation(
            description =
                    "client generated alpha numeric code used as identifier during the entire transfer")
    @NotNull
    private String code;

    @Documentation(description = "signature for validation. base64")
    @NotNull
    private String signature;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
