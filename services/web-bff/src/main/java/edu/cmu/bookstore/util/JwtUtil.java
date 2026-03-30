package edu.cmu.bookstore.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cmu.bookstore.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Component
public class JwtUtil {

    private static final Set<String> VALID_SUBJECTS =
            Set.of("starlord", "gamora", "drax", "rocket", "groot");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validateAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Missing Authorization header.");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid Authorization header.");
        }

        String token = authorizationHeader.substring(7).trim();
        validateToken(token);
    }

    private void validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("Invalid JWT token.");
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            JsonNode payload = objectMapper.readTree(payloadJson);

            String sub = payload.path("sub").asText(null);
            String iss = payload.path("iss").asText(null);
            JsonNode expNode = payload.get("exp");

            if (sub == null || !VALID_SUBJECTS.contains(sub)) {
                throw new UnauthorizedException("Invalid JWT token.");
            }

            if (iss == null || !"cmu.edu".equals(iss)) {
                throw new UnauthorizedException("Invalid JWT token.");
            }

            if (expNode == null || !expNode.isNumber()) {
                throw new UnauthorizedException("Invalid JWT token.");
            }

            long exp = expNode.asLong();
            long now = Instant.now().getEpochSecond();

            if (exp <= now) {
                throw new UnauthorizedException("JWT token expired.");
            }
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid JWT token.");
        }
    }
}