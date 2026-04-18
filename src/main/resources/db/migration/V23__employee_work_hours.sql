-- Per-employee reception hours for online booking (intersected with branch hours on the server).
-- IF NOT EXISTS: safe if the script is re-applied manually; Flyway still records the version once.
ALTER TABLE employee ADD COLUMN IF NOT EXISTS work_start_time TIME;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS work_end_time TIME;
