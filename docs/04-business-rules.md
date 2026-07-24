# Turf AI Booking — Business Rules

**Document:** 04-business-rules.md  
**Version:** 1.0  
**Status:** Draft / Validation Required  
**Last Updated:** 2026-07-24  
**Initial Market:** Kolhapur, Maharashtra, India  

---

# 1. Purpose

This document defines the core business rules for Turf AI Booking.

These rules determine how the system handles:

- Turf availability
- Time slots
- Bookings
- Booking holds
- Payments
- Booking conflicts
- Cancellations
- Refunds
- Rescheduling
- Blocked slots
- Maintenance
- Pricing
- No-shows
- Booking status

The backend must enforce these rules.

AI agents must never bypass these rules.

---

# 2. Core Business Principle

The booking engine is the single source of truth for turf availability.

The AI agent must not decide whether a turf is available.

Correct flow:

Customer
    ↓
AI understands request
    ↓
AI calls availability tool
    ↓
Backend booking service
    ↓
Database
    ↓
Availability result
    ↓
AI responds to customer

The AI is responsible for understanding the customer's intent.

The backend is responsible for enforcing business rules.

---

# 3. Turf Operating Hours

Each turf business must define its operating hours.

Example:

```text
Opening Time:
06:00 AM

Closing Time:
11:00 PM