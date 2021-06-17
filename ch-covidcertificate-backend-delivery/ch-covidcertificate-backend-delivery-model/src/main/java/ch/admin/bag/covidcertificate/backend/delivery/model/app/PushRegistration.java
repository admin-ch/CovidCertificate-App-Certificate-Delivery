package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotNull;

public class PushRegistration {
    @NotNull private String pushToken;
    @NotNull private PushType pushType;
    @JsonIgnore private int id;

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
}
