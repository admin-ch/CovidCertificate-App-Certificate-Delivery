package ch.admin.bag.covidcertificate.backend.delivery.ws.security;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidActionException;

public enum Action {
    DELETE,
    GET,
    REGISTER;

    public static Action forName(String name) throws InvalidActionException {
        for (Action action : Action.values()) {
            if (action.name().equalsIgnoreCase(name)) {
                return action;
            }
        }
        throw new InvalidActionException();
    }
}
