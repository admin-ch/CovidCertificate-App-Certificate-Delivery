package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import javax.validation.constraints.NotNull;

public class PushRegistration {
    @Documentation(description = "if null or blank it is a deregistration payload")
    private String pushToken;

    @NotNull private PushType pushType;
    @NotNull private String registerId;
    @JsonIgnore private int id;
    @JsonIgnore private Instant lastPush;

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public PushType getPushType() {
        return pushType;
    }

    public void setPushType(PushType pushType) {
        this.pushType = pushType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public Instant getLastPush() {
        return lastPush;
    }

    public void setLastPush(Instant lastPush) {
        this.lastPush = lastPush;
    }
}
