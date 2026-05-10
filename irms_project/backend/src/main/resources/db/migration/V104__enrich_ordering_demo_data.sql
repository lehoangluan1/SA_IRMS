-- Enrich order, kitchen, billing, and reporting demos with denser live scenarios.

insert into dining_tables (table_id, branch_id, code, capacity, zone, floor_label, section_label, position_row, position_col, status) values
('24000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000001', 'VIP-16', 10, 'Private Room', 'Upper', 'VIP', 6, 1, 'occupied'),
('24000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', 'PATIO-17', 4, 'Patio', 'Ground', 'F', 2, 4, 'occupied');

insert into customer_profiles (customer_profile_id, full_name, phone, email) values
('25000000-0000-0000-0000-000000000008', 'Ethan Brooks', '+1-555-0108', 'ethan.brooks@example.com'),
('25000000-0000-0000-0000-000000000009', 'Isabella Moore', '+1-555-0109', 'isabella.moore@example.com');

insert into table_sessions (session_id, table_id, reservation_id, server_user_id, guest_count, status, correlation_id, opened_at, closed_at) values
('25500000-0000-0000-0000-000000000009', '24000000-0000-0000-0000-000000000016', null, '22000000-0000-0000-0000-000000000003', 7, 'active', 'corr-session-009', now() - interval '28 minutes', null),
('25500000-0000-0000-0000-000000000010', '24000000-0000-0000-0000-000000000017', null, '22000000-0000-0000-0000-000000000008', 4, 'billing', 'corr-session-010', now() - interval '36 minutes', null);

insert into orders (order_id, table_session_id, channel, status, server_user_id, special_instructions, correlation_id, created_at, confirmed_at) values
('28000000-0000-0000-0000-000000000009', '25500000-0000-0000-0000-000000000009', 'dine_in', 'confirmed', '22000000-0000-0000-0000-000000000003', 'VIP pacing, appetizer first.', 'corr-order-009', now() - interval '26 minutes', now() - interval '25 minutes'),
('28000000-0000-0000-0000-000000000010', '25500000-0000-0000-0000-000000000010', 'dine_in', 'ready', '22000000-0000-0000-0000-000000000008', 'Need bill after second mocktail.', 'corr-order-010', now() - interval '33 minutes', now() - interval '31 minutes'),
('28000000-0000-0000-0000-000000000011', '25500000-0000-0000-0000-000000000005', 'dine_in', 'in_progress', '22000000-0000-0000-0000-000000000004', 'One guest sharing starters.', 'corr-order-011', now() - interval '9 minutes', now() - interval '8 minutes');

insert into order_snapshots (snapshot_id, order_id, menu_version, priced_at, currency, subtotal) values
('28100000-0000-0000-0000-000000000009', '28000000-0000-0000-0000-000000000009', 'menu-v3', now() - interval '25 minutes', 'USD', 53.50),
('28100000-0000-0000-0000-000000000010', '28000000-0000-0000-0000-000000000010', 'menu-v3', now() - interval '31 minutes', 'USD', 39.50),
('28100000-0000-0000-0000-000000000011', '28000000-0000-0000-0000-000000000011', 'menu-v3', now() - interval '8 minutes', 'USD', 47.00);

insert into order_items (order_item_id, order_id, menu_item_id, snapshot_name, quantity, unit_price, special_instruction, allergy_notes, line_status, fire_at, sent_to_kitchen_at, served_at, cancelled_at, cancellation_reason) values
('28200000-0000-0000-0000-000000000021', '28000000-0000-0000-0000-000000000009', '26100000-0000-0000-0000-000000000008', 'Mushroom Veloute', 1, 9.00, 'Warm bowls only.', null, 'blocked', now() - interval '24 minutes', now() - interval '23 minutes', null, null, null),
('28200000-0000-0000-0000-000000000022', '28000000-0000-0000-0000-000000000009', '26100000-0000-0000-0000-000000000002', 'Grilled Ribeye', 1, 38.00, 'Medium rare.', null, 'cooking', now() - interval '22 minutes', now() - interval '21 minutes', null, null, null),
('28200000-0000-0000-0000-000000000023', '28000000-0000-0000-0000-000000000009', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 1, 6.50, null, null, 'served', now() - interval '24 minutes', now() - interval '23 minutes', now() - interval '11 minutes', null, null),
('28200000-0000-0000-0000-000000000024', '28000000-0000-0000-0000-000000000010', '26100000-0000-0000-0000-000000000003', 'Fish and Chips', 1, 18.00, null, null, 'ready', now() - interval '30 minutes', now() - interval '29 minutes', null, null, null),
('28200000-0000-0000-0000-000000000025', '28000000-0000-0000-0000-000000000010', '26100000-0000-0000-0000-000000000005', 'Chocolate Tart', 1, 9.50, 'Hold until photos are done.', null, 'hold_for_service', null, null, null, null, null),
('28200000-0000-0000-0000-000000000026', '28000000-0000-0000-0000-000000000010', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 2, 6.50, null, null, 'served', now() - interval '30 minutes', now() - interval '29 minutes', now() - interval '12 minutes', null, null),
('28200000-0000-0000-0000-000000000027', '28000000-0000-0000-0000-000000000011', '26100000-0000-0000-0000-000000000007', 'Caesar Salad', 2, 12.50, 'Split onto two plates.', null, 'sent_to_kitchen', now() - interval '8 minutes', now() - interval '7 minutes', null, null, null),
('28200000-0000-0000-0000-000000000028', '28000000-0000-0000-0000-000000000011', '26100000-0000-0000-0000-000000000006', 'Wagyu Burger', 1, 26.00, 'No pickles.', null, 'cooking', now() - interval '7 minutes', now() - interval '6 minutes', null, null, null);

insert into order_item_modifiers (selection_id, order_item_id, modifier_option_id, name_snapshot, extra_price, qty_multiplier) values
('28300000-0000-0000-0000-000000000006', '28200000-0000-0000-0000-000000000022', '26300000-0000-0000-0000-000000000001', 'Medium Rare', 0.00, 1),
('28300000-0000-0000-0000-000000000007', '28200000-0000-0000-0000-000000000028', '26300000-0000-0000-0000-000000000004', 'Crispy Bacon', 3.00, 1),
('28300000-0000-0000-0000-000000000008', '28200000-0000-0000-0000-000000000027', '26300000-0000-0000-0000-000000000006', 'Grilled Shrimp', 6.00, 1);

insert into order_combo_selections (combo_selection_id, order_id, combo_id, combo_name, quantity, combo_price, allergy_notes, special_instructions, created_at) values
('31030000-0000-0000-0000-000000000003', '28000000-0000-0000-0000-000000000010', '31000000-0000-0000-0000-000000000002', 'Sweet Finish Pair', 1, 13.50, null, 'Keep dessert on hold until mains clear.', now() - interval '31 minutes');

insert into order_combo_selection_items (combo_selection_item_id, combo_selection_id, combo_group_id, combo_option_id, menu_item_id, menu_item_name, station, quantity, allocated_unit_price, created_at) values
('31040000-0000-0000-0000-000000000005', '31030000-0000-0000-0000-000000000003', '31010000-0000-0000-0000-000000000003', '31020000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000005', 'Chocolate Tart', 'dessert', 1, 9.50, now() - interval '31 minutes'),
('31040000-0000-0000-0000-000000000006', '31030000-0000-0000-0000-000000000003', '31010000-0000-0000-0000-000000000004', '31020000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 'drinks', 1, 4.00, now() - interval '31 minutes');

insert into route_plans (route_plan_id, created_from_order_id, notes) values
('28500000-0000-0000-0000-000000000003', '28000000-0000-0000-0000-000000000011', 'Salads first, burger after shrimp plate leaves pass.');

insert into kitchen_tickets (ticket_id, order_id, station_id, route_plan_id, priority, priority_label, status, blocked_reason, created_at, started_at, ready_at, target_service_at, expedited_at, next_action_at) values
('28600000-0000-0000-0000-000000000010', '28000000-0000-0000-0000-000000000009', '28400000-0000-0000-0000-000000000006', null, 9, 'rush', 'blocked', 'Soup garnish replacement pending', now() - interval '23 minutes', now() - interval '22 minutes', null, now() + interval '4 minutes', null, now() + interval '3 minutes'),
('28600000-0000-0000-0000-000000000011', '28000000-0000-0000-0000-000000000009', '28400000-0000-0000-0000-000000000001', null, 9, 'rush', 'cooking', null, now() - interval '21 minutes', now() - interval '20 minutes', null, now() + interval '6 minutes', null, now() + interval '5 minutes'),
('28600000-0000-0000-0000-000000000012', '28000000-0000-0000-0000-000000000010', '28400000-0000-0000-0000-000000000002', null, 4, 'normal', 'ready', null, now() - interval '29 minutes', now() - interval '28 minutes', now() - interval '9 minutes', now() + interval '2 minutes', null, now() + interval '1 minutes'),
('28600000-0000-0000-0000-000000000013', '28000000-0000-0000-0000-000000000011', '28400000-0000-0000-0000-000000000005', '28500000-0000-0000-0000-000000000003', 7, 'expedite', 'cooking', null, now() - interval '7 minutes', now() - interval '6 minutes', null, now() + interval '4 minutes', now() - interval '5 minutes', now() + interval '3 minutes');

insert into kitchen_ticket_items (ticket_item_id, ticket_id, order_item_id, quantity, status, fire_at, hold_reason, next_action_at) values
('28700000-0000-0000-0000-000000000011', '28600000-0000-0000-0000-000000000010', '28200000-0000-0000-0000-000000000021', 1, 'blocked', now() - interval '22 minutes', 'Awaiting updated garnish', now() + interval '3 minutes'),
('28700000-0000-0000-0000-000000000012', '28600000-0000-0000-0000-000000000011', '28200000-0000-0000-0000-000000000022', 1, 'cooking', now() - interval '20 minutes', null, now() + interval '5 minutes'),
('28700000-0000-0000-0000-000000000013', '28600000-0000-0000-0000-000000000012', '28200000-0000-0000-0000-000000000024', 1, 'ready', now() - interval '28 minutes', null, now() + interval '1 minutes'),
('28700000-0000-0000-0000-000000000014', '28600000-0000-0000-0000-000000000013', '28200000-0000-0000-0000-000000000027', 2, 'cooking', now() - interval '6 minutes', null, now() + interval '2 minutes'),
('28700000-0000-0000-0000-000000000015', '28600000-0000-0000-0000-000000000013', '28200000-0000-0000-0000-000000000028', 1, 'cooking', now() - interval '5 minutes', null, now() + interval '4 minutes');

insert into dish_status_events (event_id, ticket_item_id, handoff_id, type, source_station, details, occurred_at) values
('28900000-0000-0000-0000-000000000006', '28700000-0000-0000-0000-000000000011', null, 'blocked', 'Prep Station', '{"reason":"vip garnish change"}', now() - interval '21 minutes'),
('28900000-0000-0000-0000-000000000007', '28700000-0000-0000-0000-000000000012', null, 'cooking', 'Grill Station', '{"temperature":"medium_rare"}', now() - interval '19 minutes'),
('28900000-0000-0000-0000-000000000008', '28700000-0000-0000-0000-000000000013', null, 'ready', 'Fryer Station', '{"queue":"fast-lane"}', now() - interval '9 minutes'),
('28900000-0000-0000-0000-000000000009', '28700000-0000-0000-0000-000000000014', null, 'cooking', 'Salad Station', '{"expedite":true}', now() - interval '5 minutes');

insert into bills (bill_id, table_session_id, promotion_campaign_id, status, sub_total, tax_amount, service_fee, tip_amount, grand_total, discount_total, promotion_code, correlation_id, created_at, finalized_at) values
('29000000-0000-0000-0000-000000000005', '25500000-0000-0000-0000-000000000010', '26500000-0000-0000-0000-000000000002', 'open', 39.50, 3.36, 1.98, 6.00, 48.84, 5.93, 'HAPPY15', 'corr-bill-005', now() - interval '8 minutes', now() - interval '7 minutes');

insert into bill_lines (bill_line_id, bill_id, source_order_item_id, label, quantity, line_total) values
('29100000-0000-0000-0000-000000000011', '29000000-0000-0000-0000-000000000005', '28200000-0000-0000-0000-000000000024', 'Fish and Chips', 1, 18.00),
('29100000-0000-0000-0000-000000000012', '29000000-0000-0000-0000-000000000005', '28200000-0000-0000-0000-000000000025', 'Chocolate Tart', 1, 9.50),
('29100000-0000-0000-0000-000000000013', '29000000-0000-0000-0000-000000000005', '28200000-0000-0000-0000-000000000026', 'Citrus Mocktail', 2, 13.00);

insert into bill_splits (split_id, bill_id, label, status, allocated_total, tip_amount) values
('29200000-0000-0000-0000-000000000007', '29000000-0000-0000-0000-000000000005', 'Mocktail Guest', 'paid', 18.92, 2.50),
('29200000-0000-0000-0000-000000000008', '29000000-0000-0000-0000-000000000005', 'Main Table', 'pending', 29.92, 3.50);

insert into split_allocations (allocation_id, split_id, bill_line_id, amount, ratio) values
('29300000-0000-0000-0000-000000000016', '29200000-0000-0000-0000-000000000007', '29100000-0000-0000-0000-000000000013', 13.00, 1.0000),
('29300000-0000-0000-0000-000000000017', '29200000-0000-0000-0000-000000000008', '29100000-0000-0000-0000-000000000011', 18.00, 1.0000),
('29300000-0000-0000-0000-000000000018', '29200000-0000-0000-0000-000000000008', '29100000-0000-0000-0000-000000000012', 9.50, 1.0000);

insert into payments (payment_id, bill_id, split_id, method, amount, status, gateway_ref, cash_received, change_due, processed_by_user_id, paid_at) values
('29400000-0000-0000-0000-000000000006', '29000000-0000-0000-0000-000000000005', '29200000-0000-0000-0000-000000000007', 'credit_card', 18.92, 'completed', 'txn-cc-006', null, null, '22000000-0000-0000-0000-000000000006', now() - interval '6 minutes'),
('29400000-0000-0000-0000-000000000007', '29000000-0000-0000-0000-000000000005', null, 'cash', 10.00, 'pending', null, 10.00, 0.00, '22000000-0000-0000-0000-000000000006', now() - interval '2 minutes');

insert into notification_messages (message_id, low_stock_alert_id, reservation_id, payment_id, channel, type, template_code, payload, title, body, recipient_role, recipient_user_id, recipient_address, priority, status, created_at, queued_at, sent_at, read_at, failure_reason) values
('29900000-0000-0000-0000-000000000006', null, null, '29400000-0000-0000-0000-000000000007', 'in_app', 'payment_watch', 'PAYMENT_WATCH', '{"billId":"29000000-0000-0000-0000-000000000005","sessionId":"25500000-0000-0000-0000-000000000010"}', 'Cash settlement pending', 'A billing session still has one unsettled payment after partial card capture.', 'cashier', '22000000-0000-0000-0000-000000000006', 'david.ross@irms.io', 'high', 'queued', now() - interval '90 seconds', now() - interval '90 seconds', null, null, null);

insert into shift_assignments (shift_assignment_id, user_id, branch_id, shift_date, start_at, end_at, position, zone, status) values
('2B000000-0000-0000-0000-000000000008', '22000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', current_date, time '18:00', time '23:30', 'Server', 'VIP', 'scheduled');
