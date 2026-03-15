-- V5: Расписание и визиты, услуги

CREATE TABLE service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    cost_price DECIMAL(12,2),
    branch_id UUID REFERENCES branch(id),
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE time_slot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id),
    room_id UUID NOT NULL REFERENCES room(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT true
);

CREATE TABLE appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    time_slot_id UUID REFERENCES time_slot(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    room_id UUID NOT NULL REFERENCES room(id),
    status appointment_status NOT NULL,
    source appointment_source NOT NULL,
    notes TEXT,
    created_by UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE appointment_service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointment(id),
    service_id UUID NOT NULL REFERENCES service(id),
    quantity INT DEFAULT 1,
    price DECIMAL(12,2)
);
