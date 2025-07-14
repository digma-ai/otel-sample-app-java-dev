-- Create partitioned table for better data management
CREATE TABLE clinic_activity_logs_partitioned (
    id INTEGER NOT NULL,
    activity_type VARCHAR,
    numeric_value INTEGER,
    event_timestamp TIMESTAMP,
    status_flag BOOLEAN,
    payload TEXT
) PARTITION BY RANGE (event_timestamp);

-- Create partitions for last 3 months
CREATE TABLE clinic_activity_logs_partition_current PARTITION OF clinic_activity_logs_partitioned
    FOR VALUES FROM (CURRENT_DATE - INTERVAL '3 months') TO (CURRENT_DATE + INTERVAL '1 day');

CREATE TABLE clinic_activity_logs_partition_old PARTITION OF clinic_activity_logs_partitioned
    FOR VALUES FROM (MINVALUE) TO (CURRENT_DATE - INTERVAL '3 months');

-- Create indexes on partitioned table
CREATE INDEX idx_clinic_activity_logs_part_event_timestamp ON clinic_activity_logs_partitioned(event_timestamp);
CREATE INDEX idx_clinic_activity_logs_part_activity_type ON clinic_activity_logs_partitioned(activity_type);
CREATE INDEX idx_clinic_activity_logs_part_payload_length ON clinic_activity_logs_partitioned(LENGTH(payload));

-- Create function for maintenance
CREATE OR REPLACE FUNCTION maintain_clinic_activity_logs_partitions()
RETURNS void AS $$
BEGIN
    -- Create next month's partition if it doesn't exist
    IF NOT EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'clinic_activity_logs_partition_next_month'
        AND n.nspname = 'public'
    ) THEN
        EXECUTE format(
            'CREATE TABLE clinic_activity_logs_partition_next_month PARTITION OF clinic_activity_logs_partitioned
             FOR VALUES FROM (%L) TO (%L)',
            CURRENT_DATE + INTERVAL '1 day',
            CURRENT_DATE + INTERVAL '1 month'
        );
    END IF;
END;
$$ LANGUAGE plpgsql;