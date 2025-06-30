-- Migration V16: Create base tables for menu and ordering system
-- This migration creates the core tables needed for the restaurant/menu functionality

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, organization_id)
);

-- Create items table
CREATE TABLE IF NOT EXISTS items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    image_url VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    is_available BOOLEAN DEFAULT true,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, category_id)
);

-- Create restaurant_tables table
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id SERIAL PRIMARY KEY,
    table_number INTEGER NOT NULL CHECK (table_number > 0),
    qr_code VARCHAR(500) UNIQUE,
    qr_code_url VARCHAR(2000),
    is_active BOOLEAN DEFAULT true,
    capacity INTEGER CHECK (capacity IS NULL OR capacity > 0),
    location_description VARCHAR(255),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(table_number, organization_id)
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    table_number INTEGER,
    table_id BIGINT REFERENCES restaurant_tables(id) ON DELETE SET NULL,
    customer_name VARCHAR(100),
    customer_phone VARCHAR(20),
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED', 'CANCELLED')),
    special_instructions TEXT,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    special_instructions VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_categories_organization ON categories(organization_id);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active, organization_id);
CREATE INDEX IF NOT EXISTS idx_categories_display_order ON categories(display_order, organization_id);

CREATE INDEX IF NOT EXISTS idx_items_category ON items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_organization ON items(organization_id);
CREATE INDEX IF NOT EXISTS idx_items_active_available ON items(is_active, is_available, organization_id);
CREATE INDEX IF NOT EXISTS idx_items_display_order ON items(display_order, category_id);

CREATE INDEX IF NOT EXISTS idx_restaurant_tables_org_table_number ON restaurant_tables(organization_id, table_number);
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_qr_code ON restaurant_tables(qr_code);
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_active ON restaurant_tables(is_active, organization_id);

CREATE INDEX IF NOT EXISTS idx_orders_organization ON orders(organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_table_number ON orders(table_number, organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_table_id ON orders(table_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status, organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_item ON order_items(item_id);

-- Add comments for documentation
COMMENT ON TABLE categories IS 'Menu categories for restaurant items';
COMMENT ON TABLE items IS 'Menu items belonging to categories';
COMMENT ON TABLE restaurant_tables IS 'Restaurant tables with QR codes for ordering';
COMMENT ON TABLE orders IS 'Customer orders placed through the system';
COMMENT ON TABLE order_items IS 'Individual items within an order';

COMMENT ON COLUMN restaurant_tables.qr_code IS 'Unique QR code identifier for table';
COMMENT ON COLUMN restaurant_tables.qr_code_url IS 'Generated QR code image URL';
COMMENT ON COLUMN orders.table_id IS 'Foreign key reference to restaurant_tables';
COMMENT ON COLUMN orders.table_number IS 'Denormalized table number for quick access';
