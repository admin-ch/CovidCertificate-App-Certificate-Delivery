package ch.admin.bag.covidcertificate.backend.delivery.ws.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class DeliveryJWTValidator implements OAuth2TokenValidator<Jwt> {
    private final String certificateCreatorRole;
    private final String resourceAccessPath;
    private final String rolePath;
    ObjectMapper objectMapper = new ObjectMapper();

    public DeliveryJWTValidator(String resourceAccessPath, String rolePath, String role) {
        this.resourceAccessPath = resourceAccessPath;
        this.rolePath = rolePath;
        this.certificateCreatorRole = role;
    }


    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {

        final var containsClaim = token.containsClaim(resourceAccessPath);
        if (Boolean.TRUE.equals(containsClaim)) {
            final var claimMap = token.getClaimAsString(resourceAccessPath);
            try {
                final var roles =
                        objectMapper
                                .readTree(claimMap)
                                .at(rolePath);
                if (!roles.isArray()) {
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN));
                }
                final var arrayNode = (ArrayNode) roles;
                for (JsonNode element : arrayNode) {
                    if (certificateCreatorRole.equals(element.textValue())) {
                        return OAuth2TokenValidatorResult.success();
                    }
                }
            } catch (JsonProcessingException e) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN));
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
    }
}
