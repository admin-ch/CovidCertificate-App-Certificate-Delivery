package ch.admin.bag.covidcertificate.backend.delivery.model.cgs;

import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;

public class CgsCovidCert {
    @Documentation(
            description =
                    "client generated alpha numeric code used as identifier during the entire transfer")
    @NotNull
    private String code;

    @Documentation(description = "covidcert hcert. HC1:<base64>")
    @NotNull
    private String hcert;

    @Documentation(description = "covidcert pdf. base64")
    @NotNull
    private String pdf;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHcert() {
        return hcert;
    }

    public void setHcert(String hcert) {
        this.hcert = hcert;
    }

    public String getPdf() {
        return pdf;
    }

    public void setPdf(String pdf) {
        this.pdf = pdf;
    }
}
