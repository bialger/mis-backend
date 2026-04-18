-- Align role matrix:
--   SYSADMIN manages accounts/structure/permissions.
--   HEAD (chief doctor) has full clinical/operational access except writes for accounts/structure/permissions.
--   ADMIN/DOCTOR/NURSE keep operational scopes.

UPDATE role
SET display_name = 'Главный врач',
    description = 'Полный клинический и операционный доступ без изменения сотрудников, филиалов, организаций и прав'
WHERE name = 'HEAD';

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

        -- HEAD (chief doctor): full access except account/structure/permission writes
        ('HEAD', 'dashboard.view'),
        ('HEAD', 'posts.view'),
        ('HEAD', 'patients.view'),
        ('HEAD', 'doctors.view'),
        ('HEAD', 'schedule.view'),
        ('HEAD', 'appointments.view'),
        ('HEAD', 'reports.view'),
        ('HEAD', 'inventory.view'),
        ('HEAD', 'inventory.write'),
        ('HEAD', 'audit.view'),
        ('HEAD', 'settings.view'),
        ('HEAD', 'settings.write'),
        ('HEAD', 'employee.read'),
        ('HEAD', 'branch.read'),
        ('HEAD', 'organization.read'),
        ('HEAD', 'permission.read'),
        ('HEAD', 'finance.view'),
        ('HEAD', 'finance.write'),
        ('HEAD', 'egisz.manual.send'),
        ('HEAD', 'appointments.backdate.edit'),

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
