package SA.irms.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.config.AppProperties;

@Service
public class SignedAccessTokenService {
    private static final String PREFIX = "irms-at.v1";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public SignedAccessTokenService(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String issue(SignedAccessTokenClaims claims) {
        try {
            String payload = base64Url(objectMapper.writeValueAsBytes(Map.of(
                    "sid", claims.sessionId().toString(),
                    "uid", claims.userId().toString(),
                    "un", safe(claims.username()),
                    "dn", safe(claims.displayName()),
                    "roles", claims.roles() == null ? Set.of() : claims.roles(),
                    "perms", claims.permissions() == null ? Set.of() : claims.permissions(),
                    "iat", claims.issuedAt().toString(),
                    "exp", claims.expiresAt().toString()
            )));
            String signature = base64Url(sign(PREFIX + "." + payload));
            return PREFIX + "." + payload + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("Access token could not be issued.", exception);
        }
    }

    public Optional<SignedAccessTokenClaims> verify(String token, Instant now) {
        if (token == null || !token.startsWith(PREFIX + ".")) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 4 || !PREFIX.equals(parts[0] + "." + parts[1])) {
            return Optional.empty();
        }
        String payload = parts[2];
        String signature = parts[3];
        if (!constantTimeEquals(signature, base64Url(sign(PREFIX + "." + payload)))) {
            return Optional.empty();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(Base64.getUrlDecoder().decode(payload), MAP_TYPE);
            Instant expiresAt = Instant.parse(string(values.get("exp")));
            if (!expiresAt.isAfter(now)) {
                return Optional.empty();
            }
            return Optional.of(new SignedAccessTokenClaims(
                    UUID.fromString(string(values.get("sid"))),
                    UUID.fromString(string(values.get("uid"))),
                    string(values.get("un")),
                    string(values.get("dn")),
                    stringSet(values.get("roles")),
                    stringSet(values.get("perms")),
                    Instant.parse(string(values.get("iat"))),
                    expiresAt
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private byte[] sign(String material) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Access token signature could not be computed.", exception);
        }
    }

    private String secret() {
        String secret = properties.security().accessTokenSigningSecret();
        if (secret == null || secret.isBlank()) {
            return properties.security().internalServiceToken();
        }
        return secret;
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Set<String> stringSet(Object value) {
        if (value instanceof Iterable<?> iterable) {
            java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
            for (Object item : iterable) {
                result.add(String.valueOf(item));
            }
            return Set.copyOf(result);
        }
        return Set.of();
    }
}
