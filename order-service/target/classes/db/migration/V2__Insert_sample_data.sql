-- Insert sample users
INSERT INTO users (id, first_name, last_name, email, phone_number, street_address, city, state, postal_code, country, status) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'John', 'Doe', 'john.doe@email.com', '555-0101', '123 Main St', 'New York', 'NY', '10001', 'USA', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440002', 'Jane', 'Smith', 'jane.smith@email.com', '555-0102', '456 Oak Ave', 'Los Angeles', 'CA', '90210', 'USA', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440003', 'Bob', 'Johnson', 'bob.johnson@email.com', '555-0103', '789 Pine Rd', 'Chicago', 'IL', '60601', 'USA', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440004', 'Alice', 'Williams', 'alice.williams@email.com', '555-0104', '321 Elm St', 'Houston', 'TX', '77001', 'USA', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440005', 'Charlie', 'Brown', 'charlie.brown@email.com', '555-0105', '654 Maple Dr', 'Phoenix', 'AZ', '85001', 'USA', 'ACTIVE');

-- Insert sample products
INSERT INTO products (id, name, description, sku, price, stock_quantity, category, brand, weight, status) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'Wireless Bluetooth Headphones', 'High-quality wireless headphones with noise cancellation', 'WBH-001', 99.99, 50, 'Electronics', 'AudioTech', 0.25, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440002', 'Smartphone Case', 'Protective case for smartphones with shock absorption', 'SPC-002', 19.99, 100, 'Accessories', 'ProtectPro', 0.05, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440003', 'Laptop Stand', 'Adjustable aluminum laptop stand for better ergonomics', 'LPS-003', 49.99, 30, 'Office', 'ErgoDesk', 1.20, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440004', 'Coffee Mug', 'Ceramic coffee mug with heat retention technology', 'CMG-004', 14.99, 75, 'Kitchen', 'BrewMaster', 0.30, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440005', 'Fitness Tracker', 'Smart fitness tracker with heart rate monitoring', 'FTR-005', 79.99, 25, 'Health', 'FitLife', 0.08, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440006', 'Desk Lamp', 'LED desk lamp with adjustable brightness and color temperature', 'DLM-006', 34.99, 40, 'Office', 'LightPro', 0.80, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440007', 'Water Bottle', 'Stainless steel water bottle with temperature retention', 'WBT-007', 24.99, 60, 'Sports', 'HydroMax', 0.45, 'ACTIVE'),
('660e8400-e29b-41d4-a716-446655440008', 'Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 'WMS-008', 29.99, 80, 'Electronics', 'ClickTech', 0.12, 'ACTIVE');

-- Insert sample orders
INSERT INTO orders (id, order_number, user_id, status, total_amount, tax_amount, shipping_amount, shipping_street_address, shipping_city, shipping_state, shipping_postal_code, shipping_country, created_at) VALUES
('770e8400-e29b-41d4-a716-446655440001', 'ORD-2024-001', '550e8400-e29b-41d4-a716-446655440001', 'DELIVERED', 134.97, 10.80, 9.99, '123 Main St', 'New York', 'NY', '10001', 'USA', '2024-01-15 10:30:00'),
('770e8400-e29b-41d4-a716-446655440002', 'ORD-2024-002', '550e8400-e29b-41d4-a716-446655440002', 'SHIPPED', 84.97, 6.80, 7.99, '456 Oak Ave', 'Los Angeles', 'CA', '90210', 'USA', '2024-01-20 14:15:00'),
('770e8400-e29b-41d4-a716-446655440003', 'ORD-2024-003', '550e8400-e29b-41d4-a716-446655440003', 'PROCESSING', 59.98, 4.80, 5.99, '789 Pine Rd', 'Chicago', 'IL', '60601', 'USA', '2024-01-25 09:45:00'),
('770e8400-e29b-41d4-a716-446655440004', 'ORD-2024-004', '550e8400-e29b-41d4-a716-446655440001', 'CONFIRMED', 44.98, 3.60, 4.99, '123 Main St', 'New York', 'NY', '10001', 'USA', '2024-01-28 16:20:00'),
('770e8400-e29b-41d4-a716-446655440005', 'ORD-2024-005', '550e8400-e29b-41d4-a716-446655440004', 'PENDING', 104.98, 8.40, 8.99, '321 Elm St', 'Houston', 'TX', '77001', 'USA', '2024-01-30 11:10:00');

-- Insert sample order items
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, total_price) VALUES
-- Order 1 items
('880e8400-e29b-41d4-a716-446655440001', '770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440001', 1, 99.99, 99.99),
('880e8400-e29b-41d4-a716-446655440002', '770e8400-e29b-41d4-a716-446655440001', '660e8400-e29b-41d4-a716-446655440004', 1, 14.99, 14.99),

-- Order 2 items
('880e8400-e29b-41d4-a716-446655440003', '770e8400-e29b-41d4-a716-446655440002', '660e8400-e29b-41d4-a716-446655440005', 1, 79.99, 79.99),

-- Order 3 items
('880e8400-e29b-41d4-a716-446655440004', '770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440002', 2, 19.99, 39.98),
('880e8400-e29b-41d4-a716-446655440005', '770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440008', 1, 29.99, 29.99),

-- Order 4 items
('880e8400-e29b-41d4-a716-446655440006', '770e8400-e29b-41d4-a716-446655440004', '660e8400-e29b-41d4-a716-446655440006', 1, 34.99, 34.99),

-- Order 5 items
('880e8400-e29b-41d4-a716-446655440007', '770e8400-e29b-41d4-a716-446655440005', '660e8400-e29b-41d4-a716-446655440003', 1, 49.99, 49.99),
('880e8400-e29b-41d4-a716-446655440008', '770e8400-e29b-41d4-a716-446655440005', '660e8400-e29b-41d4-a716-446655440007', 2, 24.99, 49.98);
