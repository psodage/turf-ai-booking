-- ============================================================
-- Repeatable Migration: Seed Demo Data for Local Dev & Testing
-- Target Business: Green Pitch Kolhapur
-- Target Owner: Rajesh Patil (+919876543210)
-- Target Turf: Green Pitch Main Turf (5v5)
-- ============================================================

-- 1. Insert Business
INSERT INTO business (id, name, address, city, state, pincode, google_maps_link, phone, whatsapp_phone_number_id, timezone, status)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Green Pitch Kolhapur',
    'Near Rankala Lake, Ring Road',
    'Kolhapur',
    'Maharashtra',
    '416012',
    'https://maps.google.com/?q=Rankala+Kolhapur',
    '+919876543210',
    'PHONE_NUM_ID_DEMO_001',
    'Asia/Kolkata',
    'ACTIVE'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    whatsapp_phone_number_id = EXCLUDED.whatsapp_phone_number_id;

-- 2. Insert Owner User
INSERT INTO users (id, business_id, name, phone, email, role, language, status)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Rajesh Patil',
    '+919876543210',
    'rajesh@greenpitch.in',
    'OWNER',
    'en',
    'ACTIVE'
)
ON CONFLICT (phone) DO UPDATE SET
    name = EXCLUDED.name,
    business_id = EXCLUDED.business_id;

-- 3. Insert Turf
INSERT INTO turf (id, business_id, name, type, capacity, status)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Green Pitch Main Turf',
    'FIVE_A_SIDE',
    10,
    'ACTIVE'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    capacity = EXCLUDED.capacity;

-- 4. Insert Operating Hours (Mon-Sun: 06:00 to 23:00)
INSERT INTO operating_hours (id, turf_id, day_of_week, opening_time, closing_time, is_closed)
VALUES
    ('44444444-4444-4444-4444-444444444400', '33333333-3333-3333-3333-333333333333', 0, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444401', '33333333-3333-3333-3333-333333333333', 1, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444402', '33333333-3333-3333-3333-333333333333', 2, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444403', '33333333-3333-3333-3333-333333333333', 3, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444404', '33333333-3333-3333-3333-333333333333', 4, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444405', '33333333-3333-3333-3333-333333333333', 5, '06:00:00', '23:00:00', FALSE),
    ('44444444-4444-4444-4444-444444444406', '33333333-3333-3333-3333-333333333333', 6, '06:00:00', '23:00:00', FALSE)
ON CONFLICT (turf_id, day_of_week) DO UPDATE SET
    opening_time = EXCLUDED.opening_time,
    closing_time = EXCLUDED.closing_time,
    is_closed = EXCLUDED.is_closed;

-- 5. Insert Pricing Rules (BASE = ₹1000, WEEKEND = ₹1200, PEAK 18:00-22:00 = ₹1500)
INSERT INTO pricing_rule (id, turf_id, pricing_type, day_of_week, start_time, end_time, amount)
VALUES
    ('55555555-5555-5555-5555-555555555501', '33333333-3333-3333-3333-333333333333', 'BASE', NULL, NULL, NULL, 1000.00),
    ('55555555-5555-5555-5555-555555555502', '33333333-3333-3333-3333-333333333333', 'WEEKEND', NULL, NULL, NULL, 1200.00),
    ('55555555-5555-5555-5555-555555555503', '33333333-3333-3333-3333-333333333333', 'PEAK', NULL, '18:00:00', '22:00:00', 1500.00)
ON CONFLICT (id) DO UPDATE SET
    amount = EXCLUDED.amount;

-- 6. Insert System Settings
INSERT INTO system_setting (key, value, description)
VALUES
    ('HOLD_DURATION_MINUTES', '10', 'Booking hold duration before expiration'),
    ('CANCELLATION_WINDOW_HOURS', '2', 'Minimum lead time required before slot start time for customer cancellation'),
    ('ADVANCE_BOOKING_DAYS', '30', 'Maximum days in advance a customer can book a slot'),
    ('BUSINESS_DEFAULT_TIMEZONE', 'Asia/Kolkata', 'System default business timezone')
ON CONFLICT (key) DO UPDATE SET
    value = EXCLUDED.value;
