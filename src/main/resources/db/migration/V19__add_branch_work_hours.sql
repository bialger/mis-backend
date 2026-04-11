-- Add configurable working hours to the branch table.
-- Previously these were hardcoded as 08:00-20:00 in the application layer.

ALTER TABLE branch
    ADD COLUMN IF NOT EXISTS start_time TIME NOT NULL DEFAULT '08:00:00',
    ADD COLUMN IF NOT EXISTS end_time   TIME NOT NULL DEFAULT '20:00:00';
