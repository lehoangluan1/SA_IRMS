package SA.irms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import SA.irms.gateway.ApiGatewayApplication;

class IrmsApplicationTests {

    @Test
    void applicationMainMethodExists() throws NoSuchMethodException {
        assertNotNull(IrmsApplication.class.getMethod("main", String[].class));
    }

    @Test
    void firstNonBlankIgnoresNullAndBlankCandidates() throws Exception {
        Method firstNonBlank = IrmsApplication.class.getDeclaredMethod("firstNonBlank", String[].class);
        firstNonBlank.setAccessible(true);

        assertEquals(
                "api-gateway",
                firstNonBlank.invoke(null, (Object) new String[]{null, " ", "api-gateway", "reporting-service"})
        );
        assertEquals("", firstNonBlank.invoke(null, (Object) new String[]{null, "", " "}));
    }

    @Test
    void apiGatewayApplicationScansOnlyStatelessCommonPackages() {
        SpringBootApplication annotation = ApiGatewayApplication.class.getAnnotation(SpringBootApplication.class);

        assertNotNull(annotation);
        var scannedPackages = Arrays.asList(annotation.scanBasePackages());
        assertTrue(scannedPackages.contains("SA.irms.gateway"));
        assertTrue(scannedPackages.contains("SA.irms.common.security"));
        assertTrue(scannedPackages.contains("SA.irms.common.identity"));
        assertTrue(scannedPackages.contains("SA.irms.common.error"));
        assertTrue(scannedPackages.stream().noneMatch(packageName -> packageName.equals("SA.irms.common")));
        assertTrue(scannedPackages.stream().noneMatch(packageName -> packageName.startsWith("SA.irms.common.outbox")));
    }
}
