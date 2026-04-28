package SA.irms.nonfunctional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.billing.api.RefundsController;
import SA.irms.billing.application.BillingService;
import SA.irms.common.error.DomainException;
import SA.irms.common.error.ForbiddenException;
import SA.irms.common.error.UnauthorizedException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.security.CurrentUser;
import SA.irms.common.security.PermissionGuard;
import SA.irms.inventory.api.InventoryController;
import SA.irms.inventory.application.InventoryService;
import SA.irms.ordering.api.OrdersController;
import SA.irms.ordering.application.OrdersService;
import SA.irms.reporting.api.DashboardController;
import SA.irms.reporting.application.DashboardService;
import SA.irms.reservation.api.WaitlistController;
import SA.irms.reservation.application.ReservationService;
import SA.irms.common.api.HealthController;
import SA.irms.common.application.HealthStatusService;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

class Section1NonFunctionalContractTest extends ApiContractTestSupport {

    @Mock
    private OrdersService ordersService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private HealthStatusService healthStatusService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private BillingService billingService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PermissionGuard permissionGuard;

    private AutoCloseable mocks;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(8);
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void singleRequestBaselineForCriticalEndpointsStaysInsideSection1Targets() {
        when(ordersService.load(any())).thenReturn(TestFixtures.ordersOverview());
        when(dashboardService.load()).thenReturn(TestFixtures.dashboardView());
        when(healthStatusService.isDatabaseUp()).thenReturn(true);

        MockMvc mockMvc = mockMvcFor(
                new OrdersController(ordersService, permissionGuard),
                new DashboardController(dashboardService, permissionGuard),
                new HealthController(healthStatusService, "irms-api-gateway")
        );

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                mockMvc.perform(get("/api/orders/overview").queryParam("sessionId", TestFixtures.SESSION_ID.toString()))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()));

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                mockMvc.perform(get("/api/dashboard"))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()));

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                mockMvc.perform(get("/api/health"))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()));
    }

    @Test
    void burstOrderRequestsPreserveCorrelationIdsUnderPeakHourStyleLoad() throws Exception {
        when(ordersService.createOrder(any(), any(), anyString(), any())).thenReturn(TestFixtures.ordersOverview());
        MockMvc mockMvc = mockMvcFor(new OrdersController(ordersService, permissionGuard));

        List<Callable<JsonNode>> calls = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            String correlationId = "peak-order-" + index;
            calls.add(() -> {
                String body = mockMvc.perform(post("/api/orders")
                                .contentType(APPLICATION_JSON)
                                .header("X-Correlation-Id", correlationId)
                                .content("""
                                        {
                                          "sessionId": "%s",
                                          "draft": false,
                                          "items": [
                                            {
                                              "menuItemId": "%s",
                                              "quantity": 1
                                            }
                                          ]
                                        }
                                        """.formatted(TestFixtures.SESSION_ID, TestFixtures.MENU_ITEM_ID)))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
                return objectMapper.readTree(body);
            });
        }

        for (JsonNode node : invokeAll(calls)) {
            Assertions.assertTrue(node.path("meta").path("correlationId").asText().startsWith("peak-order-"));
            Assertions.assertEquals(TestFixtures.SESSION_ID.toString(), node.path("data").path("selectedSessionId").asText());
        }
    }

    @Test
    void waitlistSeatingAllowsOneSuccessAndSurfacesConflictsDuringConcurrency() throws Exception {
        AtomicBoolean seated = new AtomicBoolean(false);
        when(reservationService.seatWaitlist(any(), any(), anyString(), any())).thenAnswer(invocation -> {
            if (seated.compareAndSet(false, true)) {
                return TestFixtures.waitlistView();
            }
            throw new DomainException(HttpStatus.CONFLICT, "waitlist_conflict", "Waitlist entry already seated.");
        });

        MockMvc mockMvc = mockMvcFor(new WaitlistController(reservationService, permissionGuard));

        List<Callable<Integer>> calls = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            calls.add(() -> mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/seat", TestFixtures.WAITLIST_ID))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        List<Integer> statuses = invokeAll(calls);
        Assertions.assertEquals(1L, statuses.stream().filter(status -> status == 200).count());
        Assertions.assertEquals(7L, statuses.stream().filter(status -> status == 409).count());
    }

    @Test
    void refundApprovalConcurrencyAvoidsSilentDuplicateSuccess() throws Exception {
        AtomicBoolean decided = new AtomicBoolean(false);
        when(billingService.reviewRefund(any(), any(), any(), anyString(), any())).thenAnswer(invocation -> {
            if (decided.compareAndSet(false, true)) {
                return TestFixtures.refundView();
            }
            throw new DomainException(HttpStatus.CONFLICT, "refund_decision_conflict", "Refund decision already recorded.");
        });

        MockMvc mockMvc = mockMvcFor(new RefundsController(billingService, permissionGuard));

        List<Callable<Integer>> calls = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            calls.add(() -> mockMvc.perform(post("/api/refunds/{refundId}/approval", TestFixtures.REFUND_ID)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "action": "APPROVE",
                                      "reason": "Validated"
                                    }
                                    """))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        List<Integer> statuses = invokeAll(calls);
        Assertions.assertEquals(1L, statuses.stream().filter(status -> status == 200).count());
        Assertions.assertEquals(5L, statuses.stream().filter(status -> status == 409).count());
    }

    @Test
    void inventoryUpdateConcurrencySurfacesConflictsInsteadOfHidingThem() throws Exception {
        AtomicBoolean updated = new AtomicBoolean(false);
        when(inventoryService.updateIngredient(any(), any(), any(), anyString(), any())).thenAnswer(invocation -> {
            if (updated.compareAndSet(false, true)) {
                return TestFixtures.ingredientView();
            }
            throw new DomainException(HttpStatus.CONFLICT, "inventory_conflict", "Inventory item was updated by another request.");
        });

        MockMvc mockMvc = mockMvcFor(new InventoryController(inventoryService, permissionGuard));

        List<Callable<Integer>> calls = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            calls.add(() -> mockMvc.perform(put("/api/inventory/items/{inventoryItemId}", TestFixtures.INVENTORY_ITEM_ID)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Salmon",
                                      "unit": "kg",
                                      "current": 8.5,
                                      "minimum": 2.0,
                                      "maximum": 15.0,
                                      "cost": 18.0,
                                      "category": "Seafood"
                                    }
                                    """))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        List<Integer> statuses = invokeAll(calls);
        Assertions.assertEquals(1L, statuses.stream().filter(status -> status == 200).count());
        Assertions.assertEquals(4L, statuses.stream().filter(status -> status == 409).count());
    }

    @Test
    void correlationHeadersAndSessionPermissionGuardsRemainObservable() throws Exception {
        when(healthStatusService.isDatabaseUp()).thenReturn(true);
        MockMvc healthMvc = mockMvcFor(new HealthController(healthStatusService, "irms-api-gateway"));

        String explicit = healthMvc.perform(get("/api/health").header("X-Correlation-Id", "trace-123"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode explicitNode = objectMapper.readTree(explicit);
        Assertions.assertEquals("trace-123", explicitNode.path("meta").path("correlationId").asText());

        var generatedResponse = healthMvc.perform(get("/api/health")).andReturn().getResponse();
        JsonNode generatedNode = objectMapper.readTree(generatedResponse.getContentAsString());
        Assertions.assertEquals(generatedResponse.getHeader("X-Correlation-Id"), generatedNode.path("meta").path("correlationId").asText());
        Assertions.assertFalse(generatedResponse.getHeader("X-Correlation-Id").isBlank());

        CurrentUser currentUser = new CurrentUser();
        Assertions.assertThrows(UnauthorizedException.class, currentUser::require);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(TestFixtures.USER_ID, "staff@irms.local", "Taylor Staff", java.util.Set.of("staff"), java.util.Set.of("orders.view"), TestFixtures.SESSION_ID),
                null,
                java.util.List.of()
        ));
        PermissionGuard guard = new PermissionGuard(currentUser);
        Assertions.assertThrows(ForbiddenException.class, () -> guard.require("orders.edit"));
    }

    private <T> List<T> invokeAll(List<Callable<T>> calls) throws InterruptedException, ExecutionException {
        List<Future<T>> futures = executor.invokeAll(calls);
        List<T> results = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }
}
