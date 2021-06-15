package ch.admin.bag.covidcertificate.backend.delivery.data.impl;

import ch.admin.bag.covidcertificate.backend.delivery.model.app.PushRegistration;

public class PushRegistrationWrapper {

    private final int id;
    private final PushRegistration pushRegistration;

    public PushRegistrationWrapper(int id,
        PushRegistration pushRegistration) {
        this.id = id;
        this.pushRegistration = pushRegistration;
    }

    public int getId() {
        return id;
    }

    public PushRegistration getPushRegistration() {
        return pushRegistration;
    }
}
