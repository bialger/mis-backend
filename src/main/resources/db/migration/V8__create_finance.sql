-- V8: Финансы

CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointment(id),
    amount DECIMAL(12,2) NOT NULL,
    payment_method payment_method_type NOT NULL,
    payment_status payment_status_type NOT NULL,
    notes TEXT,
    created_by UUID NOT NULL REFERENCES employee(id),
    created_at TIMESTAMP
);

CREATE TABLE salary_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    period_start DATE,
    period_end DATE,
    hours_worked DECIMAL(6,2),
    shifts_count INT,
    amount DECIMAL(12,2),
    created_at TIMESTAMP
);
