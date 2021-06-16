package ch.admin.bag.covidcertificate.backend.delivery.ws.security;

import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidActionException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignaturePayloadException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SignaturePayloadValidator {

    public void validate(String signaturePayload, Action expectedAction, String code)
            throws InvalidSignaturePayloadException, InvalidActionException {
        String[] actionCodeTimestamp = signaturePayload.split(":");

        if (actionCodeTimestamp.length != 3) {
            throw new InvalidSignaturePayloadException();
        }

        if (!isCurrentTimestamp(Long.valueOf(actionCodeTimestamp[2]))) {
            throw new InvalidSignaturePayloadException();
        }

        if (!code.equals(actionCodeTimestamp[1])) {
            throw new InvalidSignaturePayloadException();
        }

        if (!expectedAction.equals(Action.forName(actionCodeTimestamp[0]))) {
            throw new InvalidActionException();
        }
    }

    private boolean isCurrentTimestamp(Long epochMilli) {
        Instant now = Instant.now();
        Instant timestamp = Instant.ofEpochMilli(epochMilli);
        return timestamp.isAfter(now.minus(5, ChronoUnit.MINUTES))
                && timestamp.isBefore(now.plus(5, ChronoUnit.MINUTES));
    }
}
