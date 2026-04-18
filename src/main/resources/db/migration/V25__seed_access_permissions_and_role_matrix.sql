-- Seed the canonical permission catalog and role matrix for account-level ACL.
-- Also enforce one override row per (employee, permission).

-- 1) Canonical permission list.
INSERT INTO permission (id, code, name, description)
VALUES
    (gen_random_uuid(), 'dashboard.view', 'Просмотр панели', 'Доступ к /'),
    (gen_random_uuid(), 'posts.view', 'Просмотр постов', 'Доступ к /posts'),
    (gen_random_uuid(), 'patients.view', 'Пациенты', 'Просмотр и работа с пациентами'),
    (gen_random_uuid(), 'doctors.view', 'Врачи', 'Просмотр раздела врачей'),
    (gen_random_uuid(), 'schedule.view', 'Расписание', 'Просмотр и работа с расписанием'),
    (gen_random_uuid(), 'appointments.view', 'Записи', 'Просмотр и работа с записями'),
    (gen_random_uuid(), 'reports.view', 'Отчеты', 'Доступ к отчетам'),
    (gen_random_uuid(), 'inventory.view', 'Склад: просмотр', 'Просмотр остатков склада'),
    (gen_random_uuid(), 'inventory.write', 'Склад: изменение', 'Операции изменения склада'),
    (gen_random_uuid(), 'audit.view', 'Аудит', 'Просмотр журнала аудита'),
    (gen_random_uuid(), 'settings.view', 'Настройки: просмотр', 'Доступ к экрану настроек'),
    (gen_random_uuid(), 'settings.write', 'Настройки: изменение', 'Изменение системных настроек'),
    (gen_random_uuid(), 'employee.read', 'Сотрудники: просмотр', 'Просмотр сотрудников'),
    (gen_random_uuid(), 'employee.write', 'Сотрудники: изменение', 'Создание и редактирование сотрудников'),
    (gen_random_uuid(), 'branch.read', 'Филиалы: просмотр', 'Просмотр филиалов'),
    (gen_random_uuid(), 'branch.write', 'Филиалы: изменение', 'Создание и редактирование филиалов'),
    (gen_random_uuid(), 'organization.read', 'Организации: просмотр', 'Просмотр организаций'),
    (gen_random_uuid(), 'organization.write', 'Организации: изменение', 'Создание и редактирование организаций'),
    (gen_random_uuid(), 'permission.read', 'Права: просмотр', 'Просмотр матрицы прав'),
    (gen_random_uuid(), 'permission.write', 'Права: изменение', 'Изменение прав сотрудников'),
    (gen_random_uuid(), 'finance.view', 'Финансы: просмотр', 'Просмотр финансовых данных'),
    (gen_random_uuid(), 'finance.write', 'Финансы: изменение', 'Редактирование финансовых данных'),
    (gen_random_uuid(), 'egisz.manual.send', 'ЕГИСЗ: ручная отправка', 'Ручная отправка в ЕГИСЗ'),
    (gen_random_uuid(), 'appointments.backdate.edit', 'Редактирование задним числом', 'Редактирование записей задним числом')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 2) Account-level override rows must be unique by employee+permission.
DELETE FROM employee_permission ep_keep
USING employee_permission ep_drop
WHERE ep_keep.employee_id = ep_drop.employee_id
  AND ep_keep.permission_id = ep_drop.permission_id
  AND ep_keep.id < ep_drop.id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_employee_permission_employee_permission'
    ) THEN
        ALTER TABLE employee_permission
            ADD CONSTRAINT uq_employee_permission_employee_permission
                UNIQUE (employee_id, permission_id);
    END IF;
END
$$;

-- 2.1) Enforce first-login password rotation for all existing SYSADMIN accounts.
UPDATE employee e
SET must_change_password = TRUE,
    updated_at = NOW()
WHERE e.must_change_password = FALSE
  AND EXISTS (
    SELECT 1
    FROM employee_role er
    JOIN role r ON r.id = er.role_id
    WHERE er.employee_id = e.id
      AND r.name = 'SYSADMIN'
);

-- 3) Deterministic role matrix.
--    SYSADMIN: only settings + account/structure/permissions management.
--    HEAD: read-only for account/structure/permissions.
--    ADMIN/DOCTOR/NURSE: clinical/operational access without management writes.
DELETE FROM role_permission rp
USING role r
WHERE rp.role_id = r.id
  AND r.name IN ('SYSADMIN', 'HEAD', 'ADMIN', 'DOCTOR', 'NURSE');

WITH desired(role_name, permission_code) AS (
    VALUES
        -- SYSADMIN
        ('SYSADMIN', 'settings.view'),
        ('SYSADMIN', 'settings.write'),
        ('SYSADMIN', 'employee.read'),
        ('SYSADMIN', 'employee.write'),
        ('SYSADMIN', 'branch.read'),
        ('SYSADMIN', 'branch.write'),
        ('SYSADMIN', 'organization.read'),
        ('SYSADMIN', 'organization.write'),
        ('SYSADMIN', 'permission.read'),
        ('SYSADMIN', 'permission.write'),

        -- HEAD (read-only for org/account/permissions)
        ('HEAD', 'dashboard.view'),
        ('HEAD', 'patients.view'),
        ('HEAD', 'doctors.view'),
        ('HEAD', 'schedule.view'),
        ('HEAD', 'appointments.view'),
        ('HEAD', 'reports.view'),
        ('HEAD', 'inventory.view'),
        ('HEAD', 'audit.view'),
        ('HEAD', 'settings.view'),
        ('HEAD', 'employee.read'),
        ('HEAD', 'branch.read'),
        ('HEAD', 'organization.read'),
        ('HEAD', 'permission.read'),
        ('HEAD', 'finance.view'),

        -- ADMIN
        ('ADMIN', 'dashboard.view'),
        ('ADMIN', 'posts.view'),
        ('ADMIN', 'patients.view'),
        ('ADMIN', 'doctors.view'),
        ('ADMIN', 'schedule.view'),
        ('ADMIN', 'appointments.view'),
        ('ADMIN', 'reports.view'),
        ('ADMIN', 'inventory.view'),
        ('ADMIN', 'inventory.write'),
        ('ADMIN', 'audit.view'),
        ('ADMIN', 'settings.view'),
        ('ADMIN', 'employee.read'),
        ('ADMIN', 'branch.read'),
        ('ADMIN', 'organization.read'),
        ('ADMIN', 'permission.read'),
        ('ADMIN', 'finance.view'),
        ('ADMIN', 'finance.write'),
        ('ADMIN', 'egisz.manual.send'),
        ('ADMIN', 'appointments.backdate.edit'),

        -- DOCTOR
        ('DOCTOR', 'dashboard.view'),
        ('DOCTOR', 'patients.view'),
        ('DOCTOR', 'doctors.view'),
        ('DOCTOR', 'schedule.view'),
        ('DOCTOR', 'appointments.view'),
        ('DOCTOR', 'settings.view'),
        ('DOCTOR', 'appointments.backdate.edit'),

        -- NURSE
        ('NURSE', 'dashboard.view'),
        ('NURSE', 'patients.view'),
        ('NURSE', 'doctors.view'),
        ('NURSE', 'schedule.view'),
        ('NURSE', 'appointments.view'),
        ('NURSE', 'inventory.view')
)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM desired d
JOIN role r ON r.name = d.role_name
JOIN permission p ON p.code = d.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;
