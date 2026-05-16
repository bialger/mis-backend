-- V28: Add user_agent column to audit_log for HTTP context tracking
ALTER TABLE audit_log ADD COLUMN user_agent TEXT;
