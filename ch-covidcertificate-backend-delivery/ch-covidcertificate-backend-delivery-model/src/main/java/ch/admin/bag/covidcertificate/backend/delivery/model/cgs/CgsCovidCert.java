package ch.admin.bag.covidcertificate.backend.delivery.model.cgs;

import ch.admin.bag.covidcertificate.backend.delivery.model.util.CodeHelper;
import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CgsCovidCert {
    @Documentation(
            description =
                    "client generated alpha numeric code used as identifier during the entire transfer",
            example = "A7KBZ91XL")
    @NotNull
    @Size(min = 9, max = 9)
    private String code;

    @Documentation(description = "covidcert hcert. HC1:<base45>")
    @NotNull
    private String hcert;

    @Documentation(description = "covidcert pdf. base64")
    @NotNull
    private String pdf;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = CodeHelper.getSanitizedCode(code);
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
