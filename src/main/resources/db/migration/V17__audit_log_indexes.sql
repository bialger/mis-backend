-- V17: Indexes for audit_log to support efficient timestamp-based cleanup and entity filtering

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp
    ON audit_log (timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_log_entity_type_id
    ON audit_log (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_employee
    ON audit_log (employee_id, timestamp DESC);
