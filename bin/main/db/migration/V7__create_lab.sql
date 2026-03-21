-- V7: Laboratory

CREATE TABLE laboratory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    integration_type VARCHAR(100),
    config JSONB,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE lab_test (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12,2),
    laboratory_id UUID REFERENCES laboratory(id),
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE lab_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_record_id UUID NOT NULL REFERENCES medical_record(id),
    patient_id UUID NOT NULL REFERENCES patient(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    total_price DECIMAL(12,2),
    status lab_order_status NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE lab_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_order_id UUID NOT NULL REFERENCES lab_order(id),
    lab_test_id UUID NOT NULL REFERENCES lab_test(id),
    price DECIMAL(12,2)
);

CREATE TABLE lab_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_order_item_id UUID NOT NULL REFERENCES lab_order_item(id),
    result_data TEXT,
    source lab_result_source NOT NULL,
    received_at TIMESTAMP,
    is_sent_to_gov BOOLEAN DEFAULT false,
    sent_to_gov_at TIMESTAMP,
    sent_to_gov_by UUID REFERENCES employee(id)
);
