-- AMS Database Initialization Script
-- This script creates the database schema for the Attendance Management System
-- Schema matches exactly with microservice entity models

-- Set timezone
SET timezone = 'Asia/Kolkata';

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- AUTH SERVICE TABLES
-- ============================================================================

-- Roles table (required first for foreign keys)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Organizations table (complete for organization service)
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    entity_id VARCHAR(8) UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Super Admins table
CREATE TABLE IF NOT EXISTS super_admins (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Entity Admins table
CREATE TABLE IF NOT EXISTS entity_admins (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    organization_id BIGINT,
    role_id BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Refresh Tokens table (for EntityAdmin)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL UNIQUE,
    admin_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (admin_id) REFERENCES entity_admins(id)
);

-- Super Admin Refresh Tokens table
CREATE TABLE IF NOT EXISTS super_admin_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL UNIQUE,
    super_admin_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (super_admin_id) REFERENCES super_admins(id)
);

-- Blacklisted Tokens table
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(1024) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR(100)
);

-- ============================================================================
-- SUBSCRIBER SERVICE TABLES
-- ============================================================================

-- Subscribers table
CREATE TABLE IF NOT EXISTS subscribers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    mobile_number VARCHAR(255) NOT NULL,
    organization_id BIGINT NOT NULL,
    profile_photo_path VARCHAR(500),
    face_encoding BYTEA,
    face_encoding_version VARCHAR(20) DEFAULT '1.0',
    face_registered_at TIMESTAMP,
    face_updated_at TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- NFC Cards table
CREATE TABLE IF NOT EXISTS nfc_cards (
    id BIGSERIAL PRIMARY KEY,
    card_uid VARCHAR(255) NOT NULL UNIQUE,
    subscriber_id BIGINT UNIQUE,
    entity_id VARCHAR(8),
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id),
    FOREIGN KEY (entity_id) REFERENCES organizations(entity_id)
);

-- Subscriber Auth table
CREATE TABLE IF NOT EXISTS subscriber_auth (
    id BIGSERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL UNIQUE,
    pin VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6),
    otp_expiry_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_device_id VARCHAR(255),
    last_device_info VARCHAR(255),
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id)
);

-- ============================================================================
-- ATTENDANCE SERVICE TABLES
-- ============================================================================

-- Scheduled Sessions table
CREATE TABLE IF NOT EXISTS scheduled_sessions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    organization_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Attendance Sessions table
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    organization_id BIGINT NOT NULL,
    description VARCHAR(255),
    qr_code VARCHAR(255),
    qr_code_expiry TIMESTAMP,
    scheduled_session_id BIGINT,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (scheduled_session_id) REFERENCES scheduled_sessions(id)
);

-- Attendance Logs table
CREATE TABLE IF NOT EXISTS attendance_logs (
    id BIGSERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    check_in_time TIMESTAMP NOT NULL,
    check_out_time TIMESTAMP,
    checkin_method VARCHAR(20) NOT NULL DEFAULT 'NFC',
    checkout_method VARCHAR(20),
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id),
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
);

-- ============================================================================
-- TABLE SERVICE TABLES
-- ============================================================================

-- Restaurant Tables table
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    table_number INTEGER NOT NULL,
    organization_id BIGINT NOT NULL,
    qr_code VARCHAR(500) UNIQUE,
    qr_code_url TEXT,
    is_active BOOLEAN DEFAULT true,
    capacity INTEGER,
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- ============================================================================
-- MENU SERVICE TABLES
-- ============================================================================

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    organization_id BIGINT NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE(name, organization_id)
);

-- Items table
CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    is_available BOOLEAN DEFAULT true,
    category_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- ============================================================================
-- ORDER SERVICE TABLES
-- ============================================================================

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    organization_id BIGINT NOT NULL,
    table_number INTEGER,
    table_id BIGINT,
    customer_name VARCHAR(255),
    customer_phone VARCHAR(255),
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'PENDING',
    special_instructions TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (table_id) REFERENCES restaurant_tables(id)
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    special_instructions VARCHAR(255),
    created_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- ============================================================================
-- COLLECTION TABLES FOR ENUMS
-- ============================================================================

-- Scheduled Session Days table
CREATE TABLE IF NOT EXISTS scheduled_session_days (
    scheduled_session_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    FOREIGN KEY (scheduled_session_id) REFERENCES scheduled_sessions(id)
);

-- Scheduled Session Check-in Methods table
CREATE TABLE IF NOT EXISTS scheduled_session_checkin_methods (
    scheduled_session_id BIGINT NOT NULL,
    checkin_method VARCHAR(20) NOT NULL,
    FOREIGN KEY (scheduled_session_id) REFERENCES scheduled_sessions(id)
);

-- Session Check-in Methods table
CREATE TABLE IF NOT EXISTS session_checkin_methods (
    session_id BIGINT NOT NULL,
    checkin_method VARCHAR(20) NOT NULL,
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
);

-- Organization Permissions table
CREATE TABLE IF NOT EXISTS organization_permissions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    feature_permission VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    granted_by VARCHAR(255),
    granted_at TIMESTAMP,
    expires_at TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE(organization_id, feature_permission)
);

-- ============================================================================
-- INITIAL DATA
-- ============================================================================

-- Insert default roles
INSERT INTO roles (name) VALUES
    ('SUPER_ADMIN'),
    ('ENTITY_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Insert sample organization
INSERT INTO organizations (entity_id, name, address, contact_person, email) VALUES
    ('MSD00001', 'Demo Restaurant', '123 Main Street, City', 'John Doe', 'admin@demo.com')
ON CONFLICT (entity_id) DO NOTHING;

-- Insert default super admin (password: 'admin123')
INSERT INTO super_admins (username, password, email, first_name, last_name, role_id) VALUES
    ('superadmin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdxIcnvtcflQDHva', 'super@admin.com', 'Super', 'Admin', 1)
ON CONFLICT (username) DO NOTHING;

-- Insert default entity admin (password: 'admin123')
INSERT INTO entity_admins (username, password, organization_id, role_id) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdxIcnvtcflQDHva', 1, 2)
ON CONFLICT DO NOTHING;

-- Subscribers table
CREATE TABLE IF NOT EXISTS subscribers (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255),
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    employee_id VARCHAR(50),
    department VARCHAR(100),
    position VARCHAR(100),
    organization_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT true,
    face_registered BOOLEAN DEFAULT false,
    profile_photo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE(username, organization_id),
    UNIQUE(employee_id, organization_id)
);

-- NFC Cards table
CREATE TABLE IF NOT EXISTS nfc_cards (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(100) NOT NULL UNIQUE,
    subscriber_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT true,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Refresh Tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    entity_admin_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (entity_admin_id) REFERENCES entity_admins(id)
);

-- Super Admin Refresh Tokens table
CREATE TABLE IF NOT EXISTS super_admin_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    super_admin_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (super_admin_id) REFERENCES super_admins(id)
);

-- Blacklisted Tokens table
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_type VARCHAR(20) NOT NULL, -- 'ENTITY_ADMIN', 'SUPER_ADMIN', 'SUBSCRIBER'
    blacklisted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- Attendance Sessions table
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    check_in_time TIMESTAMP NOT NULL,
    check_out_time TIMESTAMP,
    check_in_method VARCHAR(20) NOT NULL, -- 'NFC', 'QR', 'FACE', 'MANUAL'
    check_out_method VARCHAR(20),
    check_in_location VARCHAR(255),
    check_out_location VARCHAR(255),
    total_hours DECIMAL(5,2),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'COMPLETED', 'INCOMPLETE'
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (subscriber_id) REFERENCES subscribers(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Restaurant Tables table
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    table_number VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    location VARCHAR(100),
    organization_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT true,
    occupied BOOLEAN DEFAULT false,
    qr_code VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE(table_number, organization_id)
);

-- Menu Categories table
CREATE TABLE IF NOT EXISTS menu_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    organization_id BIGINT NOT NULL,
    display_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    UNIQUE(name, organization_id)
);

-- Menu Items table
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    image_url VARCHAR(500),
    preparation_time INTEGER DEFAULT 15, -- in minutes
    available BOOLEAN DEFAULT true,
    vegetarian BOOLEAN DEFAULT false,
    vegan BOOLEAN DEFAULT false,
    spicy_level INTEGER DEFAULT 0, -- 0-5 scale
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES menu_categories(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    table_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    customer_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED', 'CANCELLED'
    total_amount DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    special_instructions TEXT,
    estimated_ready_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (table_id) REFERENCES restaurant_tables(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    special_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_organizations_entity_id ON organizations(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_admins_org_id ON entity_admins(organization_id);
CREATE INDEX IF NOT EXISTS idx_subscribers_org_id ON subscribers(organization_id);
CREATE INDEX IF NOT EXISTS idx_subscribers_employee_id ON subscribers(employee_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_card_id ON nfc_cards(card_id);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_subscriber ON attendance_sessions(subscriber_id);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_org ON attendance_sessions(organization_id);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_date ON attendance_sessions(check_in_time);
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_org ON restaurant_tables(organization_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON menu_items(category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_org ON menu_items(organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_table ON orders(table_id);
CREATE INDEX IF NOT EXISTS idx_orders_org ON orders(organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);

-- Insert sample data
INSERT INTO organizations (entity_id, name, address, contact_person, email, phone) VALUES
('MSD00001', 'Demo Restaurant', '123 Main Street, City', 'John Doe', 'admin@demo.com', '+1234567890')
ON CONFLICT (entity_id) DO NOTHING;

INSERT INTO super_admins (username, password, email, first_name, last_name) VALUES
('superadmin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdxIcnvtcflQDHva', 'super@admin.com', 'Super', 'Admin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO entity_admins (username, password, email, first_name, last_name, organization_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdxIcnvtcflQDHva', 'admin@demo.com', 'Demo', 'Admin', 1)
ON CONFLICT (username, organization_id) DO NOTHING;
