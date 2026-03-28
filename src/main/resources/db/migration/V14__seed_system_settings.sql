-- Default access (maximal)

INSERT INTO system_setting (id, branch_id, key, value, description)
SELECT gen_random_uuid(), NULL, v.key, v.value, v.description
FROM (VALUES
    ('canViewFinance', 'true', 'Просмотр финансовых разделов (RBAC)'),
    ('canEditFinance', 'true', 'Редактирование финансовых данных'),
    ('canViewInventory', 'true', 'Просмотр склада и остатков'),
    ('canWriteInventory', 'true', 'Списание и корректировки склада'),
    ('canManualEgiszSend', 'true', 'Ручная отправка в ЕГИСЗ'),
    ('canEditBackdateDays', '60', 'На сколько дней назад разрешено задним числом редактировать записи (60 = только сегодня)')
) AS v(key, value, description)
WHERE NOT EXISTS (
    SELECT 1 FROM system_setting s WHERE s.branch_id IS NULL AND s.key = v.key
);
