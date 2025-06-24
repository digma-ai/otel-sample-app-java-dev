-- Add index for numeric_value column to improve query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value 
ON clinic_activity_logs (numeric_value);