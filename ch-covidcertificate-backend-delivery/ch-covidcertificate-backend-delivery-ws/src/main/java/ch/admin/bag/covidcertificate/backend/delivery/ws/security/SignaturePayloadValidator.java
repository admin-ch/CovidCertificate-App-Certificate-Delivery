package ch.admin.bag.covidcertificate.backend.delivery.ws.security;

import ch.admin.bag.covidcertificate.backend.delivery.model.util.CodeHelper;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidActionException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidSignaturePayloadException;
import ch.admin.bag.covidcertificate.backend.delivery.ws.security.exception.InvalidTimestampException;
import java.time.Duration;
import java.time.Instant;

public class SignaturePayloadValidator {
    private final Duration halfWindow;

    public SignaturePayloadValidator(Duration halfWindow) {
        this.halfWindow = halfWindow;
    }

    public void validate(String signaturePayload, Action expectedAction, String code)
            throws InvalidSignaturePayloadException, InvalidActionException,
                    InvalidTimestampException {
        String[] signaturePayloadSplit = signaturePayload.split(":");

        if (signaturePayloadSplit.length != 3) {
            throw new InvalidSignaturePayloadException();
        }

        validateTimestamp(Long.valueOf(signaturePayloadSplit[2]));
/*
        if (!code.equals(CodeHelper.getSanitizedCode(signaturePayloadSplit[1]))) {
            throw new InvalidSignaturePayloadException();
        }*/
        //TODO define new payload

        if (!expectedAction.equals(Action.forName(signaturePayloadSplit[0]))) {
            throw new InvalidActionException();
        }
    }

    private void validateTimestamp(Long epochMilli) throws InvalidTimestampException {
        Instant now = Instant.now();
        Instant timestamp = Instant.ofEpochMilli(epochMilli);
        if (timestamp.isBefore(now.minus(halfWindow)) || timestamp.isAfter(now.plus(halfWindow))) {
            throw new InvalidTimestampException(now, halfWindow, timestamp);
        }
    }
}
