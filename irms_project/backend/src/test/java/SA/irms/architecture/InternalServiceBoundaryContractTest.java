package SA.irms.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class InternalServiceBoundaryContractTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java");
    private static final Path IRMS_SOURCE = MAIN_SOURCE.resolve("SA/irms");
    private static final String REMOVED_LEGACY_PACKAGE = "SA.irms." + "sh" + "ared";
    private static final String REMOVED_LEGACY_DIR = "sh" + "ared";
    private static final Set<String> SERVICE_ROOTS = Set.of(
            "billing",
            "identity",
            "inventory",
            "kitchen",
            "notification",
            "ordering",
            "reporting",
            "reservation"
    );

    @TestFactory
    Stream<DynamicTest> remoteClientsCallInternalEndpoints() {
        return Stream.of(
                fragment("billing/integration/RemotePromotionApplicationClient.java", "/internal/ordering/promotions/apply"),
                fragment("kitchen/integration/RemoteInventoryConsumptionClient.java", "/internal/inventory/consumption/kitchen-start"),
                fragment("kitchen/integration/RemoteOrderStateUpdateClient.java", "/internal/ordering/orders/"),
                fragment("kitchen/integration/RemoteOrderStateUpdateClient.java", "/refresh-status"),
                fragment("kitchen/integration/RemoteOrderStateUpdateClient.java", "/line-statuses"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/internal/kitchen/orders/"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/tickets"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/order-items/"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/hold"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/items/release"),
                fragment("ordering/integration/RemoteKitchenOrderRoutingClient.java", "/block"),
                fragment("common/identity/RemoteSharedIdentitySessionClient.java", "/internal/identity/sessions/by-token-hash/"),
                fragment("common/identity/RemoteSharedIdentitySessionClient.java", "/touch"),
                fragment("common/identity/RemoteSharedIdentityPolicyClient.java", "/internal/identity/policy-snapshot"),
                fragment("common/identity/RemoteSharedIdentityPolicyClient.java", "/internal/identity/default-branch"),
                fragment("common/identity/RemoteSharedIdentityDirectoryClient.java", "/internal/identity/users/display-names"),
                fragment("common/identity/RemoteSharedIdentityDirectoryClient.java", "/internal/identity/users/first-active-by-role-priority")
        ).map(expectation -> DynamicTest.dynamicTest(
                expectation.file() + " contains " + expectation.fragment(),
                () -> assertTrue(source(expectation.file()).contains(expectation.fragment()))
        ));
    }

    @TestFactory
    Stream<DynamicTest> internalControllersExposeMatchingEndpoints() {
        return Stream.of(
                fragment("ordering/api/OrderingIntegrationController.java", "@RequestMapping(\"/internal/ordering\")"),
                fragment("ordering/api/OrderingIntegrationController.java", "@PostMapping(\"/orders/{orderId}/refresh-status\")"),
                fragment("ordering/api/OrderingIntegrationController.java", "@PostMapping(\"/orders/{orderId}/line-statuses\")"),
                fragment("ordering/api/OrderingIntegrationController.java", "@PostMapping(\"/promotions/apply\")"),
                fragment("kitchen/api/KitchenIntegrationController.java", "@RequestMapping(\"/internal/kitchen\")"),
                fragment("kitchen/api/KitchenIntegrationController.java", "@PostMapping(\"/orders/{orderId}/tickets\")"),
                fragment("kitchen/api/KitchenIntegrationController.java", "@PostMapping(\"/order-items/{orderItemId}/hold\")"),
                fragment("kitchen/api/KitchenIntegrationController.java", "@PostMapping(\"/orders/{orderId}/items/release\")"),
                fragment("kitchen/api/KitchenIntegrationController.java", "@PostMapping(\"/order-items/{orderItemId}/block\")"),
                fragment("inventory/api/InventoryIntegrationController.java", "@RequestMapping(\"/internal/inventory\")"),
                fragment("inventory/api/InventoryIntegrationController.java", "@PostMapping(\"/consumption/kitchen-start\")"),
                fragment("identity/api/IdentityInternalController.java", "@RequestMapping(\"/internal/identity\")"),
                fragment("identity/api/IdentityInternalController.java", "@GetMapping(\"/sessions/by-token-hash/{tokenHash}\")"),
                fragment("identity/api/IdentityInternalController.java", "@PostMapping(\"/sessions/{sessionId}/touch\")"),
                fragment("identity/api/IdentityInternalController.java", "@GetMapping(\"/policy-snapshot\")"),
                fragment("identity/api/IdentityInternalController.java", "@GetMapping(\"/default-branch\")"),
                fragment("identity/api/IdentityInternalController.java", "@PostMapping(\"/users/display-names\")"),
                fragment("identity/api/IdentityInternalController.java", "@PostMapping(\"/users/first-active-by-role-priority\")")
        ).map(expectation -> DynamicTest.dynamicTest(
                expectation.file() + " exposes " + expectation.fragment(),
                () -> assertTrue(source(expectation.file()).contains(expectation.fragment()))
        ));
    }

    @TestFactory
    Stream<DynamicTest> servicePackagesDoNotImportOtherServicePackages() {
        return SERVICE_ROOTS.stream()
                .map(service -> DynamicTest.dynamicTest(service + " has no direct imports from another service package", () -> {
                    for (Path file : javaFiles(IRMS_SOURCE.resolve(service))) {
                        String text = Files.readString(file);
                        for (String other : SERVICE_ROOTS) {
                            if (!other.equals(service)) {
                                assertFalse(
                                        text.contains("import SA.irms." + other + "."),
                                        () -> file + " imports service package " + other
                                );
                            }
                        }
                    }
                }));
    }

    @Test
    void removedBackendPackageIsGone() throws IOException {
        assertFalse(Files.exists(IRMS_SOURCE.resolve(REMOVED_LEGACY_DIR)));
        for (Path file : javaFiles(IRMS_SOURCE)) {
            String text = Files.readString(file);
            assertFalse(text.contains(REMOVED_LEGACY_PACKAGE), () -> file + " still references removed package");
        }
    }

    @Test
    void runtimeBootstrapsDoNotScanSharedPackage() throws IOException {
        for (Path file : javaFiles(IRMS_SOURCE.resolve("runtime"))) {
            assertFalse(Files.readString(file).contains(REMOVED_LEGACY_PACKAGE), () -> file + " still scans removed package");
        }
    }

    private static ExpectedFragment fragment(String file, String fragment) {
        return new ExpectedFragment(file, fragment);
    }

    private static String source(String relativeFile) throws IOException {
        return Files.readString(IRMS_SOURCE.resolve(relativeFile));
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private record ExpectedFragment(String file, String fragment) {
    }
}
