package ch.admin.bag.covidcertificate.backend.delivery.ws.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class DeliveryJWTValidator implements OAuth2TokenValidator<Jwt> {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        final var containsClaim = token.containsClaim("resource_access");
        if (Boolean.TRUE.equals(containsClaim)) {
            final var claimMap = token.getClaimAsString("resource_access");
            try {
                if (objectMapper
                        .readTree(claimMap)
                        .get("ch-covidcertificate-backend-delivery-ws")
                        .get("roles")
                        .asText()
                        .contains("certificatecreator")) {
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
                }
            } catch (JsonProcessingException e) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
    }
}
