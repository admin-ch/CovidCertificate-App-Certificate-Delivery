package ch.admin.bag.covidcertificate.backend.delivery.model.app;

import javax.validation.constraints.NotNull;

public class PushRegistration {
    @NotNull private String pushToken;
    @NotNull private PushType pushType;

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
}
