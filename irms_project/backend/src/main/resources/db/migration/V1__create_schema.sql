create extension if not exists pgcrypto;

create table branches (
    branch_id uuid primary key,
    code varchar(50) not null unique,
    name varchar(150) not null,
    address varchar(255),
    timezone varchar(100) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table customer_profiles (
    customer_profile_id uuid primary key,
    full_name varchar(150) not null,
    phone varchar(50) not null,
    email varchar(150),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (phone),
    unique (email)
);

create table roles (
    role_id uuid primary key,
    name varchar(50) not null unique,
    scope varchar(50) not null,
    description varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (scope in ('global', 'branch'))
);

create table permissions (
    permission_id uuid primary key,
    code varchar(100) not null unique,
    description varchar(255) not null,
    created_at timestamptz not null default now()
);

create table users (
    user_id uuid primary key,
    username varchar(100) not null unique,
    email varchar(150) not null unique,
    display_name varchar(150) not null,
    password_hash varchar(255) not null,
    status varchar(30) not null,
    avatar_url varchar(255),
    hire_date date,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_login_at timestamptz,
    check (status in ('active', 'inactive', 'locked'))
);

create table user_roles (
    user_id uuid not null references users(user_id) on delete cascade,
    role_id uuid not null references roles(role_id) on delete cascade,
    primary key (user_id, role_id)
);

create table role_permissions (
    role_id uuid not null references roles(role_id) on delete cascade,
    permission_id uuid not null references permissions(permission_id) on delete cascade,
    primary key (role_id, permission_id)
);

create table user_sessions (
    session_id uuid primary key,
    user_id uuid not null references users(user_id) on delete cascade,
    token_hash varchar(128) not null unique,
    started_at timestamptz not null,
    expires_at timestamptz not null,
    last_activity_at timestamptz not null,
    device_id varchar(150),
    ip_address varchar(100),
    status varchar(30) not null,
    ended_at timestamptz,
    created_at timestamptz not null default now(),
    check (status in ('active', 'locked', 'expired', 'terminated'))
);

create table authorization_policies (
    policy_id uuid primary key,
    policy_key varchar(80) not null unique,
    requires_reason_for_override boolean not null,
    max_refund_limit numeric(12,2) not null,
    session_idle_timeout_minutes integer not null,
    session_absolute_timeout_hours integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (max_refund_limit >= 0),
    check (session_idle_timeout_minutes > 0),
    check (session_absolute_timeout_hours > 0)
);

create table seating_policies (
    policy_id uuid primary key,
    max_hold_minutes integer not null,
    walk_in_bias numeric(5,2) not null,
    reservation_grace_minutes integer not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (max_hold_minutes > 0),
    check (walk_in_bias >= 0),
    check (reservation_grace_minutes >= 0)
);

create table pricing_policies (
    policy_id uuid primary key,
    name varchar(120) not null unique,
    service_charge_rate numeric(5,2) not null,
    supports_happy_hour boolean not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (service_charge_rate >= 0)
);

create table tax_policies (
    policy_id uuid primary key,
    name varchar(120) not null unique,
    tax_rate numeric(5,2) not null,
    service_fee_rate numeric(5,2) not null,
    tip_editable boolean not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (tax_rate >= 0),
    check (service_fee_rate >= 0)
);

create table preparation_policies (
    policy_id uuid primary key,
    group_by_station boolean not null,
    supports_batching boolean not null,
    rush_threshold_min integer not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (rush_threshold_min >= 0)
);

create table expedite_rules (
    rule_id uuid primary key,
    vip_boost integer not null,
    late_threshold_min integer not null,
    requires_manager_approval boolean not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (vip_boost >= 0),
    check (late_threshold_min >= 0)
);

create table reservation_contacts (
    contact_id uuid primary key,
    name varchar(150) not null,
    phone varchar(50) not null,
    email varchar(150),
    notification_channel varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (notification_channel in ('sms', 'email', 'phone', 'in_app'))
);

create table seating_preferences (
    seating_preference_id uuid primary key,
    window_side boolean not null default false,
    indoor_only boolean not null default false,
    high_chair_count integer not null default 0,
    table_area_hint varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (high_chair_count >= 0)
);

create table dining_tables (
    table_id uuid primary key,
    branch_id uuid not null references branches(branch_id),
    code varchar(50) not null unique,
    capacity integer not null,
    zone varchar(100) not null,
    floor_label varchar(100),
    section_label varchar(100),
    position_row integer not null default 0,
    position_col integer not null default 0,
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (capacity > 0),
    check (status in ('available', 'reserved', 'occupied', 'cleaning', 'out_of_service'))
);

create table reservations (
    reservation_id uuid primary key,
    branch_id uuid not null references branches(branch_id),
    customer_profile_id uuid references customer_profiles(customer_profile_id),
    contact_id uuid not null references reservation_contacts(contact_id),
    seating_preference_id uuid references seating_preferences(seating_preference_id),
    arrival_at timestamptz not null,
    party_size integer not null,
    status varchar(30) not null,
    source varchar(30) not null,
    notes varchar(500),
    confirmed_at timestamptz,
    checked_in_at timestamptz,
    cancelled_at timestamptz,
    cancel_reason varchar(255),
    hold_expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (party_size > 0),
    check (status in ('pending', 'confirmed', 'seated', 'completed', 'cancelled', 'no_show')),
    check (source in ('phone', 'walk_in', 'online', 'manual'))
);

create table waitlist_entries (
    waitlist_entry_id uuid primary key,
    branch_id uuid not null references branches(branch_id),
    customer_profile_id uuid references customer_profiles(customer_profile_id),
    seating_preference_id uuid references seating_preferences(seating_preference_id),
    contact_name varchar(150) not null,
    contact_phone varchar(50) not null,
    contact_email varchar(150),
    notification_channel varchar(30) not null,
    party_size integer not null,
    quoted_wait_min integer not null,
    status varchar(30) not null,
    notes varchar(500),
    priority integer not null default 0,
    added_at timestamptz not null,
    hold_expires_at timestamptz,
    notified_at timestamptz,
    seated_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (party_size > 0),
    check (quoted_wait_min >= 0),
    check (status in ('waiting', 'notified', 'seated', 'left', 'expired')),
    check (notification_channel in ('sms', 'email', 'phone', 'in_app'))
);

create table table_sessions (
    session_id uuid primary key,
    table_id uuid not null references dining_tables(table_id),
    reservation_id uuid references reservations(reservation_id),
    server_user_id uuid not null references users(user_id),
    guest_count integer not null,
    status varchar(30) not null,
    correlation_id varchar(100) not null,
    opened_at timestamptz not null,
    closed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (guest_count > 0),
    check (status in ('active', 'billing', 'closed'))
);

create table table_assignments (
    assignment_id uuid primary key,
    reservation_id uuid references reservations(reservation_id),
    waitlist_entry_id uuid references waitlist_entries(waitlist_entry_id),
    table_id uuid not null references dining_tables(table_id),
    table_session_id uuid references table_sessions(session_id),
    assigned_at timestamptz not null,
    reason varchar(50) not null,
    score numeric(8,2) not null,
    released_at timestamptz,
    released_reason varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (reason in ('reservation_match', 'walk_in', 'reassignment', 'waitlist_match', 'manager_override')),
    check (score >= 0),
    check ((reservation_id is not null) or (waitlist_entry_id is not null))
);

create table menu_categories (
    category_id uuid primary key,
    name varchar(120) not null unique,
    display_order integer not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (display_order >= 0)
);

create table menu_items (
    menu_item_id uuid primary key,
    category_id uuid not null references menu_categories(category_id),
    name varchar(150) not null unique,
    description varchar(500) not null,
    base_price numeric(12,2) not null,
    station varchar(30) not null,
    availability varchar(30) not null,
    sale_status varchar(30) not null,
    preparation_time_min integer not null,
    allergens_json jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (base_price >= 0),
    check (preparation_time_min >= 0),
    check (station in ('grill', 'fryer', 'dessert', 'drinks', 'salad', 'prep')),
    check (availability in ('available', 'unavailable', 'scheduled')),
    check (sale_status in ('active', 'hidden', 'seasonal'))
);

create table modifier_groups (
    group_id uuid primary key,
    menu_item_id uuid not null references menu_items(menu_item_id) on delete cascade,
    name varchar(120) not null,
    min_select integer not null,
    max_select integer not null,
    required boolean not null,
    multi_select boolean not null,
    display_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (min_select >= 0),
    check (max_select >= 0),
    check (max_select >= min_select),
    unique (menu_item_id, name)
);

create table modifier_options (
    option_id uuid primary key,
    group_id uuid not null references modifier_groups(group_id) on delete cascade,
    name varchar(120) not null,
    extra_price numeric(12,2) not null,
    active boolean not null,
    display_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (extra_price >= 0),
    unique (group_id, name)
);

create table availability_rules (
    rule_id uuid primary key,
    menu_item_id uuid not null references menu_items(menu_item_id) on delete cascade,
    effective_from timestamptz,
    effective_to timestamptz,
    out_of_stock_hides_item boolean not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table promotion_campaigns (
    promotion_campaign_id uuid primary key,
    code varchar(80) not null unique,
    name varchar(150) not null,
    discount_type varchar(30) not null,
    discount_value numeric(12,2) not null,
    effective_from timestamptz,
    effective_to timestamptz,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (discount_type in ('amount', 'percentage')),
    check (discount_value >= 0)
);

create table orders (
    order_id uuid primary key,
    table_session_id uuid not null references table_sessions(session_id),
    channel varchar(30) not null,
    status varchar(30) not null,
    server_user_id uuid not null references users(user_id),
    special_instructions varchar(500),
    correlation_id varchar(100) not null,
    created_at timestamptz not null,
    confirmed_at timestamptz,
    updated_at timestamptz not null default now(),
    check (channel in ('dine_in', 'takeaway', 'manual')),
    check (status in ('draft', 'confirmed', 'in_progress', 'ready', 'served', 'cancelled'))
);

create table order_snapshots (
    snapshot_id uuid primary key,
    order_id uuid not null unique references orders(order_id) on delete cascade,
    menu_version varchar(80) not null,
    priced_at timestamptz not null,
    currency varchar(20) not null,
    subtotal numeric(12,2) not null,
    created_at timestamptz not null default now(),
    check (subtotal >= 0)
);

create table order_items (
    order_item_id uuid primary key,
    order_id uuid not null references orders(order_id) on delete cascade,
    menu_item_id uuid not null references menu_items(menu_item_id),
    snapshot_name varchar(150) not null,
    quantity integer not null,
    unit_price numeric(12,2) not null,
    special_instruction varchar(500),
    allergy_notes varchar(500),
    line_status varchar(30) not null,
    fire_at timestamptz,
    sent_to_kitchen_at timestamptz,
    served_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (quantity > 0),
    check (unit_price >= 0),
    check (line_status in ('pending', 'sent_to_kitchen', 'cooking', 'ready', 'served', 'cancelled', 'hold_for_service', 'blocked'))
);

create table order_item_modifiers (
    selection_id uuid primary key,
    order_item_id uuid not null references order_items(order_item_id) on delete cascade,
    modifier_option_id uuid references modifier_options(option_id),
    name_snapshot varchar(150) not null,
    extra_price numeric(12,2) not null,
    qty_multiplier integer not null default 1,
    created_at timestamptz not null default now(),
    check (extra_price >= 0),
    check (qty_multiplier > 0)
);

create table kitchen_stations (
    station_id uuid primary key,
    name varchar(120) not null unique,
    kind varchar(30) not null,
    max_parallel_tickets integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (kind in ('grill', 'fryer', 'dessert', 'drinks', 'salad', 'prep')),
    check (max_parallel_tickets > 0)
);

create table route_plans (
    route_plan_id uuid primary key,
    created_from_order_id uuid not null references orders(order_id),
    notes varchar(500),
    created_at timestamptz not null default now()
);

create table kitchen_tickets (
    ticket_id uuid primary key,
    order_id uuid not null references orders(order_id),
    station_id uuid not null references kitchen_stations(station_id),
    route_plan_id uuid references route_plans(route_plan_id),
    priority integer not null,
    priority_label varchar(30) not null,
    status varchar(30) not null,
    blocked_reason varchar(255),
    created_at timestamptz not null,
    started_at timestamptz,
    ready_at timestamptz,
    completed_at timestamptz,
    served_at timestamptz,
    target_service_at timestamptz,
    expedited_at timestamptz,
    created_at_record timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (priority >= 0),
    check (priority_label in ('normal', 'rush', 'expedite')),
    check (status in ('new', 'started', 'cooking', 'ready', 'blocked', 'hold_for_service', 'served'))
);

create table kitchen_ticket_items (
    ticket_item_id uuid primary key,
    ticket_id uuid not null references kitchen_tickets(ticket_id) on delete cascade,
    order_item_id uuid not null references order_items(order_item_id),
    quantity integer not null,
    status varchar(30) not null,
    fire_at timestamptz,
    hold_reason varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (quantity > 0),
    check (status in ('new', 'started', 'cooking', 'ready', 'blocked', 'hold_for_service', 'served'))
);

create table serve_handoffs (
    handoff_id uuid primary key,
    ticket_id uuid not null unique references kitchen_tickets(ticket_id),
    server_id uuid not null references users(user_id),
    handed_off_at timestamptz not null,
    accepted_at timestamptz,
    delivered_at timestamptz,
    returned_at timestamptz,
    return_reason varchar(255),
    created_at timestamptz not null default now()
);

create table dish_status_events (
    event_id uuid primary key,
    ticket_item_id uuid references kitchen_ticket_items(ticket_item_id),
    handoff_id uuid references serve_handoffs(handoff_id),
    type varchar(30) not null,
    source_station varchar(120),
    details jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null,
    created_at timestamptz not null default now(),
    check (type in ('queued', 'started', 'cooking', 'ready', 'blocked', 'served', 'returned'))
);

create table inventory_items (
    inventory_item_id uuid primary key,
    branch_id uuid not null references branches(branch_id),
    name varchar(150) not null unique,
    unit varchar(40) not null,
    on_hand numeric(12,2) not null,
    reserved_qty numeric(12,2) not null default 0,
    threshold numeric(12,2) not null,
    minimum_stock numeric(12,2) not null,
    maximum_stock numeric(12,2) not null,
    cost_per_unit numeric(12,2) not null,
    category varchar(120) not null,
    preferred_vendor varchar(150),
    last_restocked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (threshold >= 0),
    check (minimum_stock >= 0),
    check (maximum_stock >= minimum_stock),
    check (cost_per_unit >= 0)
);

create table recipes (
    recipe_id uuid primary key,
    menu_item_id uuid not null references menu_items(menu_item_id),
    version integer not null,
    yield_unit varchar(40) not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    retired_at timestamptz,
    unique (menu_item_id, version),
    check (version > 0)
);

create table recipe_lines (
    recipe_line_id uuid primary key,
    recipe_id uuid not null references recipes(recipe_id) on delete cascade,
    inventory_item_id uuid not null references inventory_items(inventory_item_id),
    required_qty numeric(12,2) not null,
    waste_factor numeric(5,2) not null,
    created_at timestamptz not null default now(),
    check (required_qty > 0),
    check (waste_factor >= 0)
);

create table inventory_reservations (
    reservation_id uuid primary key,
    inventory_item_id uuid not null references inventory_items(inventory_item_id),
    source_order_id uuid not null references orders(order_id),
    qty numeric(12,2) not null,
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    finalized_at timestamptz,
    cancelled_at timestamptz,
    check (qty > 0),
    check (status in ('held', 'finalized', 'cancelled'))
);

create table stock_transactions (
    transaction_id uuid primary key,
    inventory_item_id uuid not null references inventory_items(inventory_item_id),
    delta numeric(12,2) not null,
    reason varchar(30) not null,
    source_ref uuid,
    source_type varchar(50),
    previous_on_hand numeric(12,2) not null,
    new_on_hand numeric(12,2) not null,
    performed_by_user_id uuid references users(user_id),
    correlation_id varchar(100),
    occurred_at timestamptz not null,
    created_at timestamptz not null default now(),
    check (reason in ('consumption', 'restock', 'manual_adjustment', 'waste', 'return'))
);

create table reorder_rules (
    rule_id uuid primary key,
    inventory_item_id uuid not null unique references inventory_items(inventory_item_id),
    threshold numeric(12,2) not null,
    preferred_vendor varchar(150),
    target_qty numeric(12,2) not null,
    lead_time_days integer not null,
    is_active boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (threshold >= 0),
    check (target_qty >= 0),
    check (lead_time_days >= 0)
);

create table low_stock_alerts (
    alert_id uuid primary key,
    inventory_item_id uuid not null references inventory_items(inventory_item_id),
    severity varchar(30) not null,
    status varchar(30) not null,
    created_at timestamptz not null,
    acknowledged_by uuid references users(user_id),
    acknowledged_at timestamptz,
    snoozed_until timestamptz,
    updated_at timestamptz not null default now(),
    check (severity in ('low', 'medium', 'high', 'critical')),
    check (status in ('open', 'acknowledged', 'snoozed'))
);

create table bills (
    bill_id uuid primary key,
    table_session_id uuid not null unique references table_sessions(session_id),
    promotion_campaign_id uuid references promotion_campaigns(promotion_campaign_id),
    status varchar(30) not null,
    sub_total numeric(12,2) not null,
    tax_amount numeric(12,2) not null,
    service_fee numeric(12,2) not null,
    tip_amount numeric(12,2) not null,
    grand_total numeric(12,2) not null,
    discount_total numeric(12,2) not null default 0,
    promotion_code varchar(80),
    correlation_id varchar(100) not null,
    created_at timestamptz not null,
    finalized_at timestamptz,
    closed_at timestamptz,
    updated_at timestamptz not null default now(),
    check (status in ('open', 'finalized', 'paid', 'partially_paid', 'refunded', 'void')),
    check (sub_total >= 0),
    check (tax_amount >= 0),
    check (service_fee >= 0),
    check (tip_amount >= 0),
    check (grand_total >= 0),
    check (discount_total >= 0)
);

create table bill_lines (
    bill_line_id uuid primary key,
    bill_id uuid not null references bills(bill_id) on delete cascade,
    source_order_item_id uuid not null references order_items(order_item_id),
    label varchar(180) not null,
    quantity integer not null,
    line_total numeric(12,2) not null,
    created_at timestamptz not null default now(),
    check (quantity > 0),
    check (line_total >= 0)
);

create table bill_splits (
    split_id uuid primary key,
    bill_id uuid not null references bills(bill_id) on delete cascade,
    label varchar(120) not null,
    status varchar(30) not null,
    allocated_total numeric(12,2) not null,
    tip_amount numeric(12,2) not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (status in ('pending', 'paid')),
    check (allocated_total >= 0),
    check (tip_amount >= 0)
);

create table split_allocations (
    allocation_id uuid primary key,
    split_id uuid not null references bill_splits(split_id) on delete cascade,
    bill_line_id uuid not null references bill_lines(bill_line_id) on delete cascade,
    amount numeric(12,2) not null,
    ratio numeric(8,4) not null,
    created_at timestamptz not null default now(),
    check (amount >= 0),
    check (ratio >= 0)
);

create table applied_discounts (
    discount_id uuid primary key,
    bill_id uuid not null references bills(bill_id) on delete cascade,
    bill_line_id uuid references bill_lines(bill_line_id),
    type varchar(30) not null,
    value numeric(12,2) not null,
    reason varchar(255),
    created_at timestamptz not null default now(),
    check (type in ('amount', 'percentage', 'promotion', 'override')),
    check (value >= 0)
);

create table payments (
    payment_id uuid primary key,
    bill_id uuid not null references bills(bill_id),
    split_id uuid references bill_splits(split_id),
    method varchar(30) not null,
    amount numeric(12,2) not null,
    status varchar(30) not null,
    gateway_ref varchar(150),
    cash_received numeric(12,2),
    change_due numeric(12,2),
    processed_by_user_id uuid not null references users(user_id),
    paid_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (method in ('cash', 'credit_card', 'debit_card', 'mobile_payment', 'gift_card')),
    check (status in ('pending', 'completed', 'failed', 'refunded')),
    check (amount >= 0)
);

create table refunds (
    refund_id uuid primary key,
    payment_id uuid not null references payments(payment_id),
    bill_id uuid not null references bills(bill_id),
    amount numeric(12,2) not null,
    reason varchar(255) not null,
    status varchar(30) not null,
    requested_by uuid not null references users(user_id),
    approved_by uuid references users(user_id),
    processed_at timestamptz,
    external_transaction_ref varchar(150),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (status in ('requested', 'approved', 'completed', 'rejected', 'pending_review')),
    check (amount >= 0)
);

create table receipts (
    receipt_id uuid primary key,
    payment_id uuid not null unique references payments(payment_id),
    bill_id uuid not null references bills(bill_id),
    issued_at timestamptz not null,
    delivery_channel varchar(30) not null,
    document_no varchar(80) not null unique,
    recipient_address varchar(180),
    created_at timestamptz not null default now(),
    check (delivery_channel in ('print', 'email', 'sms'))
);

create table notification_messages (
    message_id uuid primary key,
    low_stock_alert_id uuid references low_stock_alerts(alert_id),
    reservation_id uuid references reservations(reservation_id),
    payment_id uuid references payments(payment_id),
    channel varchar(30) not null,
    type varchar(40) not null,
    template_code varchar(80) not null,
    payload jsonb not null default '{}'::jsonb,
    title varchar(180) not null,
    body text not null,
    recipient_role varchar(50),
    recipient_user_id uuid references users(user_id),
    recipient_address varchar(180),
    priority varchar(30) not null,
    status varchar(30) not null,
    created_at timestamptz not null,
    queued_at timestamptz,
    sent_at timestamptz,
    read_at timestamptz,
    failure_reason varchar(255),
    check (channel in ('sms', 'email', 'phone', 'in_app', 'print')),
    check (priority in ('low', 'medium', 'high', 'critical')),
    check (status in ('queued', 'sent', 'failed', 'read', 'pending_retry'))
);

create table audit_logs (
    audit_log_id uuid primary key,
    actor_user_id uuid not null references users(user_id),
    action varchar(120) not null,
    entity_type varchar(80) not null,
    entity_id varchar(120) not null,
    recorded_at timestamptz not null,
    correlation_id varchar(100) not null,
    reason varchar(255),
    follow_up boolean not null default false,
    ip_address varchar(100),
    before_payload jsonb,
    after_payload jsonb,
    created_at timestamptz not null default now()
);

create table audit_details (
    detail_id uuid primary key,
    audit_log_id uuid not null references audit_logs(audit_log_id) on delete cascade,
    field_name varchar(120) not null,
    before_value text,
    after_value text,
    created_at timestamptz not null default now()
);

create table shift_assignments (
    shift_assignment_id uuid primary key,
    user_id uuid not null references users(user_id),
    branch_id uuid not null references branches(branch_id),
    shift_date date not null,
    start_at time not null,
    end_at time not null,
    position varchar(80) not null,
    zone varchar(80),
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (status in ('scheduled', 'completed', 'cancelled'))
);

create table report_queries (
    query_id uuid primary key,
    created_by_user_id uuid references users(user_id),
    filters jsonb not null default '{}'::jsonb,
    group_by varchar(80),
    measure varchar(80),
    status varchar(30) not null,
    requested_format varchar(20) not null default 'json',
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    check (status in ('queued', 'running', 'completed', 'failed')),
    check (requested_format in ('json', 'csv', 'pdf'))
);

create table report_snapshots (
    snapshot_id uuid primary key,
    branch_id uuid references branches(branch_id),
    type varchar(50) not null,
    period_start timestamptz not null,
    period_end timestamptz not null,
    generated_at timestamptz not null,
    payload jsonb not null default '{}'::jsonb,
    query_id uuid references report_queries(query_id),
    created_at timestamptz not null default now(),
    check (type in ('operations', 'revenue', 'kitchen', 'inventory', 'labor'))
);

create table outbox_events (
    event_id uuid primary key,
    type varchar(120) not null,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(120) not null,
    payload jsonb not null default '{}'::jsonb,
    correlation_id varchar(100) not null,
    status varchar(30) not null,
    retry_count integer not null default 0,
    occurred_at timestamptz not null,
    processed_at timestamptz,
    last_error varchar(255),
    created_at timestamptz not null default now(),
    check (status in ('pending', 'processed', 'failed'))
);

create index idx_user_sessions_user on user_sessions(user_id);
create index idx_user_sessions_status on user_sessions(status, expires_at);
create index idx_reservations_arrival on reservations(arrival_at);
create index idx_reservations_status on reservations(status);
create index idx_waitlist_status on waitlist_entries(status, added_at);
create index idx_table_sessions_status on table_sessions(status, opened_at);
create index idx_orders_status on orders(status, created_at);
create index idx_order_items_status on order_items(line_status);
create index idx_kitchen_tickets_status on kitchen_tickets(status, priority desc);
create index idx_inventory_items_threshold on inventory_items(threshold, on_hand);
create index idx_stock_transactions_inventory on stock_transactions(inventory_item_id, occurred_at desc);
create index idx_low_stock_alerts_status on low_stock_alerts(status, created_at desc);
create index idx_bills_status on bills(status, created_at desc);
create index idx_payments_bill on payments(bill_id, paid_at desc);
create index idx_refunds_payment on refunds(payment_id, created_at desc);
create index idx_notifications_status on notification_messages(status, priority, created_at desc);
create index idx_audit_logs_lookup on audit_logs(recorded_at desc, actor_user_id, entity_type);
create index idx_audit_logs_correlation on audit_logs(correlation_id);
create index idx_shift_assignments_date on shift_assignments(shift_date, user_id);
create index idx_report_snapshots_type on report_snapshots(type, generated_at desc);
create index idx_outbox_events_status on outbox_events(status, occurred_at);
