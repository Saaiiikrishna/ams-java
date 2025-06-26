-- Database initialization script for microservices
-- This script sets up the database schema for all microservices

-- Create databases for each microservice (future separation)
-- For now, we'll use a shared database but prepare for separation

-- Auth Service Schema
CREATE SCHEMA IF NOT EXISTS auth_service;

-- Organization Service Schema  
CREATE SCHEMA IF NOT EXISTS organization_service;

-- Subscriber Service Schema
CREATE SCHEMA IF NOT EXISTS subscriber_service;

-- Attendance Service Schema
CREATE SCHEMA IF NOT EXISTS attendance_service;

-- Menu Service Schema
CREATE SCHEMA IF NOT EXISTS menu_service;

-- Order Service Schema
CREATE SCHEMA IF NOT EXISTS order_service;

-- Table Service Schema
CREATE SCHEMA IF NOT EXISTS table_service;

-- Grant permissions to postgres user for all schemas
GRANT ALL PRIVILEGES ON SCHEMA auth_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA organization_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA subscriber_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA attendance_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA menu_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA order_service TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA table_service TO postgres;

-- Create service-specific users (for future database separation)
DO $$
BEGIN
    -- Auth Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'auth_service_user') THEN
        CREATE USER auth_service_user WITH PASSWORD 'auth_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO auth_service_user;
        GRANT USAGE ON SCHEMA auth_service TO auth_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_service TO auth_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth_service TO auth_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA auth_service GRANT ALL ON TABLES TO auth_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA auth_service GRANT ALL ON SEQUENCES TO auth_service_user;
    END IF;

    -- Organization Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'organization_service_user') THEN
        CREATE USER organization_service_user WITH PASSWORD 'organization_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO organization_service_user;
        GRANT USAGE ON SCHEMA organization_service TO organization_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA organization_service TO organization_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA organization_service TO organization_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA organization_service GRANT ALL ON TABLES TO organization_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA organization_service GRANT ALL ON SEQUENCES TO organization_service_user;
    END IF;

    -- Subscriber Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'subscriber_service_user') THEN
        CREATE USER subscriber_service_user WITH PASSWORD 'subscriber_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO subscriber_service_user;
        GRANT USAGE ON SCHEMA subscriber_service TO subscriber_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA subscriber_service TO subscriber_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA subscriber_service TO subscriber_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA subscriber_service GRANT ALL ON TABLES TO subscriber_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA subscriber_service GRANT ALL ON SEQUENCES TO subscriber_service_user;
    END IF;

    -- Attendance Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'attendance_service_user') THEN
        CREATE USER attendance_service_user WITH PASSWORD 'attendance_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO attendance_service_user;
        GRANT USAGE ON SCHEMA attendance_service TO attendance_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA attendance_service TO attendance_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA attendance_service TO attendance_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA attendance_service GRANT ALL ON TABLES TO attendance_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA attendance_service GRANT ALL ON SEQUENCES TO attendance_service_user;
    END IF;

    -- Menu Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'menu_service_user') THEN
        CREATE USER menu_service_user WITH PASSWORD 'menu_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO menu_service_user;
        GRANT USAGE ON SCHEMA menu_service TO menu_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA menu_service TO menu_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA menu_service TO menu_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA menu_service GRANT ALL ON TABLES TO menu_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA menu_service GRANT ALL ON SEQUENCES TO menu_service_user;
    END IF;

    -- Order Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'order_service_user') THEN
        CREATE USER order_service_user WITH PASSWORD 'order_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO order_service_user;
        GRANT USAGE ON SCHEMA order_service TO order_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA order_service TO order_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA order_service TO order_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA order_service GRANT ALL ON TABLES TO order_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA order_service GRANT ALL ON SEQUENCES TO order_service_user;
    END IF;

    -- Table Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'table_service_user') THEN
        CREATE USER table_service_user WITH PASSWORD 'table_service_password_2024';
        GRANT CONNECT ON DATABASE attendance_db TO table_service_user;
        GRANT USAGE ON SCHEMA table_service TO table_service_user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA table_service TO table_service_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA table_service TO table_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA table_service GRANT ALL ON TABLES TO table_service_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA table_service GRANT ALL ON SEQUENCES TO table_service_user;
    END IF;
END
$$;

-- Create indexes for better performance
-- These will be created automatically by JPA, but we can add custom ones here

-- Log the completion
INSERT INTO public.migration_log (version, description, executed_at) 
VALUES ('microservices_v1.0.0', 'Microservices database schema initialization', NOW())
ON CONFLICT (version) DO NOTHING;
