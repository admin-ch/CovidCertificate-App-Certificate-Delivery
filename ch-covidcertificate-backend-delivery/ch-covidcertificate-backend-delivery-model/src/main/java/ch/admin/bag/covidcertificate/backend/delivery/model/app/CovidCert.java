package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;

public class CovidCert {
    @Documentation(description = "covidcert hcert encrypted with client public key. base64")
    @NotNull
    private String encryptedHcert;

    @Documentation(description = "covidcert pdf encrypted with client public key. base64")
    @NotNull
    private String encryptedPdf;

    public String getEncryptedHcert() {
        return encryptedHcert;
    }

    public void setEncryptedHcert(String encryptedHcert) {
        this.encryptedHcert = encryptedHcert;
    }

    public String getEncryptedPdf() {
        return encryptedPdf;
    }

    public void setEncryptedPdf(String encryptedPdf) {
        this.encryptedPdf = encryptedPdf;
    }
}
