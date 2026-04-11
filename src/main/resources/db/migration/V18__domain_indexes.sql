-- V18: Indexes for high-volume domain tables
-- Covered: appointment, medical_record, patient, payment, time_slot, lab_order,
--          inventory_operation, notification, attachment, prescription, diagnosis,
--          employee_role, patient_tag, salary_record

-- ============================================================
-- APPOINTMENT (central table, highest row volume)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_appointment_patient
    ON appointment (patient_id);

CREATE INDEX IF NOT EXISTS idx_appointment_employee
    ON appointment (employee_id);

CREATE INDEX IF NOT EXISTS idx_appointment_branch
    ON appointment (branch_id);

CREATE INDEX IF NOT EXISTS idx_appointment_status
    ON appointment (status);

CREATE INDEX IF NOT EXISTS idx_appointment_created_at
    ON appointment (created_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_appointment_time_slot
    ON appointment (time_slot_id)
    WHERE time_slot_id IS NOT NULL;

-- ============================================================
-- MEDICAL RECORD (one per appointment, grows with appointments)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_medical_record_patient
    ON medical_record (patient_id);

CREATE INDEX IF NOT EXISTS idx_medical_record_appointment
    ON medical_record (appointment_id);

CREATE INDEX IF NOT EXISTS idx_medical_record_employee
    ON medical_record (employee_id);

CREATE INDEX IF NOT EXISTS idx_medical_record_updated
    ON medical_record (updated_at DESC NULLS LAST);

-- ============================================================
-- PATIENT (grows steadily; frequent lookup by name and org)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_patient_organization
    ON patient (organization_id);

CREATE INDEX IF NOT EXISTS idx_patient_full_name
    ON patient (full_name);

-- ============================================================
-- PAYMENT (linked to appointment; used in reports and queries)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_payment_appointment
    ON payment (appointment_id);

CREATE INDEX IF NOT EXISTS idx_payment_status
    ON payment (payment_status);

CREATE INDEX IF NOT EXISTS idx_payment_created_at
    ON payment (created_at DESC NULLS LAST);

-- ============================================================
-- TIME SLOT (fast lookup for available slots by date/doctor)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_time_slot_employee_date
    ON time_slot (employee_id, slot_date);

CREATE INDEX IF NOT EXISTS idx_time_slot_branch
    ON time_slot (branch_id);

CREATE INDEX IF NOT EXISTS idx_time_slot_available_date
    ON time_slot (slot_date, is_available)
    WHERE is_available = true;

-- ============================================================
-- LAB ORDER / ITEM (grows with lab test prescriptions)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_lab_order_medical_record
    ON lab_order (medical_record_id);

CREATE INDEX IF NOT EXISTS idx_lab_order_patient
    ON lab_order (patient_id);

CREATE INDEX IF NOT EXISTS idx_lab_order_employee
    ON lab_order (employee_id);

CREATE INDEX IF NOT EXISTS idx_lab_order_item_order
    ON lab_order_item (lab_order_id);

-- ============================================================
-- INVENTORY OPERATION (warehouse movement journal)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_inventory_operation_item_date
    ON inventory_operation (item_id, created_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_inventory_operation_employee
    ON inventory_operation (employee_id);

CREATE INDEX IF NOT EXISTS idx_inventory_operation_appointment
    ON inventory_operation (appointment_id)
    WHERE appointment_id IS NOT NULL;

-- ============================================================
-- NOTIFICATION (high volume during bulk sends)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_notification_patient
    ON notification (patient_id);

CREATE INDEX IF NOT EXISTS idx_notification_appointment
    ON notification (appointment_id)
    WHERE appointment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_status
    ON notification (status);

CREATE INDEX IF NOT EXISTS idx_notification_created_at
    ON notification (created_at DESC NULLS LAST);

-- ============================================================
-- ATTACHMENT (patient and appointment files)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_attachment_patient
    ON attachment (patient_id);

CREATE INDEX IF NOT EXISTS idx_attachment_appointment
    ON attachment (appointment_id)
    WHERE appointment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_attachment_medical_record
    ON attachment (medical_record_id)
    WHERE medical_record_id IS NOT NULL;

-- ============================================================
-- PRESCRIPTION / DIAGNOSIS (nested inside medical record)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_prescription_medical_record
    ON prescription (medical_record_id);

CREATE INDEX IF NOT EXISTS idx_diagnosis_medical_record
    ON diagnosis (medical_record_id);

-- ============================================================
-- EMPLOYEE_ROLE junction (fast role lookup per employee)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_employee_role_employee
    ON employee_role (employee_id);

-- ============================================================
-- PATIENT TAG (icon/badge lookup per patient)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_patient_tag_patient
    ON patient_tag (patient_id);

-- ============================================================
-- SALARY RECORD (period-based payroll reports)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_salary_record_employee
    ON salary_record (employee_id);

CREATE INDEX IF NOT EXISTS idx_salary_record_period
    ON salary_record (period_start DESC NULLS LAST, period_end DESC NULLS LAST);
