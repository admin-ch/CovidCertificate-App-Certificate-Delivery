package ch.admin.bag.covidcertificate.backend.delivery.model.db;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.Algorithm;
import java.time.Instant;

public class DbTransfer {
    private Integer pk;
    private Instant createdAt;
    private String code;
    private String publicKey;
    private String publicKeySha256; // base64 encoded
    private Algorithm algorithm;

    public Integer getPk() {
        return pk;
    }

    public void setPk(Integer pk) {
        this.pk = pk;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeySha256() {
        return publicKeySha256;
    }

    public void setPublicKeySha256(String publicKeySha256) {
        this.publicKeySha256 = publicKeySha256;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }
}
