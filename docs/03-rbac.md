# Turf AI Booking — Role-Based Access Control (RBAC)

**Document:** 03-rbac.md  
**Version:** 1.0  
**Status:** Draft  
**Last Updated:** 2026-07-24  

---

# 1. Purpose

This document defines the Role-Based Access Control (RBAC) model for Turf AI Booking.

RBAC determines:

- Who can access the system
- What actions each user can perform
- Which business data a user can access
- Which AI operations are allowed
- How multi-tenant data isolation is enforced

The primary security principle is:

> A user or AI agent can only access the resources and perform the actions explicitly permitted for their role and business context.

---

# 2. Roles

The initial system will have the following roles:

1. CUSTOMER
2. TURF_OWNER
3. TURF_MANAGER
4. SYSTEM_ADMIN

Future roles may include:

5. BUSINESS_STAFF
6. SUPPORT_AGENT
7. FINANCE_MANAGER

These future roles are not part of the initial MVP.

---

# 3. Role Overview

| Role | Description | Primary Interface |
|---|---|---|
| CUSTOMER | Person booking a turf | WhatsApp |
| TURF_OWNER | Owner of a turf business | WhatsApp |
| TURF_MANAGER | Staff member managing turf operations | WhatsApp |
| SYSTEM_ADMIN | Platform administrator | Admin tools / Backend |
| AI_AGENT | AI interface acting on behalf of a user | WhatsApp |

Important:

`AI_AGENT` is not a standalone human role.

The AI agent operates on behalf of an authenticated user.

For example:

Customer
    ↓
Customer AI Agent
    ↓
Customer permissions

Turf Owner
    ↓
Owner AI Agent
    ↓
Owner permissions

The AI must never receive more permissions than the user it represents.

---

# 4. Multi-Tenant Security Model

The system is multi-tenant.

Each turf business is considered a tenant.

Example:

Business A
    ├── Owner A
    ├── Manager A
    ├── Turf A1
    ├── Turf A2
    └── Bookings

Business B
    ├── Owner B
    ├── Manager B
    ├── Turf B1
    ├── Turf B2
    └── Bookings

Business A users must never access Business B data.

For example:

Owner A
    ↓
Can access
    ↓
Business A
    ├── Turf A1
    ├── Turf A2
    └── Business A bookings

Owner A
    ↓
Cannot access
    ↓
Business B
    ├── Turf B1
    └── Turf B2

Tenant isolation must be enforced at the backend level.

The AI must not be trusted to enforce tenant isolation.

---

# 5. CUSTOMER Role

The CUSTOMER role represents a person who wants to book a turf.

Customers primarily interact through WhatsApp.

---

## 5.1 Customer Permissions

Customers can:

- View public turf information
- View turf address
- View turf location
- View turf amenities
- View turf rules
- View pricing
- Check availability
- Create a booking
- Create a temporary booking hold
- Initiate payment
- View their own bookings
- Cancel their own eligible bookings
- View their own payment status
- Request booking information

Customers cannot:

- View other customers' bookings
- View business revenue
- View all bookings
- Modify turf pricing
- Block slots
- Unblock slots
- Modify turf information
- View owner information
- Access another customer's data
- Access another business's data

---

## 5.2 Customer Permission Matrix

| Action | Customer |
|---|---|
| View public turf | YES |
| View pricing | YES |
| Check availability | YES |
| Create booking | YES |
| Create payment | YES |
| View own booking | YES |
| Cancel own booking | YES, if policy allows |
| View another customer's booking | NO |
| Block slot | NO |
| Unblock slot | NO |
| Change pricing | NO |
| View revenue | NO |
| View all bookings | NO |
| Modify turf information | NO |
| Access owner data | NO |

---

# 6. TURF_OWNER Role

The TURF_OWNER role represents the owner of a turf business.

The owner has access to all operational data belonging to their own business.

The owner cannot access data belonging to another business.

---

## 6.1 Turf Owner Permissions

The owner can:

### Business Management

- View business profile
- Update business profile
- Update business contact details
- Update business address
- Update business operating hours

### Turf Management

- View turfs
- Create turf
- Update turf
- Activate turf
- Deactivate turf

### Pricing

- View pricing
- Create pricing rules
- Update pricing
- Configure peak pricing
- Configure weekend pricing

### Availability

- View availability
- Block slots
- Unblock slots
- Create maintenance blocks

### Booking Management

- View all bookings for their business
- View booking details
- Cancel bookings
- View booking status
- View payment status

### Customer Management

- View customers who booked their business
- View customer booking history related to their business

### Reports

- View revenue
- View booking statistics
- Generate Excel reports
- View cancellation statistics

---

## 6.2 Turf Owner Restrictions

The owner cannot:

- Access another business
- Access another business's customers
- Modify system-wide settings
- Modify system RBAC
- Create system administrators
- Access database credentials
- Access backend secrets
- Modify payment gateway credentials through AI

---

# 7. TURF_MANAGER Role

The TURF_MANAGER role represents a person authorized by the turf owner to manage daily operations.

The manager has limited access compared to the owner.

---

## 7.1 Turf Manager Permissions

The manager can:

- View business bookings
- View today's bookings
- View upcoming bookings
- View availability
- Block slots
- Unblock slots
- View customer booking details
- View payment status
- Cancel eligible bookings
- View basic booking statistics

---

## 7.2 Turf Manager Restrictions

The manager cannot:

- Change business ownership
- Change payment gateway configuration
- View sensitive financial configuration
- Delete the business
- Create another owner
- Manage system administrators
- Access another business
- Change subscription settings

By default, the manager cannot modify pricing.

Pricing modification should remain an owner-only permission unless explicitly delegated.

---

# 8. SYSTEM_ADMIN Role

The SYSTEM_ADMIN role represents the platform operator.

The administrator manages the overall Turf AI Booking system.

---

## 8.1 System Admin Permissions

The system administrator can:

### Business Management

- Create business
- View businesses
- Update business
- Suspend business
- Activate business

### Owner Management

- Create owner
- View owner
- Update owner
- Disable owner access

### System Monitoring

- View system logs
- View audit logs
- Monitor webhook events
- Monitor payment events
- Monitor AI tool calls

### Support

- Investigate booking issues
- Investigate payment issues
- Handle support requests

### System Configuration

- Manage system-level configuration
- Manage supported integrations

---

## 8.2 System Admin Restrictions

The system administrator should not directly modify booking or payment records without an audit trail.

Sensitive actions should be logged.

For example:

SYSTEM_ADMIN
    ↓
Changes booking status
    ↓
Audit Log
    ├── Admin ID
    ├── Action
    ├── Booking ID
    ├── Previous Value
    ├── New Value
    └── Timestamp

---

# 9. AI Agent Security Model

AI agents are interfaces.

They are not trusted administrators.

The AI must not directly access the database.

Incorrect:

AI
    ↓
PostgreSQL

Correct:

AI
    ↓
Tool Call
    ↓
Backend API
    ↓
Authentication
    ↓
Authorization
    ↓
Business Logic
    ↓
Database

---

# 10. Customer AI Agent

The customer AI agent operates with CUSTOMER permissions.

Example:

Customer:

> Is 7 PM available tomorrow?

AI
    ↓
checkAvailability()
    ↓
Backend
    ↓
Validate request
    ↓
Check database

The AI can only access information required to help the customer.

---

## 10.1 Customer AI Tools

The customer AI may have access to tools such as:

```text
get_business_info
get_turf_info
get_pricing
check_availability
create_booking_hold
create_payment_request
get_my_booking
cancel_my_booking
get_location