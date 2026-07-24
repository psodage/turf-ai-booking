# Turf AI Booking — Business Rules

**Document:** 04-business-rules.md  
**Version:** 2.0  
**Status:** Approved  
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
- Blocked slots
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

```text
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
```

The AI is responsible for understanding the customer's intent.

The backend is responsible for enforcing business rules.

---

# 3. Turf Operating Hours

Each turf defines its own operating hours per day of the week.

Example:

```text
Monday–Friday:
  Opening: 06:00 AM
  Closing: 11:00 PM

Saturday–Sunday:
  Opening: 06:00 AM
  Closing: 11:00 PM
```

Rules:

- Each turf has 7 operating hour entries (one per day).
- A turf may be closed on specific days (`is_closed = true`).
- Bookings cannot be created outside operating hours.
- Operating hours are set per turf, not per business.
- A business with multiple turfs may have different hours for each.

---

# 4. Time Slots

Slots are generated dynamically based on operating hours.

Default slot duration: **60 minutes**.

Example (opening 06:00 AM, closing 11:00 PM):

```text
06:00–07:00
07:00–08:00
08:00–09:00
...
22:00–23:00
```

Rules:

- Slots do not overlap.
- Slots are contiguous (no gaps).
- The last slot must end at or before closing time.
- Slot duration is fixed at 60 minutes for MVP.
- Future: configurable slot duration per turf.

---

# 5. Booking Lifecycle

Every booking follows this state machine:

```text
HOLD → PAYMENT_PENDING → CONFIRMED → COMPLETED
         ↓                    ↓
       EXPIRED             CANCELLED
                              ↓
                           NO_SHOW
```

> **Note (ADR-013):** COMPLETED and NO_SHOW are both terminal states reached
> directly from CONFIRMED. They are mutually exclusive outcomes.

### Status Definitions

| Status | Meaning |
|--------|---------|
| HOLD | Slot temporarily reserved. Payment not yet initiated. |
| PAYMENT_PENDING | Payment link generated. Awaiting customer payment. |
| CONFIRMED | Payment verified. Booking is firm. |
| COMPLETED | Booking time has passed. Customer used the slot. |
| EXPIRED | Hold or payment timed out. Slot released. |
| CANCELLED | Cancelled by customer or owner. |
| NO_SHOW | Customer did not arrive. Marked after booking time. |

### Valid Transitions

| From | To | Trigger |
|------|----|---------|
| HOLD | PAYMENT_PENDING | Payment link generated |
| HOLD | EXPIRED | Hold timer expired |
| PAYMENT_PENDING | CONFIRMED | Payment verified via webhook |
| PAYMENT_PENDING | EXPIRED | Payment timed out |
| CONFIRMED | COMPLETED | Booking end time passed, customer arrived |
| CONFIRMED | CANCELLED | Customer or owner cancels |
| CONFIRMED | NO_SHOW | Booking end time passed, customer did not arrive (ADR-013) |

### Invalid Transitions

- EXPIRED → CONFIRMED (slot may have been rebooked)
- CANCELLED → CONFIRMED
- COMPLETED → CANCELLED
- COMPLETED → NO_SHOW (mutually exclusive — ADR-013)
- NO_SHOW → CANCELLED
- NO_SHOW → COMPLETED

---

# 6. Booking Hold

When a customer decides to book a slot, the system creates a booking hold.

Rules:

- A hold reserves the slot for 10 minutes.
- No other customer can book the same slot while a hold is active.
- The hold has an `expires_at` timestamp set to `NOW() + 10 minutes`.
- If the customer does not complete payment within 10 minutes, the hold expires and the slot is released.

### Hold Expiry Mechanism

1. **Lazy Expiry:** Every availability check and booking creation query filters by `expires_at > NOW()`. Expired holds are invisible to business logic.
2. **Cleanup Job:** A scheduled task runs every 2 minutes to mark expired holds as `EXPIRED` and release the slot.

---

# 7. Booking Conflict Prevention

A slot is considered occupied when a booking exists with:

- Same `turf_id`
- Same `booking_date`
- Overlapping `start_time` and `end_time`
- Status IN (`HOLD`, `PAYMENT_PENDING`, `CONFIRMED`)

The system must prevent two active bookings for the same turf and time.

### Database Enforcement

- Use `SELECT ... FOR UPDATE` during booking creation to prevent race conditions.
- A unique constraint or exclusion constraint on `(turf_id, booking_date, start_time, end_time)` for active bookings.
- Transaction isolation level: `READ COMMITTED` with pessimistic locking.

---

# 8. Payment Rules

- Every booking requires payment before confirmation.
- No unpaid confirmed bookings allowed in MVP.
- Payment is verified through payment gateway webhooks only.
- The AI must never confirm a booking based on customer claims.
- Price is locked when the booking hold is created.
- One booking can have multiple payment attempts (1:N relationship).
- Only the successful payment confirms the booking.

See: `docs/05-payment-rules.md` for complete payment lifecycle.

---

# 9. Cancellation Rules

### Customer Cancellation

- Customer can cancel a CONFIRMED booking.
- Cancellation is allowed if the current time is **≥ 2 hours before booking start time**.
- Cancellation is NOT allowed if < 2 hours before booking start time.
- Customer cannot cancel COMPLETED or NO_SHOW bookings.
- Customer cannot cancel another customer's booking.

### Owner Cancellation

- Owner can cancel any CONFIRMED booking for their business at any time.
- Owner cancellation always triggers a full refund.

### System Cancellation

- System may cancel bookings when a hold or payment expires.
- System cancellation transitions the booking to EXPIRED, not CANCELLED.

---

# 10. Refund Rules

| Scenario | Refund |
|----------|--------|
| Customer cancels ≥ 2 hours before start | Full refund |
| Customer cancels < 2 hours before start | No refund (cancellation denied) |
| Owner cancels booking | Full refund |
| Payment after hold expired | Full refund (booking not confirmed) |
| Duplicate payment detected | Duplicate amount refunded |

Rules:

- Partial refunds are NOT supported in MVP.
- Refunds are processed through the payment gateway.
- Every refund references: Booking ID, Payment ID, Reason.
- Refund status: `NOT_REQUIRED`, `REQUESTED`, `PROCESSING`, `SUCCESS`, `FAILED`.

---

# 11. Pricing Rules

Each turf has pricing rules that determine the slot price.

### Pricing Types

| Type | Description |
|------|-------------|
| BASE | Default price for weekday slots |
| WEEKEND | Price for Saturday and Sunday slots |
| PEAK | Price for designated peak hours |

### Price Resolution Order

When determining the price for a slot:

1. Check for PEAK pricing (specific day + time range).
2. Check for WEEKEND pricing (Saturday/Sunday).
3. Fall back to BASE pricing.

The most specific rule wins.

### Price Lock

- The price is determined and locked when the booking hold is created.
- If the owner changes pricing after a hold is created, the existing hold keeps its original price.
- New bookings use the updated price.

---

# 12. Blocked Slots

Owners can block slots to prevent customer bookings.

### Block Reasons

| Reason | Description |
|--------|-------------|
| MAINTENANCE | Turf maintenance |
| PRIVATE_EVENT | Private event or tournament |
| OWNER_USE | Owner personal use |

Rules:

- A blocked slot cannot be booked by customers.
- Owner cannot block a slot that has ANY active booking (`HOLD`, `PAYMENT_PENDING`, or `CONFIRMED`). The booking must be cancelled or expired first.
- Blocked slots appear as unavailable to customers.
- Owner can unblock a slot at any time.

---

# 13. Advance Booking Window

- Customers can book up to **30 days in advance** (default).
- Customers cannot book for past dates.
- Customers cannot book for the current time if the slot has already started.
- The advance booking window is configurable per business in the future.
- For MVP, 30 days is the system-wide default.

---

# 14. No-Show Handling

- If a customer does not arrive for a CONFIRMED booking, the owner may mark it as NO_SHOW.
- NO_SHOW bookings do not receive refunds.
- NO_SHOW status is recorded for customer tracking.
- For MVP, no-show marking is manual (owner tells AI or future dashboard).

---

# 15. Slot Availability Check

When checking availability, the system must consider:

1. Turf operating hours for the requested day.
2. Existing bookings with status IN (`HOLD`, `PAYMENT_PENDING`, `CONFIRMED`).
3. Blocked slots for the requested turf and date.
4. Booking hold expiry (`expires_at > NOW()`).

A slot is available only if all conditions are clear.

---

# 16. Alternative Slot Suggestion

If a requested slot is unavailable, the system should suggest alternative available slots.

Rules:

- Suggest up to 3 alternative slots on the same date.
- If no slots are available on the same date, inform the customer.
- Do not suggest slots outside operating hours.
- Do not suggest blocked slots.

---

# 17. Booking Source

Every booking records its source.

| Source | Description |
|--------|-------------|
| WHATSAPP_AI | Booked through WhatsApp AI agent |
| OWNER_MANUAL | Created by owner manually (future) |
| ADMIN | Created by system administrator |

For MVP, the only source is `WHATSAPP_AI`.

---

# 18. Multi-Tenant Rules

- Every booking belongs to one business.
- Every turf belongs to one business.
- A customer's data is visible to a business only through bookings.
- Business A cannot access Business B's bookings, turfs, or revenue.
- Tenant isolation is enforced at the backend service layer.
- Every business-scoped query must include `business_id` in the WHERE clause.

---

# 19. AI Agent Rules

The AI must never:

- Guess availability
- Guess pricing
- Confirm a booking without payment verification
- Bypass the cancellation window
- Access another business's data
- Modify payment status
- Generate fake booking IDs

All booking operations must go through validated backend tools.

---

# 20. Summary of Default Values

| Setting | Default Value |
|---------|--------------|
| Slot duration | 60 minutes |
| Booking hold duration | 10 minutes |
| Cancellation window | 2 hours before start |
| Advance booking window | 30 days |
| Booking reminder | 2 hours before start |
| Payment timeout | 10 minutes |
| Hold cleanup interval | 2 minutes |

These values are system-wide defaults for MVP. Future versions may allow per-business configuration.

---

# 21. Timezone Rules (ADR-019)

- Each business stores its timezone (e.g., `Asia/Kolkata`).
- All timestamps in the database are stored in UTC.
- Business rule evaluations (operating hours, cancellation window, advance booking) use the business timezone.
- The AI displays times in the business timezone to the customer.
- Slot generation uses the business timezone for determining day boundaries.

---

# 22. Rescheduling (MVP)

Rescheduling is not a separate operation in the MVP.

The customer reschedules by:

1. Cancelling the existing booking (cancellation window applies).
2. Creating a new booking for the desired slot.

The AI should guide the customer through this two-step process.

Rules:

- Cancellation refund rules apply normally.
- The new booking requires a separate payment.
- If cancellation is denied (< 2 hours before start), rescheduling is not possible.

Future: A single-step reschedule operation may be added post-MVP.

---

# End of Document