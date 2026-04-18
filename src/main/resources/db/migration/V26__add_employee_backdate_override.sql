ALTER TABLE employee
    ADD COLUMN IF NOT EXISTS backdate_days_override INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_employee_backdate_days_override_non_negative'
    ) THEN
        ALTER TABLE employee
            ADD CONSTRAINT ck_employee_backdate_days_override_non_negative
                CHECK (backdate_days_override IS NULL OR backdate_days_override >= 0);
    END IF;
END
$$;
