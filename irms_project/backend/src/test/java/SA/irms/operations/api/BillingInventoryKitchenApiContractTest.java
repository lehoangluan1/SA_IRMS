package SA.irms.operations.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.billing.api.BillingController;
import SA.irms.billing.api.BillsController;
import SA.irms.billing.api.PaymentsController;
import SA.irms.billing.api.RefundsController;
import SA.irms.billing.application.BillingService;
import SA.irms.common.error.DomainException;
import SA.irms.common.security.PermissionGuard;
import SA.irms.inventory.api.InventoryController;
import SA.irms.inventory.application.InventoryService;
import SA.irms.kitchen.api.KitchenController;
import SA.irms.kitchen.application.KitchenService;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class BillingInventoryKitchenApiContractTest extends ApiContractTestSupport {

    @Mock
    private BillingService billingService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private KitchenService kitchenService;
    @Mock
    private PermissionGuard permissionGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
        mockMvc = mockMvcFor(
                new BillingController(billingService, permissionGuard),
                new BillsController(billingService, permissionGuard),
                new PaymentsController(billingService, permissionGuard),
                new RefundsController(billingService, permissionGuard),
                new InventoryController(inventoryService, permissionGuard),
                new KitchenController(kitchenService, permissionGuard)
        );
    }

    @Test
    void billingOverviewAndBillOperationsCoverCoreHappyPaths() throws Exception {
        when(billingService.load(any(), any())).thenReturn(TestFixtures.billingOverview());
        when(billingService.createBill(any())).thenReturn(TestFixtures.billView());
        when(billingService.updateBill(any(), any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.billView());
        when(billingService.applyPromotion(any(), anyString())).thenReturn(TestFixtures.billView());
        when(billingService.splitBill(any(), any())).thenReturn(TestFixtures.billView());
        when(billingService.processPayment(any(), any(), any())).thenReturn(TestFixtures.paymentView());

        mockMvc.perform(get("/api/billing/overview").queryParam("sessionId", TestFixtures.SESSION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.data.currentBill.id").value(TestFixtures.BILL_ID.toString()))
                .andExpect(jsonPath("$.data.refundQueue[0].status").value("PENDING"));

        mockMvc.perform(post("/api/bills")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tableSessionId": "%s"
                                }
                                """.formatted(TestFixtures.SESSION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tableNumber").value(12));

        mockMvc.perform(patch("/api/bills/{billId}", TestFixtures.BILL_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipAmount": 3.00,
                                  "discountAmount": 1.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(38.10));

        mockMvc.perform(post("/api/bills/{billId}/promotions", TestFixtures.BILL_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "LUNCH10"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bills/{billId}/splits", TestFixtures.BILL_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "method": "EVEN",
                                  "splitCount": 2,
                                  "amounts": [18.00, 20.10],
                                  "tipAmounts": [1.50, 1.50]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.splits[0].id").value(TestFixtures.SPLIT_ID.toString()));

        mockMvc.perform(post("/api/bills/{billId}/payments", TestFixtures.BILL_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "splitId": "%s",
                                  "method": "CARD",
                                  "amount": 18.00,
                                  "amountReceived": 18.00
                                }
                                """.formatted(TestFixtures.SPLIT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.PAYMENT_ID.toString()));
    }

    @Test
    void paymentReceiptAndRefundEndpointsReturnExpectedPayloads() throws Exception {
        when(billingService.issueReceipt(any(), any())).thenReturn(TestFixtures.receiptView());
        when(billingService.buildReceiptDocument(any())).thenReturn("pdf".getBytes(StandardCharsets.UTF_8));
        when(billingService.refundPayment(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.refundView());
        when(billingService.pendingRefunds()).thenReturn(java.util.List.of(TestFixtures.refundView()));
        when(billingService.reviewRefund(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.refundView());

        mockMvc.perform(post("/api/payments/{paymentId}/receipt", TestFixtures.PAYMENT_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "EMAIL",
                                  "recipientAddress": "guest@irms.local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel").value("EMAIL"));

        mockMvc.perform(get("/api/payments/{paymentId}/receipt/document", TestFixtures.PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("receipt-" + TestFixtures.PAYMENT_ID)));

        mockMvc.perform(post("/api/payments/{paymentId}/refunds", TestFixtures.PAYMENT_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5.00,
                                  "reason": "Overcharge"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.REFUND_ID.toString()));

        mockMvc.perform(get("/api/refunds/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reason").value("Overcharge"));

        mockMvc.perform(post("/api/refunds/{refundId}/approval", TestFixtures.REFUND_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "reason": "Validated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void inventoryEndpointsCoverCreateUpdateDeleteAndAlertAcknowledgement() throws Exception {
        when(inventoryService.load()).thenReturn(TestFixtures.inventoryOverview());
        when(inventoryService.createIngredient(any())).thenReturn(TestFixtures.ingredientView());
        when(inventoryService.updateIngredient(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.ingredientView());

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Salmon"))
                .andExpect(jsonPath("$.data.alerts[0].status").value("OPEN"));

        String payload = """
                {
                  "name": "Salmon",
                  "unit": "kg",
                  "current": 8.5,
                  "minimum": 2.0,
                  "maximum": 15.0,
                  "cost": 18.0,
                  "category": "Seafood"
                }
                """;

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.INVENTORY_ITEM_ID.toString()));

        mockMvc.perform(put("/api/inventory/items/{inventoryItemId}", TestFixtures.INVENTORY_ITEM_ID)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("Seafood"));

        mockMvc.perform(delete("/api/inventory/items/{inventoryItemId}", TestFixtures.INVENTORY_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("inventoryItem"));

        mockMvc.perform(patch("/api/inventory/alerts/{alertId}/acknowledge", TestFixtures.ALERT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("acknowledged"));
    }

    @Test
    void kitchenOverviewAndTicketOperationsStayOnPublicContract() throws Exception {
        when(kitchenService.load(any())).thenReturn(TestFixtures.kitchenOverview());
        when(kitchenService.updateTicketStatus(any(), anyString(), any(), anyString())).thenReturn(TestFixtures.ticketView());
        when(kitchenService.updateItemStatus(any(), anyString(), anyString(), any(), anyString(), any())).thenReturn(TestFixtures.ticketView());
        when(kitchenService.togglePriority(any(), anyString(), any(), anyString(), any())).thenReturn(TestFixtures.ticketView());
        when(kitchenService.cancelTicket(any(), anyString(), any(), anyString(), any())).thenReturn(TestFixtures.ticketView());
        when(kitchenService.serveTicket(any(), any(), anyString())).thenReturn(TestFixtures.ticketView());
        when(kitchenService.returnTicket(any(), anyString(), any(), anyString(), any())).thenReturn(TestFixtures.ticketView());

        mockMvc.perform(get("/api/kitchen/overview").queryParam("station", "grill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stations[0]").value("grill"))
                .andExpect(jsonPath("$.data.tickets[0].items[0].status").value("READY"));

        mockMvc.perform(patch("/api/kitchen/tickets/{ticketId}/status", TestFixtures.TICKET_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY",
                                  "reason": "Plated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.TICKET_ID.toString()));

        mockMvc.perform(patch("/api/kitchen/items/{ticketItemId}/status", TestFixtures.TICKET_ITEM_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY",
                                  "reason": "Cooked"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/kitchen/tickets/{ticketId}/priority", TestFixtures.TICKET_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "VIP guest"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/kitchen/tickets/{ticketId}/cancel", TestFixtures.TICKET_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Customer cancelled"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/kitchen/tickets/{ticketId}/serve", TestFixtures.TICKET_ID))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/kitchen/tickets/{ticketId}/return", TestFixtures.TICKET_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Remake requested"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void billingAndInventoryValidationErrorsRemainVisible() throws Exception {
        when(billingService.reviewRefund(any(), any(), any(), anyString(), any()))
                .thenThrow(new DomainException(HttpStatus.CONFLICT, "refund_locked", "Refund is already being reviewed."));

        mockMvc.perform(post("/api/bills")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tableSessionId"));

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "unit": "kg",
                                  "current": 8.5,
                                  "minimum": 2.0,
                                  "maximum": 15.0,
                                  "cost": 18.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")));

        mockMvc.perform(post("/api/refunds/{refundId}/approval", TestFixtures.REFUND_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "APPROVE",
                                  "reason": "Validated"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("refund_locked"))
                .andExpect(jsonPath("$.message").value("Refund is already being reviewed."));
    }
}
