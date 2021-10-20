package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.ubique.openapi.docannotations.Documentation;
import java.time.Instant;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class DeliveryRegistrationResult {
  @Documentation(
      description =
          "server generated alpha numeric code used as identifier during the entire transfer",
      example = "A7KBZ91XL")
  @NotNull
  @Size(min = 9, max = 9)
  private String code;

  @Documentation(
      description =
          "Timestamp after which the server will invalidate and delete the transfer code"
  )

  @NotNull
  private Instant expiresAt;

  @NotNull
  private Instant failsAt;

  public DeliveryRegistrationResult(String code, Instant expiresAt, Instant failsAt){
    this.code = code;
    this.expiresAt = expiresAt;
    this.failsAt = failsAt;
  }

  public DeliveryRegistrationResult() {}


  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getFailsAt() {
    return failsAt;
  }

  public void setFailsAt(Instant failsAt) {
    this.failsAt = failsAt;
  }
}
