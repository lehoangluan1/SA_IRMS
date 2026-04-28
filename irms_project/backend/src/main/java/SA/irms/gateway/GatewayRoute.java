package SA.irms.gateway;

import java.util.function.Function;
import java.util.function.Predicate;

record GatewayRoute(Predicate<GatewayRequestDescriptor> matcher, Function<GatewayRoutesProperties, String> targetBaseUrl) {
    boolean matches(String path, String method) {
        return matcher.test(new GatewayRequestDescriptor(path, method));
    }

    String target(GatewayRoutesProperties properties) {
        return targetBaseUrl.apply(properties);
    }
}

record GatewayRequestDescriptor(String path, String method) {
    boolean startsWithAny(String... prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    boolean equalsPath(String expectedPath) {
        return path.equals(expectedPath);
    }

    boolean methodIs(String expectedMethod) {
        return expectedMethod.equalsIgnoreCase(method);
    }
}
