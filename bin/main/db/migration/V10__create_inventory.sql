-- V10: Inventory

CREATE TABLE inventory_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE inventory_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES inventory_category(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    room_id UUID REFERENCES room(id),
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    min_quantity DECIMAL(10,2),
    cost_price DECIMAL(12,2)
);

CREATE TABLE inventory_operation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES inventory_item(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    operation_type inventory_operation_type NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    appointment_id UUID REFERENCES appointment(id),
    notes TEXT,
    created_at TIMESTAMP
);

CREATE TABLE inventory_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employee(id),
    role_id UUID REFERENCES role(id),
    category_id UUID NOT NULL REFERENCES inventory_category(id)
);

CREATE TABLE service_inventory_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES service(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_item(id),
    quantity DECIMAL(10,2) NOT NULL
);
