ALTER TABLE employee
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Built-in sysadmin must rotate password on first login after rollout.
UPDATE employee
SET must_change_password = TRUE,
    updated_at = NOW()
WHERE email = 'sysadmin@mis.local';
