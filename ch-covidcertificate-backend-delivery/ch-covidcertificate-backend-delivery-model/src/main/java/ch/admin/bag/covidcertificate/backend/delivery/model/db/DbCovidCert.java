package ch.admin.bag.covidcertificate.backend.delivery.model.db;

import java.time.Instant;

public class DbCovidCert {
    private Integer pk;
    private Integer fkTransfer;
    private Instant createdAt;
    private String encryptedHcert;
    private String encryptedPdf;

    public Integer getPk() {
        return pk;
    }

    public void setPk(Integer pk) {
        this.pk = pk;
    }

    public Integer getFkTransfer() {
        return fkTransfer;
    }

    public void setFkTransfer(Integer fkTransfer) {
        this.fkTransfer = fkTransfer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

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
