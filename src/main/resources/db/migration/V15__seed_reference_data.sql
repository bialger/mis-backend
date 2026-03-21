-- Non-PII reference data: default org/branch/room, specialties, roles, inventory categories, patient tag types

-- Default organization and branch (only if tables are empty)
INSERT INTO organization (id, name, created_at)
SELECT 'a0000001-0000-4000-8000-000000000001'::uuid, 'МедЦентр', NOW()
WHERE NOT EXISTS (SELECT 1 FROM organization);

INSERT INTO branch (id, organization_id, name, is_active, created_at)
SELECT 'a0000002-0000-4000-8000-000000000001'::uuid, o.id, 'Центральный', true, NOW()
FROM (SELECT id FROM organization ORDER BY created_at NULLS LAST LIMIT 1) o
WHERE NOT EXISTS (SELECT 1 FROM branch);

INSERT INTO room (id, branch_id, name, is_active)
SELECT gen_random_uuid(), sub.id, 'Кабинет 1', true
FROM (SELECT b.id FROM branch b WHERE b.name = 'Центральный' LIMIT 1) sub
WHERE NOT EXISTS (
    SELECT 1 FROM room r WHERE r.branch_id = sub.id AND r.name = 'Кабинет 1'
);

-- Specialties
INSERT INTO specialty (id, name, description)
SELECT gen_random_uuid(), v.name, v.description
FROM (VALUES
    ('Терапия', 'Общая терапия'),
    ('Хирургия', 'Хирургия'),
    ('Педиатрия', 'Дети'),
    ('Стоматология', 'Стоматология'),
    ('Кардиология', 'Сердечно-сосудистые заболевания')
) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM specialty s WHERE s.name = v.name);

-- Roles
INSERT INTO role (id, name, display_name, description)
SELECT gen_random_uuid(), v.name, v.display_name, v.description
FROM (VALUES
    ('ADMIN', 'Администратор', 'Полный доступ к настройкам'),
    ('DOCTOR', 'Врач', 'Врачебная роль'),
    ('NURSE', 'Медсестра', 'Сестринская роль'),
    ('HEAD', 'Руководитель', 'Руководитель филиала / отделения')
) AS v(name, display_name, description)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = v.name);

-- Inventory categories (codes match frontend getCategoryText keys)
INSERT INTO inventory_category (id, name, description)
SELECT gen_random_uuid(), v.name, v.description
FROM (VALUES
    ('DRUGS', 'Медикаменты'),
    ('MED_DEVICES', 'Медицинские изделия'),
    ('MED_CONSUMABLES', 'Медицинские расходники'),
    ('HOUSEHOLD_CONSUMABLES', 'Хозяйственные расходники')
) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM inventory_category c WHERE c.name = v.name);

-- Patient tag types (icons / badges)
INSERT INTO patient_tag_type (id, code, icon, name, description, is_active)
SELECT gen_random_uuid(), v.code, v.icon, v.name, v.description, true
FROM (VALUES
    ('VIP', '⭐', 'VIP', 'Приоритетное обслуживание'),
    ('ALLERGY', '⚠', 'Аллергия', 'Отмечены аллергии'),
    ('CHRONIC', '📋', 'Хронические', 'Хронические заболевания'),
    ('NEW', '🆕', 'Новый', 'Новый пациент клиники')
) AS v(code, icon, name, description)
WHERE NOT EXISTS (SELECT 1 FROM patient_tag_type p WHERE p.code = v.code);
