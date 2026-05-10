insert into branches (branch_id, code, name, address, timezone)
values ('10000000-0000-0000-0000-000000000001', 'MAIN', 'IRMS Demo Restaurant', '100 Market Street', 'UTC');

insert into roles (role_id, name, scope, description) values
('20000000-0000-0000-0000-000000000001', 'admin', 'global', 'System administrator'),
('20000000-0000-0000-0000-000000000002', 'manager', 'branch', 'Restaurant manager'),
('20000000-0000-0000-0000-000000000003', 'server', 'branch', 'Dining room server'),
('20000000-0000-0000-0000-000000000004', 'chef', 'branch', 'Kitchen operator'),
('20000000-0000-0000-0000-000000000005', 'cashier', 'branch', 'Billing and payment operator'),
('20000000-0000-0000-0000-000000000006', 'host', 'branch', 'Reservation and seating operator');

insert into permissions (permission_id, code, description) values
('21000000-0000-0000-0000-000000000001', 'all', 'Full access'),
('21000000-0000-0000-0000-000000000002', 'dashboard.view', 'View dashboard'),
('21000000-0000-0000-0000-000000000003', 'reservations.manage', 'Manage reservations'),
('21000000-0000-0000-0000-000000000004', 'tables.assign', 'Assign tables'),
('21000000-0000-0000-0000-000000000005', 'orders.create', 'Create orders'),
('21000000-0000-0000-0000-000000000006', 'orders.edit', 'Edit orders'),
('21000000-0000-0000-0000-000000000007', 'orders.send', 'Send orders to kitchen'),
('21000000-0000-0000-0000-000000000008', 'kitchen.manage', 'Manage kitchen tickets'),
('21000000-0000-0000-0000-000000000009', 'billing.manage', 'Manage billing'),
('21000000-0000-0000-0000-000000000010', 'payments.process', 'Process payments'),
('21000000-0000-0000-0000-000000000011', 'billing.refund', 'Issue refunds'),
('21000000-0000-0000-0000-000000000012', 'menu.manage', 'Manage menu'),
('21000000-0000-0000-0000-000000000013', 'inventory.manage', 'Manage inventory'),
('21000000-0000-0000-0000-000000000014', 'reports.view', 'View reports'),
('21000000-0000-0000-0000-000000000015', 'staff.manage', 'Manage staff'),
('21000000-0000-0000-0000-000000000016', 'audit.view', 'View audit log'),
('21000000-0000-0000-0000-000000000017', 'settings.view', 'View settings'),
('21000000-0000-0000-0000-000000000018', 'settings.manage', 'Manage settings');

insert into role_permissions (role_id, permission_id) values
('20000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000003'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000004'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000009'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000010'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000011'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000012'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000013'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000014'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000015'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000016'),
('20000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000017'),
('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000013'),
('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000009'),
('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000010'),
('20000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000011'),
('20000000-0000-0000-0000-000000000006', '21000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000006', '21000000-0000-0000-0000-000000000003'),
('20000000-0000-0000-0000-000000000006', '21000000-0000-0000-0000-000000000004');

insert into users (user_id, username, email, display_name, password_hash, status, hire_date)
values
('22000000-0000-0000-0000-000000000001', 'admin@irms.io', 'admin@irms.io', 'Admin User', crypt('Password123!', gen_salt('bf')), 'active', date '2021-01-01'),
('22000000-0000-0000-0000-000000000002', 'john.mitchell@irms.io', 'john.mitchell@irms.io', 'John Mitchell', crypt('Password123!', gen_salt('bf')), 'active', date '2022-03-15'),
('22000000-0000-0000-0000-000000000003', 'maria.santos@irms.io', 'maria.santos@irms.io', 'Maria Santos', crypt('Password123!', gen_salt('bf')), 'active', date '2023-01-10'),
('22000000-0000-0000-0000-000000000004', 'james.kim@irms.io', 'james.kim@irms.io', 'James Kim', crypt('Password123!', gen_salt('bf')), 'active', date '2023-06-22'),
('22000000-0000-0000-0000-000000000005', 'carlos.rodriguez@irms.io', 'carlos.rodriguez@irms.io', 'Chef Rodriguez', crypt('Password123!', gen_salt('bf')), 'active', date '2021-11-05'),
('22000000-0000-0000-0000-000000000006', 'david.ross@irms.io', 'david.ross@irms.io', 'David Ross', crypt('Password123!', gen_salt('bf')), 'active', date '2023-09-01'),
('22000000-0000-0000-0000-000000000007', 'emily.chen@irms.io', 'emily.chen@irms.io', 'Emily Chen', crypt('Password123!', gen_salt('bf')), 'active', date '2024-01-20'),
('22000000-0000-0000-0000-000000000008', 'sarah.lee@irms.io', 'sarah.lee@irms.io', 'Sarah Lee', crypt('Password123!', gen_salt('bf')), 'active', date '2024-02-14'),
('22000000-0000-0000-0000-000000000009', 'tom.baker@irms.io', 'tom.baker@irms.io', 'Tom Baker', crypt('Password123!', gen_salt('bf')), 'inactive', date '2022-05-10');

insert into user_roles (user_id, role_id) values
('22000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
('22000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002'),
('22000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003'),
('22000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003'),
('22000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000004'),
('22000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000005'),
('22000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000006'),
('22000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000003'),
('22000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000004');

insert into authorization_policies (policy_id, policy_key, requires_reason_for_override, max_refund_limit, session_idle_timeout_minutes, session_absolute_timeout_hours)
values ('23000000-0000-0000-0000-000000000001', 'default', true, 50.00, 15, 12);

insert into seating_policies (policy_id, max_hold_minutes, walk_in_bias, reservation_grace_minutes, is_active)
values ('23000000-0000-0000-0000-000000000002', 15, 1.00, 15, true);

insert into pricing_policies (policy_id, name, service_charge_rate, supports_happy_hour, is_active)
values ('23000000-0000-0000-0000-000000000003', 'Default Pricing', 5.00, false, true);

insert into tax_policies (policy_id, name, tax_rate, service_fee_rate, tip_editable, is_active)
values ('23000000-0000-0000-0000-000000000004', 'Default Tax', 8.50, 5.00, true, true);

insert into preparation_policies (policy_id, group_by_station, supports_batching, rush_threshold_min, is_active)
values ('23000000-0000-0000-0000-000000000005', true, true, 15, true);

insert into expedite_rules (rule_id, vip_boost, late_threshold_min, requires_manager_approval, is_active)
values ('23000000-0000-0000-0000-000000000006', 10, 20, true, true);

insert into dining_tables (table_id, branch_id, code, capacity, zone, floor_label, section_label, position_row, position_col, status) values
('24000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'T1', 2, 'Patio', 'Ground', 'A', 1, 1, 'available'),
('24000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'T2', 2, 'Patio', 'Ground', 'A', 1, 2, 'available'),
('24000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'T3', 4, 'Main Hall', 'Ground', 'B', 2, 1, 'occupied'),
('24000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'T4', 4, 'Main Hall', 'Ground', 'B', 2, 2, 'reserved'),
('24000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'T5', 4, 'Main Hall', 'Ground', 'C', 3, 1, 'occupied'),
('24000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'T6', 6, 'Main Hall', 'Ground', 'C', 3, 2, 'available'),
('24000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'T7', 6, 'Window', 'Ground', 'D', 4, 1, 'available'),
('24000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'T8', 6, 'Window', 'Ground', 'D', 4, 2, 'occupied'),
('24000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'T9', 8, 'Private Room', 'Ground', 'E', 5, 1, 'cleaning'),
('24000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'T10', 8, 'Private Room', 'Ground', 'E', 5, 2, 'available');

insert into customer_profiles (customer_profile_id, full_name, phone, email) values
('25000000-0000-0000-0000-000000000001', 'Ava Garcia', '+1-555-0101', 'ava.garcia@example.com'),
('25000000-0000-0000-0000-000000000002', 'Liam Johnson', '+1-555-0102', 'liam.johnson@example.com'),
('25000000-0000-0000-0000-000000000003', 'Olivia Walker', '+1-555-0103', 'olivia.walker@example.com');

insert into reservation_contacts (contact_id, name, phone, email, notification_channel) values
('25100000-0000-0000-0000-000000000001', 'Ava Garcia', '+1-555-0101', 'ava.garcia@example.com', 'sms'),
('25100000-0000-0000-0000-000000000002', 'Liam Johnson', '+1-555-0102', 'liam.johnson@example.com', 'email'),
('25100000-0000-0000-0000-000000000003', 'Olivia Walker', '+1-555-0103', 'olivia.walker@example.com', 'sms');

insert into seating_preferences (seating_preference_id, window_side, indoor_only, high_chair_count, table_area_hint) values
('25200000-0000-0000-0000-000000000001', true, false, 0, 'Window'),
('25200000-0000-0000-0000-000000000002', false, true, 1, 'Main Hall');

insert into reservations (reservation_id, branch_id, customer_profile_id, contact_id, seating_preference_id, arrival_at, party_size, status, source, notes, confirmed_at, hold_expires_at)
values
('25300000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000001', '25100000-0000-0000-0000-000000000001', '25200000-0000-0000-0000-000000000001', now() + interval '2 hours', 4, 'confirmed', 'online', 'Birthday dinner', now(), now() + interval '2 hours 15 minutes'),
('25300000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000002', '25100000-0000-0000-0000-000000000002', null, now() + interval '1 hours', 2, 'pending', 'phone', 'High chair requested', null, now() + interval '1 hours 15 minutes');

insert into waitlist_entries (waitlist_entry_id, branch_id, customer_profile_id, seating_preference_id, contact_name, contact_phone, contact_email, notification_channel, party_size, quoted_wait_min, status, notes, priority, added_at, hold_expires_at)
values
('25400000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '25000000-0000-0000-0000-000000000003', '25200000-0000-0000-0000-000000000001', 'Olivia Walker', '+1-555-0103', 'olivia.walker@example.com', 'sms', 2, 20, 'waiting', 'Prefers window if possible', 1, now() - interval '10 minutes', now() + interval '10 minutes');

insert into table_sessions (session_id, table_id, reservation_id, server_user_id, guest_count, status, correlation_id, opened_at)
values
('25500000-0000-0000-0000-000000000001', '24000000-0000-0000-0000-000000000003', null, '22000000-0000-0000-0000-000000000003', 4, 'active', 'corr-session-001', now() - interval '40 minutes'),
('25500000-0000-0000-0000-000000000002', '24000000-0000-0000-0000-000000000005', null, '22000000-0000-0000-0000-000000000004', 3, 'billing', 'corr-session-002', now() - interval '55 minutes'),
('25500000-0000-0000-0000-000000000003', '24000000-0000-0000-0000-000000000008', null, '22000000-0000-0000-0000-000000000008', 5, 'active', 'corr-session-003', now() - interval '30 minutes');

insert into table_assignments (assignment_id, reservation_id, table_id, assigned_at, reason, score)
values
('25600000-0000-0000-0000-000000000001', '25300000-0000-0000-0000-000000000001', '24000000-0000-0000-0000-000000000004', now(), 'reservation_match', 92.50);

insert into menu_categories (category_id, name, display_order, is_active) values
('26000000-0000-0000-0000-000000000001', 'Starters', 1, true),
('26000000-0000-0000-0000-000000000002', 'Mains', 2, true),
('26000000-0000-0000-0000-000000000003', 'Drinks', 3, true),
('26000000-0000-0000-0000-000000000004', 'Desserts', 4, true);

insert into menu_items (menu_item_id, category_id, name, description, base_price, station, availability, sale_status, preparation_time_min, allergens_json) values
('26100000-0000-0000-0000-000000000001', '26000000-0000-0000-0000-000000000001', 'Truffle Fries', 'Crisp fries with truffle oil and parmesan.', 8.00, 'fryer', 'available', 'active', 8, '["milk"]'),
('26100000-0000-0000-0000-000000000002', '26000000-0000-0000-0000-000000000002', 'Grilled Ribeye', 'Grilled ribeye steak with herb butter.', 38.00, 'grill', 'available', 'active', 16, '["milk"]'),
('26100000-0000-0000-0000-000000000003', '26000000-0000-0000-0000-000000000002', 'Fish and Chips', 'Beer-battered cod with tartar sauce.', 18.00, 'fryer', 'available', 'active', 12, '["fish","gluten"]'),
('26100000-0000-0000-0000-000000000004', '26000000-0000-0000-0000-000000000003', 'Sparkling Lemonade', 'Fresh lemonade with sparkling water.', 5.50, 'drinks', 'available', 'active', 3, '[]'),
('26100000-0000-0000-0000-000000000005', '26000000-0000-0000-0000-000000000004', 'Chocolate Tart', 'Dark chocolate tart with sea salt.', 9.50, 'dessert', 'available', 'active', 6, '["milk","gluten","egg"]'),
('26100000-0000-0000-0000-000000000006', '26000000-0000-0000-0000-000000000002', 'Wagyu Burger', 'Wagyu beef burger with aged cheddar.', 26.00, 'grill', 'available', 'active', 14, '["milk","gluten"]');

insert into modifier_groups (group_id, menu_item_id, name, min_select, max_select, required, multi_select, display_order) values
('26200000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000002', 'Steak Temperature', 1, 1, true, false, 1),
('26200000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000006', 'Burger Add-ons', 0, 3, false, true, 1);

insert into modifier_options (option_id, group_id, name, extra_price, active, display_order) values
('26300000-0000-0000-0000-000000000001', '26200000-0000-0000-0000-000000000001', 'Medium Rare', 0.00, true, 1),
('26300000-0000-0000-0000-000000000002', '26200000-0000-0000-0000-000000000001', 'Medium', 0.00, true, 2),
('26300000-0000-0000-0000-000000000003', '26200000-0000-0000-0000-000000000002', 'Extra Cheese', 2.50, true, 1),
('26300000-0000-0000-0000-000000000004', '26200000-0000-0000-0000-000000000002', 'Crispy Bacon', 3.00, true, 2);

insert into availability_rules (rule_id, menu_item_id, effective_from, effective_to, out_of_stock_hides_item, is_active) values
('26400000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000001', null, null, false, true);

insert into promotion_campaigns (promotion_campaign_id, code, name, discount_type, discount_value, effective_from, effective_to, is_active) values
('26500000-0000-0000-0000-000000000001', 'LOYAL10', 'Loyal Guest Discount', 'percentage', 10.00, now() - interval '1 day', now() + interval '30 days', true);

insert into inventory_items (inventory_item_id, branch_id, name, unit, on_hand, reserved_qty, threshold, minimum_stock, maximum_stock, cost_per_unit, category, preferred_vendor, last_restocked_at) values
('27000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Ribeye Steak', 'portion', 14, 0, 6, 6, 30, 18.50, 'Protein', 'Prime Cuts Co.', now() - interval '1 day'),
('27000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Cod Fillet', 'portion', 10, 0, 4, 4, 20, 8.75, 'Protein', 'Ocean Fresh', now() - interval '2 days'),
('27000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Potatoes', 'kg', 18, 0, 5, 5, 40, 1.10, 'Produce', 'Green Farm', now() - interval '2 days'),
('27000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'Burger Buns', 'piece', 24, 0, 10, 10, 60, 0.60, 'Bakery', 'Golden Bakery', now() - interval '1 day'),
('27000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'Wagyu Patty', 'piece', 8, 0, 4, 4, 20, 9.20, 'Protein', 'Prime Cuts Co.', now() - interval '1 day'),
('27000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'Sparkling Water', 'bottle', 30, 0, 8, 8, 60, 1.40, 'Beverage', 'Spring Source', now() - interval '3 days'),
('27000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'Lemon Syrup', 'liter', 2, 0, 3, 3, 12, 4.50, 'Beverage', 'Citrus Supply', now() - interval '2 days'),
('27000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'Chocolate Base', 'portion', 6, 0, 2, 2, 12, 2.80, 'Dessert', 'Sweet Works', now() - interval '2 days');

insert into recipes (recipe_id, menu_item_id, version, yield_unit, is_active) values
('27100000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000001', 1, 'plate', true),
('27100000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000002', 1, 'plate', true),
('27100000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000003', 1, 'plate', true),
('27100000-0000-0000-0000-000000000004', '26100000-0000-0000-0000-000000000004', 1, 'glass', true),
('27100000-0000-0000-0000-000000000005', '26100000-0000-0000-0000-000000000005', 1, 'portion', true),
('27100000-0000-0000-0000-000000000006', '26100000-0000-0000-0000-000000000006', 1, 'plate', true);

insert into recipe_lines (recipe_line_id, recipe_id, inventory_item_id, required_qty, waste_factor) values
('27200000-0000-0000-0000-000000000001', '27100000-0000-0000-0000-000000000001', '27000000-0000-0000-0000-000000000003', 0.35, 0.05),
('27200000-0000-0000-0000-000000000002', '27100000-0000-0000-0000-000000000002', '27000000-0000-0000-0000-000000000001', 1.00, 0.02),
('27200000-0000-0000-0000-000000000003', '27100000-0000-0000-0000-000000000003', '27000000-0000-0000-0000-000000000002', 1.00, 0.03),
('27200000-0000-0000-0000-000000000004', '27100000-0000-0000-0000-000000000003', '27000000-0000-0000-0000-000000000003', 0.40, 0.05),
('27200000-0000-0000-0000-000000000005', '27100000-0000-0000-0000-000000000004', '27000000-0000-0000-0000-000000000006', 1.00, 0.00),
('27200000-0000-0000-0000-000000000006', '27100000-0000-0000-0000-000000000004', '27000000-0000-0000-0000-000000000007', 0.05, 0.02),
('27200000-0000-0000-0000-000000000007', '27100000-0000-0000-0000-000000000005', '27000000-0000-0000-0000-000000000008', 1.00, 0.02),
('27200000-0000-0000-0000-000000000008', '27100000-0000-0000-0000-000000000006', '27000000-0000-0000-0000-000000000005', 1.00, 0.02),
('27200000-0000-0000-0000-000000000009', '27100000-0000-0000-0000-000000000006', '27000000-0000-0000-0000-000000000004', 1.00, 0.00);

insert into reorder_rules (rule_id, inventory_item_id, threshold, preferred_vendor, target_qty, lead_time_days, is_active) values
('27300000-0000-0000-0000-000000000001', '27000000-0000-0000-0000-000000000001', 6, 'Prime Cuts Co.', 20, 2, true),
('27300000-0000-0000-0000-000000000002', '27000000-0000-0000-0000-000000000007', 3, 'Citrus Supply', 10, 1, true);

insert into orders (order_id, table_session_id, channel, status, server_user_id, special_instructions, correlation_id, created_at, confirmed_at) values
('28000000-0000-0000-0000-000000000001', '25500000-0000-0000-0000-000000000001', 'dine_in', 'in_progress', '22000000-0000-0000-0000-000000000003', 'Fire mains together.', 'corr-order-001', now() - interval '35 minutes', now() - interval '33 minutes'),
('28000000-0000-0000-0000-000000000002', '25500000-0000-0000-0000-000000000002', 'dine_in', 'ready', '22000000-0000-0000-0000-000000000004', 'One guest has a gluten allergy.', 'corr-order-002', now() - interval '40 minutes', now() - interval '38 minutes'),
('28000000-0000-0000-0000-000000000003', '25500000-0000-0000-0000-000000000003', 'dine_in', 'confirmed', '22000000-0000-0000-0000-000000000008', 'Dessert after mains.', 'corr-order-003', now() - interval '18 minutes', now() - interval '16 minutes');

insert into order_snapshots (snapshot_id, order_id, menu_version, priced_at, currency, subtotal) values
('28100000-0000-0000-0000-000000000001', '28000000-0000-0000-0000-000000000001', 'menu-v1', now() - interval '33 minutes', 'USD', 54.00),
('28100000-0000-0000-0000-000000000002', '28000000-0000-0000-0000-000000000002', 'menu-v1', now() - interval '38 minutes', 'USD', 31.50),
('28100000-0000-0000-0000-000000000003', '28000000-0000-0000-0000-000000000003', 'menu-v1', now() - interval '16 minutes', 'USD', 43.50);

insert into order_items (order_item_id, order_id, menu_item_id, snapshot_name, quantity, unit_price, special_instruction, allergy_notes, line_status, sent_to_kitchen_at) values
('28200000-0000-0000-0000-000000000001', '28000000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000002', 'Grilled Ribeye', 1, 38.00, 'No butter on top.', null, 'cooking', now() - interval '30 minutes'),
('28200000-0000-0000-0000-000000000002', '28000000-0000-0000-0000-000000000001', '26100000-0000-0000-0000-000000000001', 'Truffle Fries', 2, 8.00, null, null, 'ready', now() - interval '29 minutes'),
('28200000-0000-0000-0000-000000000003', '28000000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000003', 'Fish and Chips', 1, 18.00, null, 'Gluten allergy noted for another guest.', 'ready', now() - interval '34 minutes'),
('28200000-0000-0000-0000-000000000004', '28000000-0000-0000-0000-000000000002', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 1, 5.50, null, null, 'served', now() - interval '36 minutes'),
('28200000-0000-0000-0000-000000000005', '28000000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000006', 'Wagyu Burger', 1, 26.00, 'Add extra cheese.', null, 'sent_to_kitchen', now() - interval '14 minutes'),
('28200000-0000-0000-0000-000000000006', '28000000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000005', 'Chocolate Tart', 1, 9.50, 'Serve later.', null, 'hold_for_service', null),
('28200000-0000-0000-0000-000000000007', '28000000-0000-0000-0000-000000000003', '26100000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 1, 5.50, null, null, 'served', now() - interval '15 minutes');

insert into order_item_modifiers (selection_id, order_item_id, modifier_option_id, name_snapshot, extra_price, qty_multiplier) values
('28300000-0000-0000-0000-000000000001', '28200000-0000-0000-0000-000000000001', '26300000-0000-0000-0000-000000000001', 'Medium Rare', 0.00, 1),
('28300000-0000-0000-0000-000000000002', '28200000-0000-0000-0000-000000000005', '26300000-0000-0000-0000-000000000003', 'Extra Cheese', 2.50, 1);

insert into kitchen_stations (station_id, name, kind, max_parallel_tickets) values
('28400000-0000-0000-0000-000000000001', 'Grill Station', 'grill', 6),
('28400000-0000-0000-0000-000000000002', 'Fryer Station', 'fryer', 5),
('28400000-0000-0000-0000-000000000003', 'Dessert Station', 'dessert', 3),
('28400000-0000-0000-0000-000000000004', 'Drinks Station', 'drinks', 8);

insert into route_plans (route_plan_id, created_from_order_id, notes) values
('28500000-0000-0000-0000-000000000001', '28000000-0000-0000-0000-000000000003', 'Immediate mains and drinks, dessert held.');

insert into kitchen_tickets (ticket_id, order_id, station_id, route_plan_id, priority, priority_label, status, created_at, started_at, ready_at, target_service_at)
values
('28600000-0000-0000-0000-000000000001', '28000000-0000-0000-0000-000000000001', '28400000-0000-0000-0000-000000000001', null, 5, 'normal', 'cooking', now() - interval '30 minutes', now() - interval '29 minutes', null, now() - interval '15 minutes'),
('28600000-0000-0000-0000-000000000002', '28000000-0000-0000-0000-000000000001', '28400000-0000-0000-0000-000000000002', null, 5, 'normal', 'ready', now() - interval '29 minutes', now() - interval '28 minutes', now() - interval '20 minutes', now() - interval '18 minutes'),
('28600000-0000-0000-0000-000000000003', '28000000-0000-0000-0000-000000000002', '28400000-0000-0000-0000-000000000002', null, 8, 'rush', 'ready', now() - interval '34 minutes', now() - interval '33 minutes', now() - interval '12 minutes', now() - interval '15 minutes'),
('28600000-0000-0000-0000-000000000004', '28000000-0000-0000-0000-000000000003', '28400000-0000-0000-0000-000000000001', '28500000-0000-0000-0000-000000000001', 10, 'expedite', 'started', now() - interval '14 minutes', now() - interval '12 minutes', null, now() + interval '2 minutes');

insert into kitchen_ticket_items (ticket_item_id, ticket_id, order_item_id, quantity, status, fire_at, hold_reason) values
('28700000-0000-0000-0000-000000000001', '28600000-0000-0000-0000-000000000001', '28200000-0000-0000-0000-000000000001', 1, 'cooking', now() - interval '29 minutes', null),
('28700000-0000-0000-0000-000000000002', '28600000-0000-0000-0000-000000000002', '28200000-0000-0000-0000-000000000002', 2, 'ready', now() - interval '28 minutes', null),
('28700000-0000-0000-0000-000000000003', '28600000-0000-0000-0000-000000000003', '28200000-0000-0000-0000-000000000003', 1, 'ready', now() - interval '33 minutes', null),
('28700000-0000-0000-0000-000000000004', '28600000-0000-0000-0000-000000000004', '28200000-0000-0000-0000-000000000005', 1, 'started', now() - interval '12 minutes', null);

insert into serve_handoffs (handoff_id, ticket_id, server_id, handed_off_at, accepted_at)
values ('28800000-0000-0000-0000-000000000001', '28600000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000004', now() - interval '10 minutes', now() - interval '9 minutes');

insert into dish_status_events (event_id, ticket_item_id, handoff_id, type, source_station, details, occurred_at) values
('28900000-0000-0000-0000-000000000001', '28700000-0000-0000-0000-000000000001', null, 'started', 'Grill Station', '{"note":"Cooking started"}', now() - interval '29 minutes'),
('28900000-0000-0000-0000-000000000002', '28700000-0000-0000-0000-000000000003', '28800000-0000-0000-0000-000000000001', 'served', 'Fryer Station', '{"table":"T5"}', now() - interval '8 minutes');

insert into bills (bill_id, table_session_id, promotion_campaign_id, status, sub_total, tax_amount, service_fee, tip_amount, grand_total, discount_total, promotion_code, correlation_id, created_at, finalized_at)
values
('29000000-0000-0000-0000-000000000001', '25500000-0000-0000-0000-000000000002', null, 'partially_paid', 31.50, 2.68, 1.58, 4.00, 39.76, 0.00, null, 'corr-bill-001', now() - interval '15 minutes', now() - interval '14 minutes'),
('29000000-0000-0000-0000-000000000002', '25500000-0000-0000-0000-000000000003', '26500000-0000-0000-0000-000000000001', 'open', 43.50, 3.70, 2.18, 0.00, 49.38, 4.35, 'LOYAL10', 'corr-bill-002', now() - interval '5 minutes', now() - interval '4 minutes');

insert into bill_lines (bill_line_id, bill_id, source_order_item_id, label, quantity, line_total) values
('29100000-0000-0000-0000-000000000001', '29000000-0000-0000-0000-000000000001', '28200000-0000-0000-0000-000000000003', 'Fish and Chips', 1, 18.00),
('29100000-0000-0000-0000-000000000002', '29000000-0000-0000-0000-000000000001', '28200000-0000-0000-0000-000000000004', 'Sparkling Lemonade', 1, 5.50),
('29100000-0000-0000-0000-000000000003', '29000000-0000-0000-0000-000000000002', '28200000-0000-0000-0000-000000000005', 'Wagyu Burger', 1, 28.50),
('29100000-0000-0000-0000-000000000004', '29000000-0000-0000-0000-000000000002', '28200000-0000-0000-0000-000000000006', 'Chocolate Tart', 1, 9.50),
('29100000-0000-0000-0000-000000000005', '29000000-0000-0000-0000-000000000002', '28200000-0000-0000-0000-000000000007', 'Sparkling Lemonade', 1, 5.50);

insert into bill_splits (split_id, bill_id, label, status, allocated_total, tip_amount) values
('29200000-0000-0000-0000-000000000001', '29000000-0000-0000-0000-000000000001', 'Guest 1', 'paid', 19.88, 2.00),
('29200000-0000-0000-0000-000000000002', '29000000-0000-0000-0000-000000000001', 'Guest 2', 'pending', 19.88, 2.00),
('29200000-0000-0000-0000-000000000003', '29000000-0000-0000-0000-000000000002', 'Full Bill', 'pending', 49.38, 0.00);

insert into split_allocations (allocation_id, split_id, bill_line_id, amount, ratio) values
('29300000-0000-0000-0000-000000000001', '29200000-0000-0000-0000-000000000001', '29100000-0000-0000-0000-000000000001', 11.00, 0.6111),
('29300000-0000-0000-0000-000000000002', '29200000-0000-0000-0000-000000000001', '29100000-0000-0000-0000-000000000002', 2.75, 0.5000),
('29300000-0000-0000-0000-000000000003', '29200000-0000-0000-0000-000000000002', '29100000-0000-0000-0000-000000000001', 7.00, 0.3889),
('29300000-0000-0000-0000-000000000004', '29200000-0000-0000-0000-000000000002', '29100000-0000-0000-0000-000000000002', 2.75, 0.5000),
('29300000-0000-0000-0000-000000000005', '29200000-0000-0000-0000-000000000003', '29100000-0000-0000-0000-000000000003', 28.50, 1.0000),
('29300000-0000-0000-0000-000000000006', '29200000-0000-0000-0000-000000000003', '29100000-0000-0000-0000-000000000004', 9.50, 1.0000),
('29300000-0000-0000-0000-000000000007', '29200000-0000-0000-0000-000000000003', '29100000-0000-0000-0000-000000000005', 5.50, 1.0000);

insert into payments (payment_id, bill_id, split_id, method, amount, status, gateway_ref, cash_received, change_due, processed_by_user_id, paid_at)
values
('29400000-0000-0000-0000-000000000001', '29000000-0000-0000-0000-000000000001', '29200000-0000-0000-0000-000000000001', 'cash', 19.88, 'completed', null, 20.00, 0.12, '22000000-0000-0000-0000-000000000006', now() - interval '12 minutes'),
('29400000-0000-0000-0000-000000000002', '29000000-0000-0000-0000-000000000001', null, 'credit_card', 5.00, 'refunded', 'txn-cc-001', null, null, '22000000-0000-0000-0000-000000000006', now() - interval '11 minutes');

insert into refunds (refund_id, payment_id, bill_id, amount, reason, status, requested_by, approved_by, processed_at, external_transaction_ref) values
('29500000-0000-0000-0000-000000000001', '29400000-0000-0000-0000-000000000002', '29000000-0000-0000-0000-000000000001', 5.00, 'Customer complaint: wrong dish served', 'completed', '22000000-0000-0000-0000-000000000006', '22000000-0000-0000-0000-000000000002', now() - interval '10 minutes', 'refund-cc-001');

insert into receipts (receipt_id, payment_id, bill_id, issued_at, delivery_channel, document_no, recipient_address) values
('29600000-0000-0000-0000-000000000001', '29400000-0000-0000-0000-000000000001', '29000000-0000-0000-0000-000000000001', now() - interval '12 minutes', 'print', 'RCP-1001', null);

insert into stock_transactions (transaction_id, inventory_item_id, delta, reason, source_ref, source_type, previous_on_hand, new_on_hand, performed_by_user_id, correlation_id, occurred_at) values
('29700000-0000-0000-0000-000000000001', '27000000-0000-0000-0000-000000000002', -1.00, 'consumption', '28200000-0000-0000-0000-000000000003', 'order_item', 11.00, 10.00, '22000000-0000-0000-0000-000000000005', 'corr-stock-001', now() - interval '9 minutes'),
('29700000-0000-0000-0000-000000000002', '27000000-0000-0000-0000-000000000007', -1.00, 'manual_adjustment', null, 'manual', 3.00, 2.00, '22000000-0000-0000-0000-000000000002', 'corr-stock-002', now() - interval '20 minutes');

update inventory_items set on_hand = 2 where inventory_item_id = '27000000-0000-0000-0000-000000000007';

insert into low_stock_alerts (alert_id, inventory_item_id, severity, status, created_at) values
('29800000-0000-0000-0000-000000000001', '27000000-0000-0000-0000-000000000007', 'high', 'open', now() - interval '19 minutes');

insert into notification_messages (message_id, low_stock_alert_id, reservation_id, payment_id, channel, type, template_code, payload, title, body, recipient_role, recipient_user_id, recipient_address, priority, status, created_at, queued_at, sent_at)
values
('29900000-0000-0000-0000-000000000001', '29800000-0000-0000-0000-000000000001', null, null, 'in_app', 'low_stock', 'LOW_STOCK_ALERT', '{"item":"Lemon Syrup","onHand":2}', 'Low stock alert', 'Lemon Syrup is below the configured threshold.', 'manager', '22000000-0000-0000-0000-000000000002', 'manager@irms.io', 'high', 'sent', now() - interval '18 minutes', now() - interval '18 minutes', now() - interval '17 minutes'),
('29900000-0000-0000-0000-000000000002', null, '25300000-0000-0000-0000-000000000001', null, 'sms', 'reservation_reminder', 'RESERVATION_CONFIRMATION', '{"reservationId":"25300000-0000-0000-0000-000000000001"}', 'Reservation confirmed', 'Your table has been confirmed for later today.', null, null, '+1-555-0101', 'medium', 'sent', now() - interval '2 minutes', now() - interval '2 minutes', now() - interval '1 minutes');

insert into audit_logs (audit_log_id, actor_user_id, action, entity_type, entity_id, recorded_at, correlation_id, reason, follow_up, ip_address, before_payload, after_payload) values
('2A000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000006', 'billing.refund.issued', 'Payment', '29400000-0000-0000-0000-000000000002', now() - interval '10 minutes', 'corr-audit-001', 'Customer complaint: wrong dish served', true, '192.168.1.42', '{"status":"completed","amount":5.00}', '{"status":"refunded","refundAmount":5.00}'),
('2A000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', 'inventory.manual_adjustment', 'InventoryItem', '27000000-0000-0000-0000-000000000007', now() - interval '20 minutes', 'corr-audit-002', 'Spoilage: expired syrup', false, '192.168.1.10', '{"onHand":3}', '{"onHand":2}'),
('2A000000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000002', 'iam.role.modified', 'User', '22000000-0000-0000-0000-000000000006', now() - interval '1 day', 'corr-audit-003', 'Promoted to senior cashier', true, '192.168.1.10', '{"roles":["cashier"]}', '{"roles":["cashier"]}');

insert into audit_details (detail_id, audit_log_id, field_name, before_value, after_value) values
('2A100000-0000-0000-0000-000000000001', '2A000000-0000-0000-0000-000000000001', 'status', 'completed', 'refunded'),
('2A100000-0000-0000-0000-000000000002', '2A000000-0000-0000-0000-000000000002', 'onHand', '3', '2');

insert into shift_assignments (shift_assignment_id, user_id, branch_id, shift_date, start_at, end_at, position, zone, status) values
('2B000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', current_date, time '11:00', time '19:00', 'Server', 'Section A', 'scheduled'),
('2B000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', current_date, time '16:00', time '23:00', 'Server', 'Section B', 'scheduled'),
('2B000000-0000-0000-0000-000000000003', '22000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', current_date, time '10:00', time '22:00', 'Chef', 'Grill', 'scheduled'),
('2B000000-0000-0000-0000-000000000004', '22000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', current_date, time '11:00', time '19:00', 'Cashier', 'Register 1', 'scheduled'),
('2B000000-0000-0000-0000-000000000005', '22000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', current_date, time '17:00', time '23:00', 'Host', 'Front', 'scheduled');

insert into report_queries (query_id, created_by_user_id, filters, group_by, measure, status, requested_format, completed_at) values
('2C000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000002', '{"period":"today"}', 'hour', 'revenue', 'completed', 'json', now() - interval '2 minutes');

insert into report_snapshots (snapshot_id, branch_id, type, period_start, period_end, generated_at, payload, query_id) values
('2C100000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'operations', date_trunc('day', now()), now(), now() - interval '2 minutes', '{
  "activeTables": 3,
  "totalTables": 10,
  "openOrders": 3,
  "readyToServe": 2,
  "kitchenQueue": 4,
  "averageKitchenWaitMinutes": 14,
  "revenueToday": 89.14,
  "transactionsToday": 2,
  "reservationCountToday": 2,
  "refundsToday": 5.00,
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
}'::jsonb, '2C000000-0000-0000-0000-000000000001');

insert into outbox_events (event_id, type, aggregate_type, aggregate_id, payload, correlation_id, status, occurred_at, processed_at) values
('2D000000-0000-0000-0000-000000000001', 'ReservationConfirmed', 'Reservation', '25300000-0000-0000-0000-000000000001', '{"reservationId":"25300000-0000-0000-0000-000000000001"}', 'corr-outbox-001', 'processed', now() - interval '2 minutes', now() - interval '1 minutes'),
('2D000000-0000-0000-0000-000000000002', 'InventoryLow', 'InventoryItem', '27000000-0000-0000-0000-000000000007', '{"inventoryItemId":"27000000-0000-0000-0000-000000000007"}', 'corr-outbox-002', 'processed', now() - interval '19 minutes', now() - interval '18 minutes');
