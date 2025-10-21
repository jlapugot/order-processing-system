-- Initialize Order Processing System Database
-- This script creates necessary database objects for local development

-- Create database (if needed)
-- Note: Database is already created by POSTGRES_DB env variable

-- Grant all privileges
GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;

-- Set timezone
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Order Processing System Database initialized successfully';
END
$$;
