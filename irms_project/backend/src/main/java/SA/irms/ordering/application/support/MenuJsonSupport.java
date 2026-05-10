package SA.irms.ordering.application.support;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MenuJsonSupport {
    public List<String> parseJsonArray(String value) {
        if (value == null || value.isBlank() || "[]".equals(value)) {
            return List.of();
        }
        String cleaned = value.replace("[", "").replace("]", "").replace("\"", "");
        if (cleaned.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    public String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return "[" + values.stream()
                .sorted(Comparator.naturalOrder())
                .map(value -> "\"" + value.replace("\"", "") + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]";
    }
}
