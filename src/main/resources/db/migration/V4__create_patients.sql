-- Пациенты: Patient, PatientTagType, PatientTag, PatientConsent

CREATE TABLE patient (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_number VARCHAR(50) NOT NULL UNIQUE,
    organization_id UUID NOT NULL REFERENCES organization(id),
    full_name VARCHAR(255) NOT NULL,
    gender gender_type,
    birth_date DATE,
    registration_address TEXT,
    residence_address TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    locality_type locality_type,
    citizenship VARCHAR(100),
    identity_document VARCHAR(255),
    oms_policy VARCHAR(50),
    snils VARCHAR(20),
    insurance_organization VARCHAR(255),
    contact_person VARCHAR(255),
    guardian VARCHAR(255),
    profession VARCHAR(255),
    workplace VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE patient_tag_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(10),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE patient_tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    tag_type_id UUID NOT NULL REFERENCES patient_tag_type(id),
    created_by UUID REFERENCES employee(id),
    created_at TIMESTAMP
);

CREATE TABLE patient_consent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    consent_type consent_type NOT NULL,
    is_granted BOOLEAN NOT NULL,
    granted_at TIMESTAMP,
    revoked_at TIMESTAMP
);
