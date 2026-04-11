-- Convert all TIMESTAMP (without time zone) columns to TIMESTAMPTZ.
-- Existing values are interpreted as UTC (consistent with how java.time.Instant is stored by the JDBC driver).
-- This migration is idempotent: columns already of type TIMESTAMPTZ are left unchanged.
--
-- Note: crm_post already uses TIMESTAMPTZ (V13) and is not listed here.

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        VALUES
            ('organization',       'created_at'),
            ('branch',             'created_at'),
            ('employee',           'created_at'),
            ('employee',           'updated_at'),
            ('patient',            'created_at'),
            ('patient',            'updated_at'),
            ('patient_tag',        'created_at'),
            ('patient_consent',    'granted_at'),
            ('patient_consent',    'revoked_at'),
            ('appointment',        'created_at'),
            ('appointment',        'updated_at'),
            ('template',           'created_at'),
            ('template',           'updated_at'),
            ('medical_record',     'signed_at'),
            ('medical_record',     'created_at'),
            ('medical_record',     'updated_at'),
            ('prescription',       'created_at'),
            ('insert_sheet',       'created_at'),
            ('insert_sheet',       'updated_at'),
            ('lab_order',          'created_at'),
            ('lab_result',         'received_at'),
            ('lab_result',         'sent_to_gov_at'),
            ('payment',            'created_at'),
            ('salary_record',      'created_at'),
            ('attachment',         'uploaded_at'),
            ('inventory_operation','created_at'),
            ('notification',       'sent_at'),
            ('notification',       'created_at'),
            ('integration',        'created_at'),
            ('audit_log',          'timestamp')
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = r.column1
              AND column_name  = r.column2
              AND data_type    = 'timestamp without time zone'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMPTZ USING %I AT TIME ZONE ''UTC''',
                r.column1, r.column2, r.column2
            );
        END IF;
    END LOOP;
END;
$$;
