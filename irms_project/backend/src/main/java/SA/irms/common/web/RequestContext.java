package SA.irms.common.web;

import java.util.UUID;

import SA.irms.common.context.RequestMetadata;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestContext {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = "requestCorrelationId";

    private RequestContext() {
    }

    public static RequestMetadata metadata(HttpServletRequest request) {
        if (request == null) {
            return RequestMetadata.empty();
        }
        return new RequestMetadata(
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("X-Request-Id"),
                request.getLocale() == null ? null : request.getLocale().toLanguageTag()
        );
    }

    public static String getCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }
}
