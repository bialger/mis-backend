-- V6: Medical records

CREATE TABLE template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type template_type NOT NULL,
    specialty_id UUID REFERENCES specialty(id),
    employee_id UUID REFERENCES employee(id),
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE medical_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointment(id),
    patient_id UUID NOT NULL REFERENCES patient(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    complaints TEXT,
    anamnesis TEXT,
    examination_results TEXT,
    disease_course TEXT,
    procedures TEXT,
    epicrisis TEXT,
    template_id UUID REFERENCES template(id),
    is_signed BOOLEAN DEFAULT false,
    signature_data BYTEA,
    signed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE diagnosis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_record_id UUID NOT NULL REFERENCES medical_record(id),
    code VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_primary BOOLEAN DEFAULT false
);

CREATE TABLE prescription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_record_id UUID NOT NULL REFERENCES medical_record(id),
    type prescription_type NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE insert_sheet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    appointment_id UUID NOT NULL REFERENCES appointment(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    form_type VARCHAR(100) NOT NULL,
    content TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
