package SA.irms.ordering.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.common.security.PermissionGuard;
import SA.irms.ordering.application.MenuService;
import SA.irms.ordering.application.OrdersService;
import SA.irms.ordering.application.command.OrderCommands;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class OrderingApiContractTest extends ApiContractTestSupport {

    @Mock
    private OrdersService ordersService;
    @Mock
    private MenuService menuService;
    @Mock
    private PermissionGuard permissionGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
        mockMvc = mockMvcFor(
                new OrdersController(ordersService, permissionGuard),
                new MenuController(menuService, permissionGuard),
                new MenuCategoryController(menuService, permissionGuard),
                new MenuItemController(menuService, permissionGuard),
                new MenuPromotionController(menuService, permissionGuard)
        );
    }

    @Test
    void ordersOverviewReturnsMenuItemsAndCombos() throws Exception {
        when(ordersService.load(any())).thenReturn(TestFixtures.ordersOverview());

        mockMvc.perform(get("/api/orders/overview").queryParam("sessionId", TestFixtures.SESSION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.data.selectedSessionId").value(TestFixtures.SESSION_ID.toString()))
                .andExpect(jsonPath("$.data.menuItems[0].modifierGroups[0].options[0].name").value("Lemon Butter"))
                .andExpect(jsonPath("$.data.combos[0].groups[0].options[0].menuItemName").value("Iced Tea"));
    }

    @Test
    void createOrderCapturesComboSelectionsFromFrontendPayload() throws Exception {
        when(ordersService.createOrder(any(), any(), anyString(), any())).thenReturn(TestFixtures.ordersOverview());

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "specialInstructions": "Rush if possible",
                                  "draft": false,
                                  "items": [
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 2,
                                      "note": "No peanuts",
                                      "modifierOptionIds": ["%s"],
                                      "sendLater": false
                                    }
                                  ],
                                  "comboSelections": [
                                    {
                                      "comboId": "%s",
                                      "quantity": 1,
                                      "allergyNotes": "fish",
                                      "specialInstructions": "No ice",
                                      "groups": [
                                        {
                                          "comboGroupId": "%s",
                                          "selectedOptionIds": ["%s"]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(
                                TestFixtures.SESSION_ID,
                                TestFixtures.MENU_ITEM_ID,
                                TestFixtures.MODIFIER_OPTION_ID,
                                TestFixtures.COMBO_ID,
                                TestFixtures.COMBO_GROUP_ID,
                                TestFixtures.COMBO_OPTION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderedItems[0].status").value("SENT"))
                .andExpect(jsonPath("$.data.combos[0].id").value(TestFixtures.COMBO_ID.toString()));

        ArgumentCaptor<OrderCommands.CreateOrderRequest> captor = ArgumentCaptor.forClass(OrderCommands.CreateOrderRequest.class);
        verify(ordersService).createOrder(captor.capture(), any(), anyString(), any());
        OrderCommands.CreateOrderRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, request.comboSelections().size());
        org.junit.jupiter.api.Assertions.assertEquals(TestFixtures.COMBO_ID, request.comboSelections().get(0).comboId());
        org.junit.jupiter.api.Assertions.assertEquals(TestFixtures.COMBO_OPTION_ID, request.comboSelections().get(0).groups().get(0).selectedOptionIds().get(0));
    }

    @Test
    void orderConfirmationCancellationAndItemActionsRemainStable() throws Exception {
        when(ordersService.confirmDraftOrder(any(), any(), anyString(), any())).thenReturn(TestFixtures.ordersOverview());
        when(ordersService.markServed(any(), any(), anyString())).thenReturn(TestFixtures.orderedItemView());
        when(ordersService.markDelayed(any())).thenReturn(TestFixtures.orderedItemView());
        when(ordersService.sendDelayedItem(any(), any(), anyString(), any())).thenReturn(TestFixtures.orderedItemView());
        when(ordersService.cancelOrderItem(any(), anyString(), any(), anyString(), any())).thenReturn(TestFixtures.orderedItemView());

        mockMvc.perform(post("/api/orders/{orderId}/confirm", TestFixtures.ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedSessionId").value(TestFixtures.SESSION_ID.toString()));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", TestFixtures.ORDER_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Guest left"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("order"))
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        mockMvc.perform(patch("/api/orders/items/{orderItemId}/served", TestFixtures.ORDER_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.ORDER_ITEM_ID.toString()));

        mockMvc.perform(patch("/api/orders/items/{orderItemId}/delayed", TestFixtures.ORDER_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(TestFixtures.ORDER_ID.toString()));

        mockMvc.perform(post("/api/orders/items/{orderItemId}/send", TestFixtures.ORDER_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.station").value("grill"));

        mockMvc.perform(post("/api/orders/items/{orderItemId}/cancel", TestFixtures.ORDER_ITEM_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Out of stock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Grilled Salmon"));
    }

    @Test
    void menuOverviewAndComboCrudExposeBackendComboStructures() throws Exception {
        when(menuService.load()).thenReturn(TestFixtures.menuOverview());
        when(menuService.createCombo(any())).thenReturn(TestFixtures.comboView());
        when(menuService.updateCombo(any(), any())).thenReturn(TestFixtures.comboView());

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].name").value("Mains"))
                .andExpect(jsonPath("$.data.combos[0].groups[0].required").value(true));

        String comboPayload = """
                {
                  "name": "Lunch Set",
                  "description": "Main + drink",
                  "price": 19.50,
                  "active": true,
                  "groups": [
                    {
                      "name": "Choose a drink",
                      "minSelections": 1,
                      "maxSelections": 1,
                      "required": true,
                      "options": [
                        {
                          "menuItemId": "%s",
                          "extraPrice": 1.50,
                          "active": true
                        }
                      ]
                    }
                  ]
                }
                """.formatted(TestFixtures.MENU_ITEM_ID);

        mockMvc.perform(post("/api/menu/combos")
                        .contentType(APPLICATION_JSON)
                        .content(comboPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.COMBO_ID.toString()));

        mockMvc.perform(put("/api/menu/combos/{comboId}", TestFixtures.COMBO_ID)
                        .contentType(APPLICATION_JSON)
                        .content(comboPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].options[0].menuItemId").value(TestFixtures.MENU_ITEM_ID.toString()));
    }

    @Test
    void categoryItemAndPromotionCrudStayAlignedToControllerContract() throws Exception {
        when(menuService.createCategory(anyString())).thenReturn(TestFixtures.categoryView());
        when(menuService.updateCategory(any(), anyString())).thenReturn(TestFixtures.categoryView());
        when(menuService.createItem(any(), any(), anyString(), any())).thenReturn(TestFixtures.menuCatalogItemView());
        when(menuService.updateItem(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.menuCatalogItemView());
        when(menuService.toggleAvailability(any(), any(), anyString(), any())).thenReturn(TestFixtures.menuCatalogItemView());
        when(menuService.createPromotion(any())).thenReturn(TestFixtures.promotionView());
        when(menuService.updatePromotion(any(), any())).thenReturn(TestFixtures.promotionView());

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mains"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.CATEGORY_ID.toString()));

        mockMvc.perform(put("/api/menu/categories/{categoryId}", TestFixtures.CATEGORY_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mains"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/menu/categories/{categoryId}", TestFixtures.CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("menuCategory"));

        String menuItemPayload = """
                {
                  "name": "Grilled Salmon",
                  "description": "Fresh salmon with herbs",
                  "category": "Mains",
                  "price": 15.50,
                  "station": "grill",
                  "allergens": ["fish"],
                  "ingredients": ["salmon", "lemon"],
                  "preparationTimeMin": 15
                }
                """;

        mockMvc.perform(post("/api/menu/items")
                        .contentType(APPLICATION_JSON)
                        .content(menuItemPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Grilled Salmon"));

        mockMvc.perform(put("/api/menu/items/{menuItemId}", TestFixtures.MENU_ITEM_ID)
                        .contentType(APPLICATION_JSON)
                        .content(menuItemPayload))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/menu/items/{menuItemId}/availability", TestFixtures.MENU_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));

        mockMvc.perform(delete("/api/menu/items/{menuItemId}", TestFixtures.MENU_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("menuItem"));

        mockMvc.perform(post("/api/menu/promotions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "LUNCH10",
                                  "discount": "10%",
                                  "validUntil": "2026-04-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("LUNCH10"));

        mockMvc.perform(put("/api/menu/promotions/{promotionId}", TestFixtures.PROMOTION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "LUNCH10",
                                  "discount": "10%",
                                  "validUntil": "2026-04-30"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/menu/promotions/{promotionId}", TestFixtures.PROMOTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityType").value("promotion"));
    }

    @Test
    void orderAndMenuValidationErrorsRemainExplicit() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "specialInstructions": "Rush"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sessionId"));

        mockMvc.perform(post("/api/menu/items")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Fresh salmon with herbs",
                                  "category": "Mains",
                                  "price": 15.50,
                                  "station": "grill"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }
}
