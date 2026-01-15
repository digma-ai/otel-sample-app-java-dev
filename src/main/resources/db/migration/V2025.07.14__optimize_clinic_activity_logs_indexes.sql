-- Drop duplicate and unused indexes
DROP INDEX IF EXISTS idx_clinic_activity_logs_activity_type;
DROP INDEX IF EXISTS idx_clinic_activity_logs_timestamp;
DROP INDEX IF EXISTS idx_clinic_activity_logs_payload_length;

-- Create extension for query performance monitoring
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Add configuration for improved buffer cache
ALTER SYSTEM SET shared_buffers = '1GB';  -- Adjust based on available memory
ALTER SYSTEM SET effective_cache_size = '3GB';  -- Adjust based on available memory
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '256MB';