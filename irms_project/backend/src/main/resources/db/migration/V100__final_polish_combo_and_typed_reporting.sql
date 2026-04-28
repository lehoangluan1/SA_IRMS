create table if not exists menu_combos (
    combo_id uuid primary key,
    name varchar(255) not null,
    description text,
    combo_price numeric(12,2) not null,
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp
);

create table if not exists menu_combo_groups (
    combo_group_id uuid primary key,
    combo_id uuid not null references menu_combos(combo_id) on delete cascade,
    name varchar(255) not null,
    min_selections integer not null default 0,
    max_selections integer not null default 1,
    required boolean not null default false,
    display_order integer not null default 1,
    created_at timestamp not null default now()
);

create table if not exists menu_combo_options (
    combo_option_id uuid primary key,
    combo_group_id uuid not null references menu_combo_groups(combo_group_id) on delete cascade,
    menu_item_id uuid not null,
    extra_price numeric(12,2) not null default 0,
    active boolean not null default true,
    created_at timestamp not null default now()
);

create table if not exists order_combo_selections (
    combo_selection_id uuid primary key,
    order_id uuid not null,
    combo_id uuid not null references menu_combos(combo_id),
    combo_name varchar(255) not null,
    quantity integer not null,
    combo_price numeric(12,2) not null,
    allergy_notes text,
    special_instructions text,
    created_at timestamp not null default now()
);

create table if not exists order_combo_selection_items (
    combo_selection_item_id uuid primary key,
    combo_selection_id uuid not null references order_combo_selections(combo_selection_id) on delete cascade,
    combo_group_id uuid not null,
    combo_option_id uuid not null,
    menu_item_id uuid not null,
    menu_item_name varchar(255) not null,
    station varchar(100),
    quantity integer not null,
    allocated_unit_price numeric(12,2) not null,
    created_at timestamp not null default now()
);

create index if not exists idx_order_combo_selection_order on order_combo_selections(order_id);
create index if not exists idx_order_combo_selection_combo on order_combo_selections(combo_id);
create index if not exists idx_order_combo_selection_item_selection on order_combo_selection_items(combo_selection_id);

create table if not exists reporting_sales_projection (
    business_date date primary key,
    order_count bigint not null default 0,
    bill_count bigint not null default 0,
    gross_sales numeric(14,2) not null default 0,
    refund_total numeric(14,2) not null default 0,
    net_sales numeric(14,2) not null default 0,
    updated_at timestamp not null default now()
);

create table if not exists reporting_peak_hour_projection (
    business_date date not null,
    hour_of_day integer not null,
    order_count bigint not null default 0,
    revenue_total numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, hour_of_day)
);

create table if not exists reporting_best_selling_item_projection (
    business_date date not null,
    menu_item_id uuid not null,
    item_name varchar(255) not null,
    quantity_sold bigint not null default 0,
    revenue_total numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, menu_item_id)
);

create table if not exists reporting_revenue_projection (
    business_date date not null,
    payment_method varchar(100) not null,
    gross_revenue numeric(14,2) not null default 0,
    discounts numeric(14,2) not null default 0,
    refunds numeric(14,2) not null default 0,
    net_revenue numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, payment_method)
);

create table if not exists reporting_kitchen_bottleneck_projection (
    business_date date not null,
    station varchar(100) not null,
    delayed_item_count bigint not null default 0,
    average_delay_minutes numeric(10,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, station)
);

create table if not exists reporting_staff_efficiency_projection (
    business_date date not null,
    staff_id varchar(64) not null,
    role varchar(100) not null,
    completed_tasks bigint not null default 0,
    average_service_time numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, staff_id)
);

create table if not exists reporting_inventory_usage_projection (
    business_date date not null,
    ingredient_id varchar(64) not null,
    ingredient_name varchar(255) not null,
    quantity_used numeric(14,2) not null default 0,
    waste_quantity numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, ingredient_id)
);

create table if not exists reporting_combo_sales_projection (
    business_date date not null,
    combo_id varchar(64) not null,
    combo_name varchar(255) not null,
    quantity_sold bigint not null default 0,
    revenue_total numeric(14,2) not null default 0,
    updated_at timestamp not null default now(),
    primary key (business_date, combo_id)
);
