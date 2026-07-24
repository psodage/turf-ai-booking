# Turf AI Booking — Database & ER Diagram

**Document:** 09-database-erd.md
**Version:** 2.0
**Status:** Approved Architecture
**Database:** PostgreSQL 16+
**ORM:** Spring Data JPA / Hibernate
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the complete database architecture for Turf AI Booking.

The goals are:

- Prevent double bookings
- Support multiple turf businesses
- Support AI agents
- Support WhatsApp
- Support payments
- Support reporting
- Maintain audit history
- Ensure ACID compliance

The database is the single source of truth.

---

# 2. Design Principles

The database must provide:

✓ Data Integrity

✓ Atomic Transactions

✓ Referential Integrity

✓ Multi-Tenant Isolation

✓ Auditability

✓ Scalability

✓ High Performance

✓ Booking Conflict Prevention

---

# 3. High-Level ER Diagram

```text
Business
   │
   ├──────── Users (Owner, Manager)
   │
   ├──────── Turf
   │             │
   │             ├──────── Operating Hours
   │             │
   │             ├──────── Pricing Rules
   │             │
   │             ├──────── Blocked Slots
   │             │
   │             └──────── Booking
   │                        │
   │                        ├──────── Payment (1:N)
   │                        ├──────── Booking Hold (1:1)
   │                        ├──────── Booking Audit
   │                        └──────── Users (Customer)
   │
   └──────── Reports

Users (Customer)
   │
   └──────── Bookings (across any business)

Conversation
   │
   └──────── Conversation Messages
```

Key: Customers are NOT scoped to a single business. See ADR-002.

---

# 4. Database Schema

Schema:

public

Future:

Separate schema per tenant (optional)

---

# 5. Core Tables

```
business

users

turf

operating_hours

pricing_rule

booking

booking_hold

payment

blocked_slot

conversation

conversation_message

notification

booking_audit

payment_audit

report

system_setting
```

---

# 6. BUSINESS

Represents a Turf Business.

Fields

```
id (UUID)

name

address

city

state

pincode

google_maps_link

phone

whatsapp_phone_number_id

timezone (e.g., Asia/Kolkata — ADR-019)

status

created_at

updated_at
```

`whatsapp_phone_number_id` maps this business to its WhatsApp Business phone number for webhook routing (see ADR-006).

Status: `ACTIVE`, `INACTIVE`, `SUSPENDED`

Relationship

Business → Many Turfs (1:N)

Business → Many Users (1:N, for OWNER and MANAGER roles)

---

# 7. USERS

Represents:

- Customer
- Owner
- Manager
- Admin

Fields

```
id (UUID)

business_id (nullable — see rules below)

name

phone (unique)

email

role

language

status

created_at

updated_at
```

Role

```
CUSTOMER

OWNER

MANAGER

ADMIN
```

### business_id Rules (ADR-002)

- OWNER: `business_id` is required. Owner belongs to one business.
- MANAGER: `business_id` is required. Manager belongs to one business.
- CUSTOMER: `business_id` is NULL. Customers are global. Their relationship to businesses is through bookings.
- ADMIN: `business_id` is NULL. Admins are global.

### Uniqueness

- `phone` is unique across the entire table. One phone number = one user.
- If a customer messages a new turf business, the existing user record is reused.

---

# 8. TURF

Represents one playable turf.

Fields

```
id (UUID)

business_id (required)

name

type

capacity

status

created_at

updated_at
```

Status: `ACTIVE`, `INACTIVE`

Examples

Turf 1 — 5v5

Turf 2 — 7v7

---

# 9. OPERATING HOURS

Stores daily opening hours per turf.

Fields

```
id (UUID)

turf_id

day_of_week

opening_time

closing_time

is_closed
```

Rules:

- One Turf → 7 Operating Hour records (one per day).
- Operating hours are per turf, not per business.
- `day_of_week`: 0 (Monday) through 6 (Sunday).
- `is_closed = true` means no bookings on that day.

---

# 10. PRICING RULE

Stores pricing.

Fields

```
id (UUID)

turf_id

pricing_type

day_of_week (nullable)

start_time (nullable)

end_time (nullable)

amount

created_at

updated_at
```

Pricing Types

```
BASE

WEEKEND

PEAK
```

Resolution order: PEAK → WEEKEND → BASE (most specific wins).

---

# 11. BOOKING

Central table.

Fields

```
id (UUID)

booking_number (human-readable, e.g., BK-2026-00123)

business_id

turf_id

customer_id

booking_date

start_time

end_time

price

status

booking_source

cancelled_at (nullable — timestamp when cancelled)

cancelled_by (nullable — user_id who cancelled)

created_at

updated_at
```

### booking_number Generation

Format: `BK-{YEAR}-{SEQUENCE}`

Example: `BK-2026-00001`, `BK-2026-00002`

- Uses a global PostgreSQL `SEQUENCE` (not per-business).
- Sequence resets annually.
- Collision-proof: database guarantees uniqueness.

Status (see ADR-004)

```
HOLD

PAYMENT_PENDING

CONFIRMED

COMPLETED

EXPIRED

CANCELLED

NO_SHOW
```

Booking Source

```
WHATSAPP_AI

OWNER_MANUAL

ADMIN
```

---

# 12. BOOKING HOLD (ADR-014)

Temporary slot reservation linked to a booking.

Slot data (turf_id, booking_date, start_time, end_time) is NOT duplicated here — it lives only on the `booking` table.

Fields

```
id (UUID)

booking_id (unique — one hold per booking)

expires_at

status

created_at
```

Status

```
ACTIVE

EXPIRED

CONVERTED (hold was converted to confirmed booking)
```

### Expiry (ADR-005)

- `expires_at` is set to `NOW() + 10 minutes` on creation.
- Lazy expiry: queries filter by `expires_at > NOW()`.
- Cleanup job runs every 2 minutes to mark expired holds.

### Grace Period (ADR-016)

- If a payment webhook arrives within 60 seconds after hold expiry, the system attempts to re-acquire the slot before refunding.

---

# 13. PAYMENT

Stores payment attempts. One booking can have many payments (see ADR-003).

Fields

```
id (UUID)

booking_id

business_id

customer_id

gateway

gateway_order_id

gateway_payment_id

amount

currency

status

refund_status

created_at

updated_at
```

Status

```
CREATED

PENDING

SUCCESS

FAILED

EXPIRED
```

Refund Status

```
NOT_REQUIRED

REQUESTED

PROCESSING

SUCCESS

FAILED
```

### Relationship (ADR-003)

- Booking → Payment is **1:N** (one booking, many payment attempts).
- Only a payment with `status = SUCCESS` confirms the booking.
- Failed and expired payments remain as records for audit.

---

# 14. BLOCKED SLOT

Owner-blocked periods.

Fields

```
id (UUID)

turf_id

date

start_time

end_time

reason

created_by

created_at
```

Reason

```
MAINTENANCE

PRIVATE_EVENT

OWNER_USE
```

---

# 15. CONVERSATION

Stores WhatsApp conversations.

Fields

```
id (UUID)

user_id

business_id

role

current_intent

status

last_activity

created_at
```

Status: `ACTIVE`, `CLOSED`, `EXPIRED`

---

# 16. CONVERSATION MESSAGE

Stores messages.

Fields

```
id (UUID)

conversation_id

sender (USER or AI)

message

message_type (TEXT, BUTTON, LIST, LOCATION)

whatsapp_message_id (for deduplication)

created_at
```

---

# 17. NOTIFICATION

Stores notifications.

Fields

```
id (UUID)

user_id

booking_id (nullable)

business_id

type (BOOKING_CONFIRMED, REMINDER, CANCELLATION, etc.)

channel (WHATSAPP)

status (PENDING, SENT, FAILED)

retry_count

sent_at

created_at
```

---

# 18. BOOKING AUDIT

Booking history.

Fields

```
id (UUID)

booking_id

old_status

new_status

changed_by (user_id)

reason

changed_at
```

---

# 19. PAYMENT AUDIT

Payment events.

Fields

```
id (UUID)

payment_id

event

gateway_payload (JSON)

created_at
```

---

# 20. REPORT

Generated reports.

Fields

```
id (UUID)

business_id

report_type (DAILY, WEEKLY, MONTHLY)

file_path

generated_by (user_id, nullable)

generated_at
```

Reports are stored as temporary files on local filesystem (see ADR-012).

---

# 21. SYSTEM SETTING

Stores configurable values.

Fields

```
key (primary key)

value

description
```

---

# 22. Relationships

Business → Users: 1:N (for OWNER, MANAGER roles)

Business → Turfs: 1:N

Turf → Operating Hours: 1:N (7 per turf)

Turf → Pricing Rules: 1:N

Turf → Blocked Slots: 1:N

Turf → Bookings: 1:N

Customer → Bookings: 1:N (across any business)

Booking → Booking Hold: 1:1

Booking → Payments: **1:N** (see ADR-003)

Booking → Booking Audit: 1:N

Payment → Payment Audit: 1:N

Conversation → Messages: 1:N

Business → Reports: 1:N

---

# 23. Booking Conflict Rule

A booking conflicts when:

Same Turf

AND

Same Date

AND

Overlapping Time

AND

Status IN (`HOLD`, `PAYMENT_PENDING`, `CONFIRMED`)

This includes HOLD status (see ADR-004) to prevent double-holds.

---

# 24. Database Constraint

Unique logical booking:

```
(turf_id, booking_date, start_time, end_time)
```

Applied only to bookings with status IN (`HOLD`, `PAYMENT_PENDING`, `CONFIRMED`).

Implementation: partial unique index or exclusion constraint.

---

# 25. Booking Transaction

```text
Booking Request
    ↓
Start Transaction
    ↓
Lock Turf Slot (SELECT ... FOR UPDATE)
    ↓
Check Existing Active Booking
    ↓
Create Booking (status = HOLD)
    ↓
Create Booking Hold (expires_at = NOW() + 10 min)
    ↓
Commit
```

---

# 26. Locking Strategy

Use:

```sql
SELECT ... FOR UPDATE
```

during booking creation.

Prevents race conditions.

---

# 27. Isolation Level

Recommended:

READ COMMITTED

Critical booking operations:

Pessimistic Locking

---

# 28. Indexes

Important indexes:

```
booking(turf_id, booking_date)

booking(customer_id)

booking(business_id, status)

booking_hold(status, expires_at) — status first for equality, expires_at for range

payment(booking_id)

payment(gateway_payment_id) — for deduplication

payment(status)

users(phone)

conversation(user_id, business_id)

blocked_slot(turf_id, date)

business(whatsapp_phone_number_id)
```

---

# 29. Soft Delete

Never physically delete:

Booking

Payment

Audit

Instead:

status → `DELETED` or `INACTIVE`

---

# 30. Audit Principle

Every status change recorded.

```text
Booking Created (HOLD)
    ↓
Payment Pending
    ↓
Confirmed
    ↓
Completed
```

Each transition creates a `booking_audit` entry.

---

# 31. Reporting

Reports generated from:

Booking, Payment, Users, Business tables.

Metadata stored in: `report` table.

Files stored on: local filesystem (see ADR-012).

---

# 32. Performance

Expected MVP:

<100 Businesses

<10,000 Bookings/month

Current schema sufficient.

---

# 33. Security

Every query filters:

`business_id`

Customer never accesses another business's data.

---

# 34. Multi-Tenant Rule

Every business-owned table contains:

```
business_id
```

Tables with `business_id`:

Turf, Booking, Payment, Blocked Slot, Conversation, Notification, Report

Tables WITHOUT `business_id`:

Users (customers are global — see ADR-002), System Setting

---

# 35. Future Tables

Future additions:

```
coupon

subscription

invoice

manager_assignment

customer_wallet

loyalty_points

marketing_campaign

check_in

review

rating
```

---

# 36. Database Naming Convention

Tables: snake_case

Columns: snake_case

Primary Key: id

Foreign Keys: *_id

---

# 37. UUID Strategy

All IDs use UUID v7.

Advantages:

- Globally unique
- Secure
- Distributed-friendly

---

# 38. Timestamp Convention

All tables include:

```
created_at

updated_at
```

Optional:

deleted_at

---

# 39. Data Retention

Bookings: Never deleted.

Payments: Never deleted.

Audit: Never deleted.

Conversations: Retained according to business policy.

---

# 40. MVP Database Summary

The MVP database supports:

✓ Multi-business

✓ Multi-turf

✓ Global customers (ADR-002)

✓ Payment retries (ADR-003)

✓ Booking hold expiry (ADR-005)

✓ WhatsApp routing (ADR-006)

✓ Reports

✓ Conflict Prevention

✓ Audit Trail

✓ Notifications

✓ Future Scalability

---

# End of Document