-- V11: Notifications and integrations

CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient(id),
    appointment_id UUID REFERENCES appointment(id),
    channel notification_channel NOT NULL,
    type notification_type NOT NULL,
    content TEXT,
    status notification_status NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE integration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type integration_type NOT NULL,
    name VARCHAR(255) NOT NULL,
    config JSONB,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP
);
