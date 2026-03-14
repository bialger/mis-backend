-- V9: Файлы и вложения

CREATE TABLE attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    appointment_id UUID REFERENCES appointment(id),
    medical_record_id UUID REFERENCES medical_record(id),
    file_name VARCHAR(255) NOT NULL,
    file_type file_type NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    description TEXT,
    uploaded_by UUID NOT NULL REFERENCES employee(id),
    uploaded_at TIMESTAMP
);
