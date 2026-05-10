-- Expand demo data without mutating legacy migrations.
-- This migration deepens transactional coverage and reporting projections for dashboard/reports demos.

update dining_tables
set status = 'occupied', updated_at = now()
where table_id in (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000006'
);

insert into dining_tables (table_id, branch_id, code, capacity, zone, floor_label, section_label, position_row, position_col, status) values
('24000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'T11', 2, 'Patio', 'Ground', 'A', 1, 3, 'occupied'),
('24000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'T12', 4, 'Main Hall', 'Ground', 'B', 2, 3, 'occupied'),
('24000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'T13', 4, 'Main Hall', 'Ground', 'C', 3, 3, 'available'),
('24000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'T14', 6, 'Window', 'Ground', 'D', 4, 3, 'reserved'),
('24000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', 'T15', 8, 'Private Room', 'Ground', 'E', 5, 3, 'occupied');

insert into customer_profiles (customer_profile_id, full_name, phone, email) values
('25000000-0000-0000-0000-000000000004', 'Noah Davis', '+1-555-0104', 'noah.davis@example.com'),
('25000000-0000-0000-0000-000000000005', 'Sophia Turner', '+1-555-0105', 'sophia.turner@example.com'),
('25000000-0000-0000-0000-000000000006', 'Mason Clark', '+1-555-0106', 'mason.clark@example.com'),
('25000000-0000-0000-0000-000000000007', 'Emma Lewis', '+1-555-0107', 'emma.lewis@example.com');

insert into reservation_contacts (contact_id, name, phone, email, notification_channel) values
('25100000-0000-0000-0000-000000000004', 'Noah Davis', '+1-555-0104', 'noah.davis@example.com', 'email'),
('25100000-0000-0000-0000-000000000005', 'Sophia Turner', '+1-555-0105', 'sophia.turner@example.com', 'sms'),
('25100000-0000-0000-0000-000000000006', 'Mason Clark', '+1-555-0106', 'mason.clark@example.com', 'phone'),
('25100000-0000-0000-0000-000000000007', 'Emma Lewis', '+1-555-0107', 'emma.lewis@example.com', 'in_app');

insert into seating_preferences (seating_preference_id, window_side, indoor_only, high_chair_count, table_area_hint) values
('25200000-0000-0000-0000-000000000003', false, true, 0, 'Patio'),
('25200000-0000-0000-0000-000000000004', true, true, 0, 'Private Room'),
('25200000-0000-0000-0000-000000000005', false, false, 2, 'Main Hall');

insert into reservations (reservation_id, branch_id, customer_profile_id, contact_id, seating_preference_id, arrival_at, party_size, status, source, notes, confirmed_at, checked_in_at, cancelled_at, cancel_reason, hold_expires_at)
values
('25300000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000004', '25100000-0000-0000-0000-000000000004', '25200000-0000-0000-0000-000000000004', now() - interval '30 minutes', 6, 'seated', 'manual', 'VIP anniversary dinner', now() - interval '55 minutes', now() - interval '32 minutes', null, null, now() + interval '15 minutes'),
('25300000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000005', '25100000-0000-0000-0000-000000000005', '25200000-0000-0000-0000-000000000003', now() - interval '5 hours', 2, 'completed', 'online', 'Lunch meeting', now() - interval '6 hours', now() - interval '5 hours 10 minutes', null, null, now() - interval '4 hours 30 minutes'),
('25300000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000006', '25100000-0000-0000-0000-000000000006', null, now() - interval '2 hours', 4, 'no_show', 'phone', 'Delayed flight', now() - interval '3 hours', null, null, null, now() - interval '90 minutes'),
('25300000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000007', '25100000-0000-0000-0000-000000000007', '25200000-0000-0000-0000-000000000005', now() + interval '3 hours', 5, 'cancelled', 'online', 'Allergy accommodation not needed anymore', now() - interval '10 minutes', null, now() - interval '5 minutes', 'Customer cancelled from app', now() + interval '3 hours 15 minutes');

insert into waitlist_entries (waitlist_entry_id, branch_id, customer_profile_id, seating_preference_id, contact_name, contact_phone, contact_email, notification_channel, party_size, quoted_wait_min, status, notes, priority, added_at, hold_expires_at, notified_at, seated_at)
values
('25400000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000004', '25200000-0000-0000-0000-000000000003', 'Noah Davis', '+1-555-0104', 'noah.davis@example.com', 'sms', 2, 10, 'notified', 'Ready for patio seating', 2, now() - interval '18 minutes', now() + interval '4 minutes', now() - interval '2 minutes', null),
('25400000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000005', null, 'Sophia Turner', '+1-555-0105', 'sophia.turner@example.com', 'sms', 5, 35, 'skipped', 'Party stepped out briefly', 0, now() - interval '55 minutes', now() - interval '20 minutes', now() - interval '25 minutes', null),
('25400000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000006', '25200000-0000-0000-0000-000000000004', 'Mason Clark', '+1-555-0106', 'mason.clark@example.com', 'phone', 6, 25, 'seated', 'Needs projector setup', 3, now() - interval '40 minutes', now() - interval '12 minutes', now() - interval '16 minutes', now() - interval '12 minutes'),
('25400000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000007', null, 'Emma Lewis', '+1-555-0107', 'emma.lewis@example.com', 'email', 3, 15, 'expired', 'Window preferred', 1, now() - interval '50 minutes', now() - interval '15 minutes', now() - interval '19 minutes', null);

insert into table_sessions (session_id, table_id, reservation_id, server_user_id, guest_count, status, correlation_id, opened_at, closed_at)
values
('25500000-0000-0000-0000-000000000004', '24000000-0000-0000-0000-000000000001', null, '22000000-0000-0000-0000-000000000003', 2, 'active', 'corr-session-004', now() - interval '26 minutes', null),
('25500000-0000-0000-0000-000000000005', '24000000-0000-0000-0000-000000000006', null, '22000000-0000-0000-0000-000000000004', 4, 'active', 'corr-session-005', now() - interval '22 minutes', null),
('25500000-0000-0000-0000-000000000006', '24000000-0000-0000-0000-000000000011', '25300000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000003', 6, 'billing', 'corr-session-006', now() - interval '48 minutes', null),
('25500000-0000-0000-0000-000000000007', '24000000-0000-0000-0000-000000000012', null, '22000000-0000-0000-0000-000000000008', 4, 'active', 'corr-session-007', now() - interval '16 minutes', null),
('25500000-0000-0000-0000-000000000008', '24000000-0000-0000-0000-000000000015', '25300000-0000-0000-0000-000000000004', '22000000-0000-0000-0000-000000000004', 2, 'closed', 'corr-session-008', now() - interval '6 hours', now() - interval '4 hours 20 minutes');

insert into table_assignments (assignment_id, reservation_id, table_id, assigned_at, reason, score) values
('25600000-0000-0000-0000-000000000002', '25300000-0000-0000-0000-000000000003', '24000000-0000-0000-0000-000000000011', now() - interval '50 minutes', 'manager_override', 96.10),
('25600000-0000-0000-0000-000000000003', '25300000-0000-0000-0000-000000000004', '24000000-0000-0000-0000-000000000015', now() - interval '6 hours', 'walk_in', 88.00),
('25600000-0000-0000-0000-000000000004', '25300000-0000-0000-0000-000000000006', '24000000-0000-0000-0000-000000000014', now() - interval '10 minutes', 'reservation_match', 79.40);

insert into menu_categories (category_id, name, display_order, is_active) values
('26000000-0000-0000-0000-000000000005', 'Salads', 5, true),
('26000000-0000-0000-0000-000000000006', 'Chef Specials', 6, false);

insert into menu_items (menu_item_id, category_id, name, description, base_price, station, availability, sale_status, preparation_time_min, allergens_json) values
('26100000-0000-0000-0000-000000000007', '26000000-0000-0000-0000-000000000005', 'Caesar Salad', 'Crisp romaine, parmesan, anchovy dressing.', 12.50, 'salad', 'available', 'active', 7, '["fish","egg","milk"]'),
('26100000-0000-0000-0000-000000000008', '26000000-0000-0000-0000-000000000001', 'Mushroom Veloute', 'Silky mushroom soup with chives.', 9.00, 'prep', 'scheduled', 'seasonal', 10, '["milk"]'),
('26100000-0000-0000-0000-000000000009', '26000000-0000-0000-0000-000000000006', 'Chef Tasting Board', 'Rotating premium bites for tasting menus.', 24.00, 'prep', 'unavailable', 'hidden', 18, '["gluten","nuts","milk"]'),
('26100000-0000-0000-0000-000000000010', '26000000-0000-0000-0000-000000000003', 'Citrus Mocktail', 'Orange, yuzu, basil and soda.', 6.50, 'drinks', 'available', 'active', 4, '[]');

insert into modifier_groups (group_id, menu_item_id, name, min_select, max_select, required, multi_select, display_order) values
('26200000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000007', 'Add Protein', 0, 2, false, true, 1);

insert into modifier_options (option_id, group_id, name, extra_price, active, display_order) values
('26300000-0000-0000-0000-000000000005', '26200000-0000-0000-0000-000000000003', 'Chicken', 4.50, true, 1),
('26300000-0000-0000-0000-000000000006', '26200000-0000-0000-0000-000000000003', 'Grilled Shrimp', 6.00, true, 2);

insert into availability_rules (rule_id, menu_item_id, effective_from, effective_to, out_of_stock_hides_item, is_active) values
('26400000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000009', now() - interval '7 days', now() + interval '7 days', true, true);

insert into promotion_campaigns (promotion_campaign_id, code, name, discount_type, discount_value, effective_from, effective_to, is_active) values
('26500000-0000-0000-0000-000000000002', 'HAPPY15', 'Happy Hour Drinks', 'percentage', 15.00, now() - interval '2 days', now() + interval '14 days', true);

insert into menu_combos (combo_id, name, description, combo_price, active, created_at, updated_at) values
('31000000-0000-0000-0000-000000000001', 'Lunch Set', 'Choose a main and a drink.', 19.50, true, now() - interval '8 days', now() - interval '1 day'),
('31000000-0000-0000-0000-000000000002', 'Sweet Finish Pair', 'Dessert paired with a sparkling drink.', 13.50, true, now() - interval '8 days', now() - interval '1 day');

insert into menu_combo_groups (combo_group_id, combo_id, name, min_selections, max_selections, required, display_order, created_at) values
('31010000-0000-0000-0000-000000000001', '31000000-0000-0000-0000-000000000001', 'Choose a main', 1, 1, true, 1, now() - interval '8 days'),
('31010000-0000-0000-0000-000000000002', '31000000-0000-0000-0000-000000000001', 'Choose a drink', 1, 1, true, 2, now() - interval '8 days'),
('31010000-0000-0000-0000-000000000003', '31000000-0000-0000-0000-000000000002', 'Choose a dessert', 1, 1, true, 1, now() - interval '8 days'),
('31010000-0000-0000-0000-000000000004', '31000000-0000-0000-0000-000000000002', 'Choose a beverage', 1, 1, true, 2, now() - interval '8 days');

insert into menu_combo_options (combo_option_id, combo_group_id, menu_item_id, extra_price, active, created_at) values
('31020000-0000-0000-0000-000000000001', '31010000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000003', 0.00, true, now() - interval '8 days'),
('31020000-0000-0000-0000-000000000002', '31010000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000006', 1.50, true, now() - interval '8 days'),
('31020000-0000-0000-0000-000000000003', '31010000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000004', 0.00, true, now() - interval '8 days'),
('31020000-0000-0000-0000-000000000004', '31010000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000010', 0.50, true, now() - interval '8 days'),
('31020000-0000-0000-0000-000000000005', '31010000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000005', 0.00, true, now() - interval '8 days'),
('31020000-0000-0000-0000-000000000006', '31010000-0000-0000-0000-000000000004', '26100000-0000-0000-0000-000000000004', 0.00, true, now() - interval '8 days');

insert into kitchen_stations (station_id, name, kind, max_parallel_tickets) values
('28400000-0000-0000-0000-000000000005', 'Salad Station', 'salad', 4),
('28400000-0000-0000-0000-000000000006', 'Prep Station', 'prep', 4);

insert into orders (order_id, table_session_id, channel, status, server_user_id, special_instructions, correlation_id, created_at, confirmed_at) values
('28000000-0000-0000-0000-000000000004', '25500000-0000-0000-0000-000000000004', 'dine_in', 'in_progress', '22000000-0000-0000-0000-000000000003', 'Two courses paced slowly.', 'corr-order-004', now() - interval '24 minutes', now() - interval '23 minutes'),
('28000000-0000-0000-0000-000000000005', '25500000-0000-0000-0000-000000000005', 'dine_in', 'confirmed', '22000000-0000-0000-0000-000000000004', 'Guest one is nut-free.', 'corr-order-005', now() - interval '20 minutes', now() - interval '19 minutes'),
('28000000-0000-0000-0000-000000000006', '25500000-0000-0000-0000-000000000006', 'dine_in', 'ready', '22000000-0000-0000-0000-000000000003', 'Bill requested after dessert.', 'corr-order-006', now() - interval '44 minutes', now() - interval '42 minutes'),
('28000000-0000-0000-0000-000000000007', '25500000-0000-0000-0000-000000000007', 'dine_in', 'in_progress', '22000000-0000-0000-0000-000000000008', 'Rush one salad before mains.', 'corr-order-007', now() - interval '12 minutes', now() - interval '11 minutes'),
('28000000-0000-0000-0000-000000000008', '25500000-0000-0000-0000-000000000008', 'dine_in', 'cancelled', '22000000-0000-0000-0000-000000000004', 'Customer left early after meeting.', 'corr-order-008', now() - interval '5 hours 30 minutes', now() - interval '5 hours 20 minutes');

insert into order_snapshots (snapshot_id, order_id, menu_version, priced_at, currency, subtotal) values
('28100000-0000-0000-0000-000000000004', '28000000-0000-0000-0000-000000000004', 'menu-v2', now() - interval '23 minutes', 'USD', 31.50),
('28100000-0000-0000-0000-000000000005', '28000000-0000-0000-0000-000000000005', 'menu-v2', now() - interval '19 minutes', 'USD', 44.00),
('28100000-0000-0000-0000-000000000006', '28000000-0000-0000-0000-000000000006', 'menu-v2', now() - interval '42 minutes', 'USD', 52.50),
('28100000-0000-0000-0000-000000000007', '28000000-0000-0000-0000-000000000007', 'menu-v2', now() - interval '11 minutes', 'USD', 32.50),
('28100000-0000-0000-0000-000000000008', '28000000-0000-0000-0000-000000000008', 'menu-v2', now() - interval '5 hours 20 minutes', 'USD', 17.50);

insert into order_items (order_item_id, order_id, menu_item_id, snapshot_name, quantity, unit_price, special_instruction, allergy_notes, line_status, fire_at, sent_to_kitchen_at, served_at, cancelled_at, cancellation_reason) values
('28200000-0000-0000-0000-000000000008', '28000000-0000-0000-0000-000000000004', '26100000-0000-0000-0000-000000000007', 'Caesar Salad', 1, 12.50, 'Dressing on side.', null, 'ready', now() - interval '19 minutes', now() - interval '18 minutes', null, null, null),
('28200000-0000-0000-0000-000000000009', '28000000-0000-0000-0000-000000000004', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 1, 6.50, null, null, 'served', now() - interval '21 minutes', now() - interval '20 minutes', now() - interval '10 minutes', null, null),
('28200000-0000-0000-0000-000000000010', '28000000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000002', 'Grilled Ribeye', 1, 38.00, 'Medium.', null, 'cooking', now() - interval '18 minutes', now() - interval '17 minutes', null, null, null),
('28200000-0000-0000-0000-000000000011', '28000000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 1, 5.50, null, null, 'served', now() - interval '18 minutes', now() - interval '17 minutes', now() - interval '9 minutes', null, null),
('28200000-0000-0000-0000-000000000012', '28000000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000003', 'Fish and Chips', 1, 18.00, null, null, 'ready', now() - interval '39 minutes', now() - interval '38 minutes', null, null, null),
('28200000-0000-0000-0000-000000000013', '28000000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000005', 'Chocolate Tart', 2, 9.50, 'One to-go.', null, 'hold_for_service', null, null, null, null, null),
('28200000-0000-0000-0000-000000000014', '28000000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 1, 6.50, null, null, 'served', now() - interval '40 minutes', now() - interval '39 minutes', now() - interval '18 minutes', null, null),
('28200000-0000-0000-0000-000000000015', '28000000-0000-0000-0000-000000000007', '26100000-0000-0000-0000-000000000007', 'Caesar Salad', 1, 12.50, 'Extra parmesan.', null, 'sent_to_kitchen', now() - interval '10 minutes', now() - interval '9 minutes', null, null, null),
('28200000-0000-0000-0000-000000000016', '28000000-0000-0000-0000-000000000007', '26100000-0000-0000-0000-000000000006', 'Wagyu Burger', 1, 26.00, 'No onions.', null, 'cooking', now() - interval '9 minutes', now() - interval '8 minutes', null, null, null),
('28200000-0000-0000-0000-000000000017', '28000000-0000-0000-0000-000000000008', '26100000-0000-0000-0000-000000000001', 'Truffle Fries', 1, 8.00, null, null, 'cancelled', null, null, null, now() - interval '5 hours 10 minutes', 'Guest left before item fired'),
('28200000-0000-0000-0000-000000000018', '28000000-0000-0000-0000-000000000008', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 1, 5.50, null, null, 'served', now() - interval '5 hours 18 minutes', now() - interval '5 hours 17 minutes', now() - interval '5 hours 12 minutes', null, null),
('28200000-0000-0000-0000-000000000019', '28000000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000008', 'Mushroom Veloute', 1, 9.00, null, null, 'blocked', now() - interval '16 minutes', now() - interval '15 minutes', null, null, null),
('28200000-0000-0000-0000-000000000020', '28000000-0000-0000-0000-000000000007', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 1, 6.50, null, null, 'served', now() - interval '10 minutes', now() - interval '9 minutes', now() - interval '4 minutes', null, null);

insert into order_item_modifiers (selection_id, order_item_id, modifier_option_id, name_snapshot, extra_price, qty_multiplier) values
('28300000-0000-0000-0000-000000000003', '28200000-0000-0000-0000-000000000008', '26300000-0000-0000-0000-000000000006', 'Grilled Shrimp', 6.00, 1),
('28300000-0000-0000-0000-000000000004', '28200000-0000-0000-0000-000000000015', '26300000-0000-0000-0000-000000000005', 'Chicken', 4.50, 1),
('28300000-0000-0000-0000-000000000005', '28200000-0000-0000-0000-000000000010', '26300000-0000-0000-0000-000000000002', 'Medium', 0.00, 1);

insert into order_combo_selections (combo_selection_id, order_id, combo_id, combo_name, quantity, combo_price, allergy_notes, special_instructions, created_at) values
('31030000-0000-0000-0000-000000000001', '28000000-0000-0000-0000-000000000006', '31000000-0000-0000-0000-000000000001', 'Lunch Set', 1, 19.50, null, 'Serve the drink immediately.', now() - interval '43 minutes'),
('31030000-0000-0000-0000-000000000002', '28000000-0000-0000-0000-000000000007', '31000000-0000-0000-0000-000000000002', 'Sweet Finish Pair', 1, 13.50, null, 'Dessert after mains.', now() - interval '11 minutes');

insert into order_combo_selection_items (combo_selection_item_id, combo_selection_id, combo_group_id, combo_option_id, menu_item_id, menu_item_name, station, quantity, allocated_unit_price, created_at) values
('31040000-0000-0000-0000-000000000001', '31030000-0000-0000-0000-000000000001', '31010000-0000-0000-0000-000000000001', '31020000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000003', 'Fish and Chips', 'fryer', 1, 13.00, now() - interval '43 minutes'),
('31040000-0000-0000-0000-000000000002', '31030000-0000-0000-0000-000000000001', '31010000-0000-0000-0000-000000000002', '31020000-0000-0000-0000-000000000004', '26100000-0000-0000-0000-000000000010', 'Citrus Mocktail', 'drinks', 1, 6.50, now() - interval '43 minutes'),
('31040000-0000-0000-0000-000000000003', '31030000-0000-0000-0000-000000000002', '31010000-0000-0000-0000-000000000003', '31020000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000005', 'Chocolate Tart', 'dessert', 1, 9.50, now() - interval '11 minutes'),
('31040000-0000-0000-0000-000000000004', '31030000-0000-0000-0000-000000000002', '31010000-0000-0000-0000-000000000004', '31020000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 'drinks', 1, 4.00, now() - interval '11 minutes');

insert into route_plans (route_plan_id, created_from_order_id, notes) values
('28500000-0000-0000-0000-000000000002', '28000000-0000-0000-0000-000000000007', 'Salad first, burger after 5 minutes.');

insert into kitchen_tickets (ticket_id, order_id, station_id, route_plan_id, priority, priority_label, status, blocked_reason, created_at, started_at, ready_at, target_service_at, expedited_at)
values
('28600000-0000-0000-0000-000000000005', '28000000-0000-0000-0000-000000000004', '28400000-0000-0000-0000-000000000005', null, 4, 'normal', 'ready', null, now() - interval '18 minutes', now() - interval '17 minutes', now() - interval '2 minutes', now() + interval '3 minutes', null),
('28600000-0000-0000-0000-000000000006', '28000000-0000-0000-0000-000000000005', '28400000-0000-0000-0000-000000000001', null, 7, 'rush', 'cooking', null, now() - interval '17 minutes', now() - interval '16 minutes', null, now() + interval '4 minutes', null),
('28600000-0000-0000-0000-000000000007', '28000000-0000-0000-0000-000000000005', '28400000-0000-0000-0000-000000000006', null, 5, 'normal', 'blocked', 'Stock check on garnish', now() - interval '15 minutes', now() - interval '14 minutes', null, now() + interval '8 minutes', null),
('28600000-0000-0000-0000-000000000008', '28000000-0000-0000-0000-000000000006', '28400000-0000-0000-0000-000000000003', null, 3, 'normal', 'hold_for_service', null, now() - interval '35 minutes', now() - interval '34 minutes', null, now() + interval '12 minutes', null),
('28600000-0000-0000-0000-000000000009', '28000000-0000-0000-0000-000000000007', '28400000-0000-0000-0000-000000000005', '28500000-0000-0000-0000-000000000002', 8, 'expedite', 'cooking', null, now() - interval '9 minutes', now() - interval '8 minutes', null, now() + interval '5 minutes', now() - interval '7 minutes');

insert into kitchen_ticket_items (ticket_item_id, ticket_id, order_item_id, quantity, status, fire_at, hold_reason) values
('28700000-0000-0000-0000-000000000005', '28600000-0000-0000-0000-000000000005', '28200000-0000-0000-0000-000000000008', 1, 'ready', now() - interval '17 minutes', null),
('28700000-0000-0000-0000-000000000006', '28600000-0000-0000-0000-000000000006', '28200000-0000-0000-0000-000000000010', 1, 'cooking', now() - interval '16 minutes', null),
('28700000-0000-0000-0000-000000000007', '28600000-0000-0000-0000-000000000007', '28200000-0000-0000-0000-000000000019', 1, 'blocked', now() - interval '14 minutes', 'Waiting for prep garnish'),
('28700000-0000-0000-0000-000000000008', '28600000-0000-0000-0000-000000000008', '28200000-0000-0000-0000-000000000013', 2, 'hold_for_service', null, 'Hold until mains clear'),
('28700000-0000-0000-0000-000000000009', '28600000-0000-0000-0000-000000000009', '28200000-0000-0000-0000-000000000015', 1, 'cooking', now() - interval '8 minutes', null),
('28700000-0000-0000-0000-000000000010', '28600000-0000-0000-0000-000000000009', '28200000-0000-0000-0000-000000000016', 1, 'cooking', now() - interval '7 minutes', null);

insert into dish_status_events (event_id, ticket_item_id, handoff_id, type, source_station, details, occurred_at) values
('28900000-0000-0000-0000-000000000003', '28700000-0000-0000-0000-000000000006', null, 'cooking', 'Grill Station', '{"delayMinutes":4}', now() - interval '15 minutes'),
('28900000-0000-0000-0000-000000000004', '28700000-0000-0000-0000-000000000007', null, 'blocked', 'Prep Station', '{"reason":"garnish substitution"}', now() - interval '14 minutes'),
('28900000-0000-0000-0000-000000000005', '28700000-0000-0000-0000-000000000009', null, 'cooking', 'Salad Station', '{"priority":"expedite"}', now() - interval '8 minutes');

insert into bills (bill_id, table_session_id, promotion_campaign_id, status, sub_total, tax_amount, service_fee, tip_amount, grand_total, discount_total, promotion_code, correlation_id, created_at, finalized_at)
values
('29000000-0000-0000-0000-000000000003', '25500000-0000-0000-0000-000000000006', null, 'partially_paid', 52.50, 4.46, 2.63, 8.50, 68.09, 0.00, null, 'corr-bill-003', now() - interval '14 minutes', now() - interval '13 minutes'),
('29000000-0000-0000-0000-000000000004', '25500000-0000-0000-0000-000000000008', '26500000-0000-0000-0000-000000000001', 'refunded', 17.50, 1.49, 0.88, 2.00, 21.87, 0.00, 'LOYAL10', 'corr-bill-004', now() - interval '5 hours 10 minutes', now() - interval '5 hours');

insert into bill_lines (bill_line_id, bill_id, source_order_item_id, label, quantity, line_total) values
('29100000-0000-0000-0000-000000000006', '29000000-0000-0000-0000-000000000003', '28200000-0000-0000-0000-000000000012', 'Fish and Chips', 1, 18.00),
('29100000-0000-0000-0000-000000000007', '29000000-0000-0000-0000-000000000003', '28200000-0000-0000-0000-000000000013', 'Chocolate Tart', 2, 19.00),
('29100000-0000-0000-0000-000000000008', '29000000-0000-0000-0000-000000000003', '28200000-0000-0000-0000-000000000014', 'Citrus Mocktail', 1, 6.50),
('29100000-0000-0000-0000-000000000009', '29000000-0000-0000-0000-000000000004', '28200000-0000-0000-0000-000000000017', 'Truffle Fries', 1, 8.00),
('29100000-0000-0000-0000-000000000010', '29000000-0000-0000-0000-000000000004', '28200000-0000-0000-0000-000000000018', 'Sparkling Lemonade', 1, 5.50);

insert into bill_splits (split_id, bill_id, label, status, allocated_total, tip_amount) values
('29200000-0000-0000-0000-000000000004', '29000000-0000-0000-0000-000000000003', 'Window Party A', 'paid', 34.04, 4.25),
('29200000-0000-0000-0000-000000000005', '29000000-0000-0000-0000-000000000003', 'Window Party B', 'pending', 34.05, 4.25),
('29200000-0000-0000-0000-000000000006', '29000000-0000-0000-0000-000000000004', 'Full Bill', 'paid', 21.87, 2.00);

insert into split_allocations (allocation_id, split_id, bill_line_id, amount, ratio) values
('29300000-0000-0000-0000-000000000008', '29200000-0000-0000-0000-000000000004', '29100000-0000-0000-0000-000000000006', 9.00, 0.5000),
('29300000-0000-0000-0000-000000000009', '29200000-0000-0000-0000-000000000004', '29100000-0000-0000-0000-000000000007', 10.00, 0.5263),
('29300000-0000-0000-0000-000000000010', '29200000-0000-0000-0000-000000000004', '29100000-0000-0000-0000-000000000008', 3.25, 0.5000),
('29300000-0000-0000-0000-000000000011', '29200000-0000-0000-0000-000000000005', '29100000-0000-0000-0000-000000000006', 9.00, 0.5000),
('29300000-0000-0000-0000-000000000012', '29200000-0000-0000-0000-000000000005', '29100000-0000-0000-0000-000000000007', 9.00, 0.4737),
('29300000-0000-0000-0000-000000000013', '29200000-0000-0000-0000-000000000005', '29100000-0000-0000-0000-000000000008', 3.25, 0.5000),
('29300000-0000-0000-0000-000000000014', '29200000-0000-0000-0000-000000000006', '29100000-0000-0000-0000-000000000009', 8.00, 1.0000),
('29300000-0000-0000-0000-000000000015', '29200000-0000-0000-0000-000000000006', '29100000-0000-0000-0000-000000000010', 5.50, 1.0000);

insert into payments (payment_id, bill_id, split_id, method, amount, status, gateway_ref, cash_received, change_due, processed_by_user_id, paid_at)
values
('29400000-0000-0000-0000-000000000003', '29000000-0000-0000-0000-000000000003', '29200000-0000-0000-0000-000000000004', 'debit_card', 34.04, 'completed', 'txn-db-003', null, null, '22000000-0000-0000-0000-000000000006', now() - interval '12 minutes'),
('29400000-0000-0000-0000-000000000004', '29000000-0000-0000-0000-000000000004', '29200000-0000-0000-0000-000000000006', 'mobile_payment', 21.87, 'refunded', 'txn-mp-004', null, null, '22000000-0000-0000-0000-000000000006', now() - interval '5 hours'),
('29400000-0000-0000-0000-000000000005', '29000000-0000-0000-0000-000000000003', null, 'credit_card', 12.00, 'pending', 'txn-cc-005', null, null, '22000000-0000-0000-0000-000000000006', now() - interval '6 minutes');

insert into refunds (refund_id, payment_id, bill_id, amount, reason, status, requested_by, approved_by, processed_at, external_transaction_ref) values
('29500000-0000-0000-0000-000000000002', '29400000-0000-0000-0000-000000000004', '29000000-0000-0000-0000-000000000004', 21.87, 'Private-room bill reopened after guest dispute', 'completed', '22000000-0000-0000-0000-000000000006', '22000000-0000-0000-0000-000000000002', now() - interval '4 hours 55 minutes', 'refund-mp-004'),
('29500000-0000-0000-0000-000000000003', '29400000-0000-0000-0000-000000000005', '29000000-0000-0000-0000-000000000003', 12.00, 'Card terminal retried during split settlement', 'pending_review', '22000000-0000-0000-0000-000000000006', null, null, null);

insert into receipts (receipt_id, payment_id, bill_id, issued_at, delivery_channel, document_no, recipient_address) values
('29600000-0000-0000-0000-000000000002', '29400000-0000-0000-0000-000000000003', '29000000-0000-0000-0000-000000000003', now() - interval '12 minutes', 'email', 'RCP-1002', 'vip.party@example.com'),
('29600000-0000-0000-0000-000000000003', '29400000-0000-0000-0000-000000000004', '29000000-0000-0000-0000-000000000004', now() - interval '5 hours', 'sms', 'RCP-1003', '+1-555-0105');

insert into stock_transactions (transaction_id, inventory_item_id, delta, reason, source_ref, source_type, previous_on_hand, new_on_hand, performed_by_user_id, correlation_id, occurred_at) values
('29700000-0000-0000-0000-000000000003', '27000000-0000-0000-0000-000000000001', -3.00, 'consumption', '28200000-0000-0000-0000-000000000010', 'order_item', 14.00, 11.00, '22000000-0000-0000-0000-000000000005', 'corr-stock-003', now() - interval '16 minutes'),
('29700000-0000-0000-0000-000000000004', '27000000-0000-0000-0000-000000000008', -1.00, 'waste', null, 'manual', 6.00, 5.00, '22000000-0000-0000-0000-000000000002', 'corr-stock-004', now() - interval '1 hour'),
('29700000-0000-0000-0000-000000000005', '27000000-0000-0000-0000-000000000004', 24.00, 'restock', null, 'purchase_order', 24.00, 48.00, '22000000-0000-0000-0000-000000000002', 'corr-stock-005', now() - interval '3 hours'),
('29700000-0000-0000-0000-000000000006', '27000000-0000-0000-0000-000000000006', -2.00, 'return', '29600000-0000-0000-0000-000000000003', 'receipt', 30.00, 28.00, '22000000-0000-0000-0000-000000000006', 'corr-stock-006', now() - interval '4 hours');

insert into low_stock_alerts (alert_id, inventory_item_id, severity, status, created_at, acknowledged_by, acknowledged_at, snoozed_until) values
('29800000-0000-0000-0000-000000000002', '27000000-0000-0000-0000-000000000001', 'medium', 'acknowledged', now() - interval '45 minutes', '22000000-0000-0000-0000-000000000002', now() - interval '40 minutes', null),
('29800000-0000-0000-0000-000000000003', '27000000-0000-0000-0000-000000000008', 'critical', 'snoozed', now() - interval '70 minutes', '22000000-0000-0000-0000-000000000002', now() - interval '65 minutes', now() + interval '20 minutes');

insert into notification_messages (message_id, low_stock_alert_id, reservation_id, payment_id, channel, type, template_code, payload, title, body, recipient_role, recipient_user_id, recipient_address, priority, status, created_at, queued_at, sent_at, read_at, failure_reason)
values
('29900000-0000-0000-0000-000000000003', '29800000-0000-0000-0000-000000000002', null, null, 'email', 'inventory_digest', 'LOW_STOCK_DIGEST', '{"item":"Ribeye Steak","status":"acknowledged"}', 'Low stock acknowledged', 'Manager acknowledged the ribeye threshold alert.', 'manager', '22000000-0000-0000-0000-000000000002', 'john.mitchell@irms.io', 'medium', 'read', now() - interval '38 minutes', now() - interval '38 minutes', now() - interval '37 minutes', now() - interval '20 minutes', null),
('29900000-0000-0000-0000-000000000004', null, '25300000-0000-0000-0000-000000000006', null, 'email', 'reservation_cancelled', 'RESERVATION_CANCELLED', '{"reservationId":"25300000-0000-0000-0000-000000000006"}', 'Reservation cancelled', 'Your reservation has been cancelled as requested.', null, null, 'emma.lewis@example.com', 'low', 'queued', now() - interval '5 minutes', now() - interval '5 minutes', null, null, null),
('29900000-0000-0000-0000-000000000005', null, null, '29400000-0000-0000-0000-000000000005', 'in_app', 'payment_retry', 'PAYMENT_RETRY', '{"paymentId":"29400000-0000-0000-0000-000000000005"}', 'Payment retry required', 'One split payment needs manual retry.', 'cashier', '22000000-0000-0000-0000-000000000006', 'david.ross@irms.io', 'high', 'failed', now() - interval '4 minutes', now() - interval '4 minutes', null, null, 'Terminal timeout');

insert into audit_logs (audit_log_id, actor_user_id, action, entity_type, entity_id, recorded_at, correlation_id, reason, follow_up, ip_address, before_payload, after_payload) values
('2A000000-0000-0000-0000-000000000004', '22000000-0000-0000-0000-000000000002', 'billing.bill.reopened', 'Bill', '29000000-0000-0000-0000-000000000004', now() - interval '4 hours 58 minutes', 'corr-audit-004', 'Customer disputed duplicate mobile charge', true, '192.168.1.55', '{"status":"paid"}', '{"status":"refunded"}'),
('2A000000-0000-0000-0000-000000000005', '22000000-0000-0000-0000-000000000002', 'inventory.manual_adjustment', 'InventoryItem', '27000000-0000-0000-0000-000000000008', now() - interval '58 minutes', 'corr-audit-005', 'Dessert spoilage logged after station close', false, '192.168.1.11', '{"onHand":6}', '{"onHand":5}');

insert into audit_details (detail_id, audit_log_id, field_name, before_value, after_value) values
('2A100000-0000-0000-0000-000000000003', '2A000000-0000-0000-0000-000000000004', 'status', 'paid', 'refunded'),
('2A100000-0000-0000-0000-000000000004', '2A000000-0000-0000-0000-000000000005', 'onHand', '6', '5');

insert into shift_assignments (shift_assignment_id, user_id, branch_id, shift_date, start_at, end_at, position, zone, status) values
('2B000000-0000-0000-0000-000000000006', '22000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', current_date - 1, time '10:00', time '18:00', 'Server', 'Patio', 'completed'),
('2B000000-0000-0000-0000-000000000007', '22000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', current_date + 1, time '17:00', time '23:00', 'Host', 'Front', 'cancelled');

insert into report_queries (query_id, created_by_user_id, filters, group_by, measure, status, requested_format, completed_at) values
('2C000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', '{"period":"week","focus":"operations-and-reports"}', 'day', 'net-revenue', 'completed', 'json', now() - interval '30 seconds');

insert into report_snapshots (snapshot_id, branch_id, type, period_start, period_end, generated_at, payload, query_id) values
('2C100000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'operations', date_trunc('day', now()), now(), now() - interval '30 seconds', '{
  "activeTables": 8,
  "totalTables": 15,
  "openOrders": 12,
  "readyToServe": 3,
  "kitchenQueue": 7,
  "averageKitchenWaitMinutes": 14,
  "averageServiceTimeMinutes": 38,
  "revenueToday": 4280.00,
  "transactionsToday": 42,
  "reservationCountToday": 14,
  "refundsToday": 49.50,
  "revenueTrend": [
    {"hour":"10AM","revenue":450},
    {"hour":"11AM","revenue":820},
    {"hour":"12PM","revenue":1340},
    {"hour":"1PM","revenue":1580},
    {"hour":"2PM","revenue":980},
    {"hour":"3PM","revenue":620},
    {"hour":"4PM","revenue":440},
    {"hour":"5PM","revenue":760},
    {"hour":"6PM","revenue":1420},
    {"hour":"7PM","revenue":1890},
    {"hour":"8PM","revenue":2100},
    {"hour":"9PM","revenue":1650}
  ],
  "weeklyRevenue": [
    {"day":"Mon","revenue":3200},
    {"day":"Tue","revenue":4100},
    {"day":"Wed","revenue":3800},
    {"day":"Thu","revenue":4600},
    {"day":"Fri","revenue":6200},
    {"day":"Sat","revenue":7800},
    {"day":"Sun","revenue":5400}
  ],
  "topDishes": [
    {"name":"Grilled Ribeye","orders":142,"revenue":5396},
    {"name":"Wagyu Burger","orders":98,"revenue":2548},
    {"name":"Fish and Chips","orders":87,"revenue":1566},
    {"name":"Caesar Salad","orders":76,"revenue":950},
    {"name":"Truffle Fries","orders":134,"revenue":1072}
  ],
  "peakHours": [
    {"hour":"11AM","orders":8},
    {"hour":"12PM","orders":22},
    {"hour":"1PM","orders":28},
    {"hour":"2PM","orders":15},
    {"hour":"5PM","orders":12},
    {"hour":"6PM","orders":25},
    {"hour":"7PM","orders":35},
    {"hour":"8PM","orders":38},
    {"hour":"9PM","orders":22}
  ],
  "categoryRevenue": [
    {"name":"Grill","value":8200},
    {"name":"Mains","value":4800},
    {"name":"Appetizers","value":3200},
    {"name":"Drinks","value":2800},
    {"name":"Desserts","value":1600}
  ],
  "kitchenMetrics": [
    {"station":"Grill","avgTime":"14 min","tickets":42,"delayed":3},
    {"station":"Fryer","avgTime":"9 min","tickets":28,"delayed":1},
    {"station":"Dessert","avgTime":"7 min","tickets":15,"delayed":0},
    {"station":"Drinks","avgTime":"4 min","tickets":38,"delayed":0},
    {"station":"Salad","avgTime":"6 min","tickets":22,"delayed":1}
  ]
}'::jsonb, '2C000000-0000-0000-0000-000000000002');

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'reporting_sales_projection'
          and column_name = 'projection_id'
    ) then
        drop table reporting_sales_projection;
    end if;
end $$;

create table if not exists reporting_sales_projection (
    business_date date primary key,
    order_count bigint not null default 0,
    bill_count bigint not null default 0,
    gross_sales numeric(14,2) not null default 0,
    refund_total numeric(14,2) not null default 0,
    net_sales numeric(14,2) not null default 0,
    updated_at timestamp not null default now()
);

insert into reporting_sales_projection (business_date, order_count, bill_count, gross_sales, refund_total, net_sales, updated_at) values
(current_date - 6, 26, 22, 3200.00, 0.00, 3200.00, now() - interval '6 days'),
(current_date - 5, 30, 26, 4100.00, 25.00, 4075.00, now() - interval '5 days'),
(current_date - 4, 28, 24, 3800.00, 0.00, 3800.00, now() - interval '4 days'),
(current_date - 3, 32, 29, 4600.00, 40.00, 4560.00, now() - interval '3 days'),
(current_date - 2, 41, 36, 6200.00, 75.00, 6125.00, now() - interval '2 days'),
(current_date - 1, 48, 43, 7800.00, 120.00, 7680.00, now() - interval '1 day'),
(current_date, 34, 30, 4280.00, 49.50, 4230.50, now() - interval '30 seconds')
on conflict (business_date) do update
set order_count = excluded.order_count,
    bill_count = excluded.bill_count,
    gross_sales = excluded.gross_sales,
    refund_total = excluded.refund_total,
    net_sales = excluded.net_sales,
    updated_at = excluded.updated_at;

insert into reporting_peak_hour_projection (business_date, hour_of_day, order_count, revenue_total, updated_at) values
(current_date, 11, 8, 450.00, now() - interval '30 seconds'),
(current_date, 12, 22, 820.00, now() - interval '30 seconds'),
(current_date, 13, 28, 1340.00, now() - interval '30 seconds'),
(current_date, 14, 15, 980.00, now() - interval '30 seconds'),
(current_date, 17, 12, 620.00, now() - interval '30 seconds'),
(current_date, 18, 25, 1420.00, now() - interval '30 seconds'),
(current_date, 19, 35, 1890.00, now() - interval '30 seconds'),
(current_date, 20, 38, 2100.00, now() - interval '30 seconds'),
(current_date, 21, 22, 1650.00, now() - interval '30 seconds')
on conflict (business_date, hour_of_day) do update
set order_count = excluded.order_count,
    revenue_total = excluded.revenue_total,
    updated_at = excluded.updated_at;

insert into reporting_best_selling_item_projection (business_date, menu_item_id, item_name, quantity_sold, revenue_total, updated_at) values
(current_date, '26100000-0000-0000-0000-000000000002', 'Grilled Ribeye', 142, 5396.00, now() - interval '30 seconds'),
(current_date, '26100000-0000-0000-0000-000000000006', 'Wagyu Burger', 98, 2548.00, now() - interval '30 seconds'),
(current_date, '26100000-0000-0000-0000-000000000003', 'Fish and Chips', 87, 1566.00, now() - interval '30 seconds'),
(current_date, '26100000-0000-0000-0000-000000000007', 'Caesar Salad', 76, 950.00, now() - interval '30 seconds'),
(current_date, '26100000-0000-0000-0000-000000000001', 'Truffle Fries', 134, 1072.00, now() - interval '30 seconds')
on conflict (business_date, menu_item_id) do update
set item_name = excluded.item_name,
    quantity_sold = excluded.quantity_sold,
    revenue_total = excluded.revenue_total,
    updated_at = excluded.updated_at;

insert into reporting_revenue_projection (business_date, payment_method, gross_revenue, discounts, refunds, net_revenue, updated_at) values
(current_date, 'cash', 1160.00, 0.00, 25.00, 1135.00, now() - interval '30 seconds'),
(current_date, 'credit_card', 1740.00, 0.00, 12.00, 1728.00, now() - interval '30 seconds'),
(current_date, 'debit_card', 730.00, 0.00, 0.00, 730.00, now() - interval '30 seconds'),
(current_date, 'mobile_payment', 650.00, 0.00, 12.50, 637.50, now() - interval '30 seconds')
on conflict (business_date, payment_method) do update
set gross_revenue = excluded.gross_revenue,
    discounts = excluded.discounts,
    refunds = excluded.refunds,
    net_revenue = excluded.net_revenue,
    updated_at = excluded.updated_at;

insert into reporting_kitchen_bottleneck_projection (business_date, station, delayed_item_count, average_delay_minutes, updated_at) values
(current_date, 'grill', 3, 14.00, now() - interval '30 seconds'),
(current_date, 'fryer', 1, 9.00, now() - interval '30 seconds'),
(current_date, 'dessert', 0, 7.00, now() - interval '30 seconds'),
(current_date, 'drinks', 0, 4.00, now() - interval '30 seconds'),
(current_date, 'salad', 1, 6.00, now() - interval '30 seconds')
on conflict (business_date, station) do update
set delayed_item_count = excluded.delayed_item_count,
    average_delay_minutes = excluded.average_delay_minutes,
    updated_at = excluded.updated_at;

insert into reporting_staff_efficiency_projection (business_date, staff_id, role, completed_tasks, average_service_time, updated_at) values
(current_date, '22000000-0000-0000-0000-000000000003', 'server', 22, 34.00, now() - interval '30 seconds'),
(current_date, '22000000-0000-0000-0000-000000000004', 'server', 19, 31.00, now() - interval '30 seconds'),
(current_date, '22000000-0000-0000-0000-000000000005', 'chef', 42, 12.00, now() - interval '30 seconds'),
(current_date, '22000000-0000-0000-0000-000000000007', 'host', 16, 9.00, now() - interval '30 seconds')
on conflict (business_date, staff_id) do update
set role = excluded.role,
    completed_tasks = excluded.completed_tasks,
    average_service_time = excluded.average_service_time,
    updated_at = excluded.updated_at;

insert into reporting_inventory_usage_projection (business_date, ingredient_id, ingredient_name, quantity_used, waste_quantity, updated_at) values
(current_date, '27000000-0000-0000-0000-000000000001', 'Ribeye Steak', 11.00, 0.00, now() - interval '30 seconds'),
(current_date, '27000000-0000-0000-0000-000000000002', 'Cod Fillet', 8.00, 0.00, now() - interval '30 seconds'),
(current_date, '27000000-0000-0000-0000-000000000003', 'Potatoes', 14.00, 0.30, now() - interval '30 seconds'),
(current_date, '27000000-0000-0000-0000-000000000007', 'Lemon Syrup', 2.20, 0.10, now() - interval '30 seconds'),
(current_date, '27000000-0000-0000-0000-000000000008', 'Chocolate Base', 5.00, 0.50, now() - interval '30 seconds')
on conflict (business_date, ingredient_id) do update
set ingredient_name = excluded.ingredient_name,
    quantity_used = excluded.quantity_used,
    waste_quantity = excluded.waste_quantity,
    updated_at = excluded.updated_at;

insert into reporting_combo_sales_projection (business_date, combo_id, combo_name, quantity_sold, revenue_total, updated_at) values
(current_date - 1, '31000000-0000-0000-0000-000000000001', 'Lunch Set', 10, 195.00, now() - interval '1 day'),
(current_date, '31000000-0000-0000-0000-000000000001', 'Lunch Set', 12, 234.00, now() - interval '30 seconds'),
(current_date, '31000000-0000-0000-0000-000000000002', 'Sweet Finish Pair', 7, 94.50, now() - interval '30 seconds')
on conflict (business_date, combo_id) do update
set combo_name = excluded.combo_name,
    quantity_sold = excluded.quantity_sold,
    revenue_total = excluded.revenue_total,
    updated_at = excluded.updated_at;
