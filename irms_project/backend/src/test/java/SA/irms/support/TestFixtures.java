package SA.irms.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import SA.irms.billing.application.view.BillingViews;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.identity.application.view.IdentityViews;
import SA.irms.inventory.application.view.InventoryViews;
import SA.irms.kitchen.application.view.KitchenViews;
import SA.irms.notification.application.view.NotificationViews;
import SA.irms.ordering.application.view.MenuViews;
import SA.irms.ordering.application.view.OrderViews;
import SA.irms.reporting.application.view.DashboardViews;
import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.ReportMetricView;
import SA.irms.reporting.application.view.ReportValueType;
import SA.irms.reporting.application.view.ReportingProjectionRows;
import SA.irms.reporting.application.view.TypedReportViews;
import SA.irms.reservation.application.view.ReservationViews;

public final class TestFixtures {
    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID ORDER_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID MENU_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID MODIFIER_GROUP_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID MODIFIER_OPTION_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    public static final UUID COMBO_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    public static final UUID COMBO_GROUP_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    public static final UUID COMBO_OPTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID CATEGORY_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID PROMOTION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID RESERVATION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    public static final UUID WAITLIST_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final UUID TABLE_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    public static final UUID TICKET_ID = UUID.fromString("12121212-1212-1212-1212-121212121212");
    public static final UUID TICKET_ITEM_ID = UUID.fromString("13131313-1313-1313-1313-131313131313");
    public static final UUID BILL_ID = UUID.fromString("14141414-1414-1414-1414-141414141414");
    public static final UUID PAYMENT_ID = UUID.fromString("15151515-1515-1515-1515-151515151515");
    public static final UUID SPLIT_ID = UUID.fromString("16161616-1616-1616-1616-161616161616");
    public static final UUID REFUND_ID = UUID.fromString("17171717-1717-1717-1717-171717171717");
    public static final UUID INVENTORY_ITEM_ID = UUID.fromString("18181818-1818-1818-1818-181818181818");
    public static final UUID ALERT_ID = UUID.fromString("19191919-1919-1919-1919-191919191919");
    public static final UUID AUDIT_ID = UUID.fromString("20202020-2020-2020-2020-202020202020");
    public static final UUID MESSAGE_ID = UUID.fromString("21212121-2121-2121-2121-212121212121");
    public static final UUID SHIFT_ID = UUID.fromString("23232323-2323-2323-2323-232323232323");

    public static final Instant NOW = Instant.parse("2026-04-21T10:15:30Z");
    public static final LocalDate BUSINESS_DATE = LocalDate.parse("2026-04-21");

    private TestFixtures() {
    }

    public static AuthenticatedUser authenticatedUser(String... permissions) {
        return new AuthenticatedUser(
                USER_ID,
                "manager@irms.local",
                "Alex Manager",
                java.util.Set.of("manager"),
                permissions.length == 0 ? java.util.Set.of("all") : java.util.Set.of(permissions),
                SESSION_ID
        );
    }

    public static IdentityViews.UserView userView() {
        return new IdentityViews.UserView(
                USER_ID,
                "manager@irms.local",
                "Alex Manager",
                List.of("manager"),
                List.of("all"),
                SESSION_ID
        );
    }

    public static IdentityViews.LoginResult loginResult() {
        return new IdentityViews.LoginResult("jwt-token", NOW.plusSeconds(900), userView());
    }

    public static IdentityViews.StaffView staffView() {
        return new IdentityViews.StaffView(List.of(), List.of(), List.of());
    }

    public static IdentityViews.StaffRowView staffRowView() {
        return new IdentityViews.StaffRowView(
                USER_ID,
                "Alex Manager",
                "manager@irms.local",
                "ACTIVE",
                BUSINESS_DATE.minusYears(2),
                java.util.Set.of("manager"),
                java.util.Set.of("staff.manage")
        );
    }

    public static IdentityViews.ShiftView shiftView() {
        return new IdentityViews.ShiftView(
                SHIFT_ID,
                USER_ID,
                "Alex Manager",
                "Manager",
                BUSINESS_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Floor",
                "A",
                "SCHEDULED"
        );
    }

    public static Map<String, Object> settings() {
        return Map.of(
                "waitlistHoldMinutes", 15,
                "reservationGraceMinutes", 10,
                "kitchenRushThresholdMinutes", 20,
                "kitchenLateThresholdMinutes", 30,
                "refundWindowHours", 24
        );
    }

    public static Map<String, Object> auditSearchResult() {
        return Map.of("items", List.of(Map.of("auditLogId", AUDIT_ID.toString(), "action", "audit.log.viewed")));
    }

    public static ReservationViews.TableView tableView() {
        return new ReservationViews.TableView(TABLE_ID, 12, 4, "AVAILABLE", "Window", SESSION_ID, "2026-04-21T18:00:00", "2026-04-21T18:30:00");
    }

    public static ReservationViews.ReservationView reservationView() {
        return new ReservationViews.ReservationView(RESERVATION_ID, "Jamie Guest", "0123456789", "guest@irms.local", 4, "2026-04-21", "18:30", "CONFIRMED", 12, "Birthday");
    }

    public static ReservationViews.WaitlistView waitlistView() {
        return new ReservationViews.WaitlistView(WAITLIST_ID, "Taylor Walk-in", "0987654321", "walkin@irms.local", 2, "15 min", "WAITING", "2026-04-21T18:10:00", "Near bar", 1, 12);
    }

    public static ReservationViews.ReservationOverview reservationOverview() {
        return new ReservationViews.ReservationOverview(List.of(tableView()), List.of(reservationView()), List.of(waitlistView()));
    }

    public static ReservationViews.ReservationRecommendation recommendation() {
        return new ReservationViews.ReservationRecommendation("2026-04-21", "19:00", 4, List.of(new ReservationViews.RecommendedTable(TABLE_ID, 12, 4)), false, 0);
    }

    public static ReservationViews.TableActionResult tableActionResult() {
        return new ReservationViews.TableActionResult(TABLE_ID, "SEATED", "Table assigned.", null);
    }

    public static MenuViews.ComboView comboView() {
        return new MenuViews.ComboView(
                COMBO_ID,
                "Lunch Set",
                "Main + drink",
                bd("19.50"),
                true,
                List.of(new MenuViews.ComboGroupView(
                        COMBO_GROUP_ID,
                        "Choose a drink",
                        1,
                        1,
                        true,
                        List.of(new MenuViews.ComboOptionView(COMBO_OPTION_ID, MENU_ITEM_ID, "Iced Tea", "bar", bd("1.50"), true))
                ))
        );
    }

    public static MenuViews.MenuItemView menuCatalogItemView() {
        return new MenuViews.MenuItemView(
                MENU_ITEM_ID,
                "Grilled Salmon",
                "Fresh salmon with herbs",
                "Mains",
                bd("15.50"),
                "grill",
                true,
                List.of("fish"),
                1,
                3,
                List.of("salmon", "lemon")
        );
    }

    public static MenuViews.CategoryView categoryView() {
        return new MenuViews.CategoryView(CATEGORY_ID, "Mains", 8, true);
    }

    public static MenuViews.PromotionView promotionView() {
        return new MenuViews.PromotionView(PROMOTION_ID, "LUNCH10", "10%", "2026-04-30", true);
    }

    public static MenuViews.MenuOverview menuOverview() {
        return new MenuViews.MenuOverview(List.of(categoryView()), List.of(menuCatalogItemView()), List.of(promotionView()), List.of(comboView()));
    }

    public static OrderViews.MenuItemView orderMenuItemView() {
        return new OrderViews.MenuItemView(
                MENU_ITEM_ID,
                "Grilled Salmon",
                bd("15.50"),
                "Mains",
                "grill",
                true,
                List.of("fish"),
                List.of(new OrderViews.ModifierGroupView(
                        MODIFIER_GROUP_ID,
                        "Sauce",
                        0,
                        1,
                        false,
                        false,
                        List.of(new OrderViews.ModifierOptionView(MODIFIER_OPTION_ID, "Lemon Butter", bd("1.00"), true))
                ))
        );
    }

    public static OrderViews.OrderedItemView orderedItemView() {
        return new OrderViews.OrderedItemView(ORDER_ITEM_ID, ORDER_ID, "OPEN", "Grilled Salmon", 2, bd("31.00"), "No peanuts", "SENT", "grill");
    }

    public static OrderViews.OrdersOverview ordersOverview() {
        return new OrderViews.OrdersOverview(
                List.of(new OrderViews.TableSessionView(SESSION_ID, 12, 4)),
                List.of(orderMenuItemView()),
                List.of(comboView()),
                List.of(orderedItemView()),
                SESSION_ID
        );
    }

    public static KitchenViews.TicketView ticketView() {
        return new KitchenViews.TicketView(
                TICKET_ID,
                ORDER_ID,
                12,
                "Alex Manager",
                "HIGH",
                "IN_PROGRESS",
                NOW,
                List.of(new KitchenViews.TicketItemView(TICKET_ITEM_ID, ORDER_ITEM_ID, "Grilled Salmon", 2, List.of("Lemon Butter"), "fish", "No peanuts", "READY", "grill", null))
        );
    }

    public static KitchenViews.KitchenOverview kitchenOverview() {
        return new KitchenViews.KitchenOverview(List.of("grill", "bar"), List.of(ticketView()));
    }

    public static BillingViews.LineView billLine() {
        return new BillingViews.LineView(UUID.fromString("24242424-2424-2424-2424-242424242424"), "Grilled Salmon", 2, bd("31.00"), "Lemon Butter");
    }

    public static BillingViews.PaymentView paymentView() {
        return new BillingViews.PaymentView(PAYMENT_ID, BILL_ID, SPLIT_ID, "Seat 1", "CARD", bd("18.00"), "PAID", "2026-04-21T19:05:00");
    }

    public static BillingViews.RefundView refundView() {
        return new BillingViews.RefundView(REFUND_ID, PAYMENT_ID, BILL_ID, bd("5.00"), "Overcharge", "PENDING", "Alex Manager", null, "CARD", 12, "2026-04-21T19:06:00");
    }

    public static BillingViews.BillView billView() {
        return new BillingViews.BillView(
                BILL_ID,
                SESSION_ID,
                12,
                List.of(billLine()),
                bd("31.00"),
                bd("3.10"),
                bd("0.10"),
                bd("2.00"),
                bd("1.00"),
                bd("3.00"),
                bd("38.10"),
                "OPEN",
                List.of(new BillingViews.SplitView(SPLIT_ID, "Seat 1", bd("18.00"), bd("2.00"), "OPEN")),
                List.of(paymentView())
        );
    }

    public static BillingViews.BillingOverview billingOverview() {
        return new BillingViews.BillingOverview(
                billView(),
                List.of(new BillingViews.BillableSessionView(SESSION_ID, 12, 4, "2026-04-21T18:00:00")),
                List.of(new BillingViews.RecentBillView(BILL_ID, 12, bd("38.10"), "PAID", "CARD", "2026-04-21T19:05:00")),
                List.of(refundView())
        );
    }

    public static BillingViews.ReceiptView receiptView() {
        return new BillingViews.ReceiptView(UUID.fromString("25252525-2525-2525-2525-252525252525"), BILL_ID, "EMAIL", NOW, "guest@irms.local");
    }

    public static InventoryViews.IngredientView ingredientView() {
        return new InventoryViews.IngredientView(
                INVENTORY_ITEM_ID,
                "Salmon",
                "kg",
                bd("8.5"),
                bd("2.0"),
                bd("15.0"),
                bd("18.0"),
                "Seafood",
                "2026-04-20T10:00:00",
                List.of("Grilled Salmon")
        );
    }

    public static InventoryViews.InventoryOverview inventoryOverview() {
        return new InventoryViews.InventoryOverview(
                List.of(ingredientView()),
                List.of(new InventoryViews.TransactionView(UUID.fromString("26262626-2626-2626-2626-262626262626"), "Salmon", "RESTOCK", bd("5.0"), "Alex Manager", "2026-04-20T10:00:00")),
                List.of(new InventoryViews.ReorderRecommendationView(INVENTORY_ITEM_ID, "Salmon", bd("2.0"), bd("10.0"), bd("8.5"), bd("1.2"), bd("3.4"), bd("4.0"), "HIGH")),
                List.of(new InventoryViews.AlertView(ALERT_ID, INVENTORY_ITEM_ID, "Salmon", "MEDIUM", "OPEN", "2026-04-21T08:00:00", null, null, List.of("Grilled Salmon")))
        );
    }

    public static OperationsReportView operationsReport() {
        return new OperationsReportView(
                NOW,
                NOW.minusSeconds(3600),
                NOW,
                12,
                15,
                8,
                3,
                5,
                9,
                38,
                2,
                bd("4280.00"),
                42,
                14,
                bd("49.50"),
                List.of(
                        new OperationsReportView.RevenueTrendPoint("10AM", bd("450.00")),
                        new OperationsReportView.RevenueTrendPoint("11AM", bd("820.00"))
                ),
                List.of(
                        new OperationsReportView.WeeklyRevenuePoint("Mon", bd("3200.00")),
                        new OperationsReportView.WeeklyRevenuePoint("Tue", bd("4100.00"))
                ),
                List.of(
                        new OperationsReportView.TopDishPoint("Grilled Ribeye", 142, bd("5396.00"))
                ),
                List.of(
                        new OperationsReportView.PeakHourPoint("7PM", 35)
                ),
                List.of(
                        new OperationsReportView.CategoryRevenuePoint("Grill", bd("8200.00"))
                ),
                List.of(
                        new OperationsReportView.KitchenMetricPoint("Grill", "14 min", 42, 3)
                ),
                "outbox-1"
        );
    }

    public static TypedReportViews.SalesReportView salesReport() {
        return new TypedReportViews.SalesReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.SalesReportRow(BUSINESS_DATE.minusDays(1), 28, 24, bd("4100.00"), bd("50.00"), bd("4050.00")),
                        new ReportingProjectionRows.SalesReportRow(BUSINESS_DATE, 34, 30, bd("4280.00"), bd("49.50"), bd("4230.50"))
                ),
                List.of(new ReportMetricView("netSalesTotal", "Net Sales Total", ReportValueType.MONEY, "8280.5"))
        );
    }

    public static TypedReportViews.PeakHourReportView peakHourReport() {
        return new TypedReportViews.PeakHourReportView(
                NOW,
                NOW.minusSeconds(3600),
                NOW,
                List.of(new ReportingProjectionRows.PeakHourReportRow(BUSINESS_DATE, 19, 24, bd("540.00"))),
                List.of(new ReportMetricView("peakRevenue", "Peak Revenue", ReportValueType.MONEY, "540.00"))
        );
    }

    public static TypedReportViews.ComboSalesReportView comboSalesReport() {
        return new TypedReportViews.ComboSalesReportView(
                NOW,
                NOW.minusSeconds(3600),
                NOW,
                List.of(new ReportingProjectionRows.ComboSalesReportRow(BUSINESS_DATE, COMBO_ID.toString(), "Lunch Set", 12, bd("234.00"))),
                List.of(new ReportMetricView("comboRevenue", "Combo Revenue", ReportValueType.MONEY, "234.00"))
        );
    }

    public static TypedReportViews.BestSellingItemReportView bestSellingItemReport() {
        return new TypedReportViews.BestSellingItemReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.BestSellingItemReportRow(BUSINESS_DATE, MENU_ITEM_ID.toString(), "Grilled Ribeye", 142, bd("5396.00"))
                ),
                List.of(new ReportMetricView("bestSellingItemRows", "Best Selling Item Rows", ReportValueType.INTEGER, "1"))
        );
    }

    public static TypedReportViews.RevenueReportView revenueReport() {
        return new TypedReportViews.RevenueReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.RevenueReportRow(BUSINESS_DATE, "CARD", bd("3120.00"), bd("0.00"), bd("24.50"), bd("3095.50")),
                        new ReportingProjectionRows.RevenueReportRow(BUSINESS_DATE, "CASH", bd("1160.00"), bd("0.00"), bd("25.00"), bd("1135.00"))
                ),
                List.of(new ReportMetricView("netRevenueTotal", "Net Revenue Total", ReportValueType.MONEY, "4230.5"))
        );
    }

    public static TypedReportViews.KitchenBottleneckReportView kitchenBottleneckReport() {
        return new TypedReportViews.KitchenBottleneckReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.KitchenBottleneckReportRow(BUSINESS_DATE, "grill", 3, bd("14.00"))
                ),
                List.of(new ReportMetricView("kitchenBottleneckRows", "Kitchen Bottleneck Rows", ReportValueType.INTEGER, "1"))
        );
    }

    public static TypedReportViews.StaffEfficiencyReportView staffEfficiencyReport() {
        return new TypedReportViews.StaffEfficiencyReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.StaffEfficiencyReportRow(BUSINESS_DATE, USER_ID.toString(), "server", 18, bd("32.00"))
                ),
                List.of(new ReportMetricView("staffEfficiencyRows", "Staff Efficiency Rows", ReportValueType.INTEGER, "1"))
        );
    }

    public static TypedReportViews.InventoryUsageReportView inventoryUsageReport() {
        return new TypedReportViews.InventoryUsageReportView(
                NOW,
                NOW.minusSeconds(7 * 86400L),
                NOW,
                List.of(
                        new ReportingProjectionRows.InventoryUsageReportRow(BUSINESS_DATE, INVENTORY_ITEM_ID.toString(), "Salmon", bd("12.50"), bd("0.50"))
                ),
                List.of(new ReportMetricView("inventoryUsageRows", "Inventory Usage Rows", ReportValueType.INTEGER, "1"))
        );
    }

    public static DashboardViews.DashboardView dashboardView() {
        return new DashboardViews.DashboardView(
                operationsReport(),
                List.of(new DashboardViews.DashboardMetric("liveCovers", "Live Covers", "42", ReportValueType.INTEGER)),
                List.of(new DashboardViews.DashboardKpi("avgTicket", "Average Ticket", "38.10", "USD", DashboardViews.TrendDirection.UP)),
                List.of(new DashboardViews.AlertView(ALERT_ID, "LOW_STOCK", "Salmon is running low.", "2 min ago")),
                List.of(new DashboardViews.ActiveOrderView(ORDER_ID, 12, "Alex Manager", 3, "PREPARING", "4 min"))
        );
    }

    public static NotificationViews.NotificationView notificationView() {
        return new NotificationViews.NotificationView(MESSAGE_ID, "SYSTEM", "Kitchen delay", "Ticket 12 is delayed.", "HIGH", "UNREAD", NOW, null, Map.of("ticketId", TICKET_ID.toString()));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
