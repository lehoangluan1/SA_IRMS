package SA.irms.common.json;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.error.DomainException;

@Component
public class JsonSupport {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "json_write_failed",
                    "The system could not serialize the response payload.");
        }
    }

    public Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new DomainException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "json_read_failed",
                    "The system could not parse stored JSON data.");
        }
    }
}
