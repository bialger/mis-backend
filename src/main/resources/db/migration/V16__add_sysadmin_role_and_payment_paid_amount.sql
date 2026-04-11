-- V16: Add SYSADMIN role; add paid_amount to payment for exact partial-payment tracking

-- 1. SYSADMIN role (system administrator: manages orgs, branches, accounts, permissions)
INSERT INTO role (id, name, display_name, description)
SELECT gen_random_uuid(), 'SYSADMIN', 'Системный администратор',
       'Полный доступ: управление организациями, филиалами, учётными записями и разрешениями'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'SYSADMIN');

-- 2. Re-describe ADMIN as clinic manager (not system admin)
UPDATE role
SET display_name = 'Менеджер клиники',
    description  = 'Управление расписанием, пациентами и финансами в рамках клиники'
WHERE name = 'ADMIN'
  AND display_name = 'Администратор';

-- 3. paid_amount in payment — exact amount paid (NULL = not recorded separately)
ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(12, 2);
