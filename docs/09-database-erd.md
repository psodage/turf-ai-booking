# Turf AI Booking — Database & ER Diagram

**Document:** 09-database-erd.md
**Version:** 1.0
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
   ├──────── Owner(User)
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
   │                        ├──────── Payment
   │                        ├──────── Booking Audit
   │                        └──────── Customer(User)
   │
   └──────── Excel Reports
```

---

# 4. Database Schema

Schema:

public

Future:

Separate schema per tenant (optional)

---

# 5. Core Tables

Core entities:

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

status

created_at

updated_at
```

Relationship

Business

↓

Many Turfs

↓

Many Users

---

# 7. USERS

Represents:

- Customer
- Owner
- Manager
- Admin

Fields

```
id

business_id

name

phone

email

role

language

status

created_at
```

Role

```
CUSTOMER

OWNER

MANAGER

ADMIN
```

---

# 8. TURF

Represents one playable turf.

Fields

```
id

business_id

name

type

capacity

status

created_at
```

Examples

Turf 1

5v5

Turf 2

7v7

---

# 9. OPERATING HOURS

Stores daily opening hours.

Fields

```
id

turf_id

day_of_week

opening_time

closing_time

is_closed
```

One Turf

↓

Many Operating Hour records

---

# 10. PRICING RULE

Stores pricing.

Fields

```
id

turf_id

pricing_type

day

start_time

end_time

amount
```

Pricing Types

BASE

WEEKEND

PEAK

SPECIAL_DATE

---

# 11. BOOKING

Central table.

Fields

```
id

booking_number

business_id

turf_id

customer_id

booking_date

start_time

end_time

price

status

booking_source

created_at

updated_at
```

Status

```
INITIATED

PAYMENT_PENDING

CONFIRMED

CANCELLED

COMPLETED

NO_SHOW

EXPIRED
```

---

# 12. BOOKING HOLD

Temporary slot reservation.

Fields

```
id

booking_id

expires_at

status
```

Status

ACTIVE

EXPIRED

---

# 13. PAYMENT

Stores payment.

Fields

```
id

booking_id

gateway

gateway_order_id

gateway_payment_id

amount

currency

status

created_at
```

Status

CREATED

PENDING

SUCCESS

FAILED

REFUNDED

---

# 14. BLOCKED SLOT

Owner blocked periods.

Fields

```
id

turf_id

date

start_time

end_time

reason
```

Reason

MAINTENANCE

PRIVATE_EVENT

OWNER_USE

---

# 15. CONVERSATION

Stores WhatsApp conversations.

Fields

```
id

user_id

business_id

role

current_intent

status

last_activity
```

---

# 16. CONVERSATION MESSAGE

Stores messages.

Fields

```
id

conversation_id

sender

message

message_type

whatsapp_message_id

created_at
```

---

# 17. NOTIFICATION

Stores notifications.

Fields

```
id

user_id

booking_id

channel

status

sent_at
```

---

# 18. BOOKING AUDIT

Booking history.

Fields

```
id

booking_id

old_status

new_status

changed_by

changed_at
```

---

# 19. PAYMENT AUDIT

Payment events.

Fields

```
id

payment_id

event

gateway_payload

created_at
```

---

# 20. REPORT

Generated reports.

Fields

```
id

business_id

report_type

file_path

generated_at
```

---

# 21. SYSTEM SETTING

Stores configurable values.

Fields

```
key

value

description
```

---

# 22. Relationships

Business

↓

Users

1:N

Business

↓

Turfs

1:N

Turf

↓

Bookings

1:N

Customer

↓

Bookings

1:N

Booking

↓

Payment

1:1

Booking

↓

Booking Audit

1:N

Conversation

↓

Messages

1:N

---

# 23. Booking Conflict Rule

A booking conflicts when:

Same Turf

AND

Same Date

AND

Overlapping Time

AND

Status IN

```
PAYMENT_PENDING

CONFIRMED
```

---

# 24. Database Constraint

Unique logical booking:

```
(turf_id,
booking_date,
start_time,
end_time)
```

Applied only to active bookings.

---

# 25. Booking Transaction

Booking Request

↓

Start Transaction

↓

Lock Turf Slot

↓

Check Existing Booking

↓

Create Hold

↓

Commit

---

# 26. Locking Strategy

Use:

```
SELECT ...

FOR UPDATE
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
booking

(turf_id,
booking_date)

booking(customer_id)

payment(status)

users(phone)

conversation(user_id)

blocked_slot(turf_id,date)
```

---

# 29. Soft Delete

Never physically delete:

Booking

Payment

Audit

Instead:

status

↓

DELETED

or

INACTIVE

---

# 30. Audit Principle

Every status change recorded.

Booking Created

↓

Hold

↓

Payment

↓

Confirmed

↓

Completed

---

# 31. Excel Reporting

Reports generated from:

Booking

Payment

Users

Business

Stored in:

report table

---

# 32. Performance

Expected MVP:

<100 Businesses

<10,000 Bookings/month

Current schema sufficient.

---

# 33. Security

Every query filters:

business_id

Customer never accesses another business.

---

# 34. Multi-Tenant Rule

Every business-owned table contains:

```
business_id
```

Examples:

Booking

Turf

Users

Pricing

Reports

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

Tables:

snake_case

Columns:

snake_case

Primary Key:

id

Foreign Keys:

*_id

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

Bookings:

Never deleted.

Payments:

Never deleted.

Audit:

Never deleted.

Conversation:

Retained according to business policy.

---

# 40. MVP Database Summary

The MVP database supports:

✓ Multi-business

✓ Multi-turf

✓ WhatsApp AI

✓ Payments

✓ Reports

✓ Booking Holds

✓ Conflict Prevention

✓ Audit Trail

✓ Notifications

✓ Future Scalability

---

# 41. Next Document

The next document is:

docs/10-excel-report.md

This document defines:

- Excel workbook structure
- Daily reports
- Monthly reports
- Revenue reports
- Booking summaries
- Owner exports
- AI-generated Excel files