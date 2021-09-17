package ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception;

import java.time.Duration;
import java.time.Instant;

public class InvalidTimestampException extends Exception {
    private final Instant acceptedFrom;
    private final Instant acceptedTo;
    private final Instant actual;

    public InvalidTimestampException(Instant now, Duration halfWindow, Instant timestamp) {
        this.acceptedFrom = now.minus(halfWindow);
        this.acceptedTo = now.plus(halfWindow);
        this.actual = timestamp;
    }

    @Override
    public String toString() {
        return "InvalidTimestampException{"
                + "acceptedFrom="
                + acceptedFrom
                + ", acceptedTo="
                + acceptedTo
                + ", actual="
                + actual
                + '}';
    }
}
