# Turf AI Booking — Architecture Decision Records

> This document records every architectural decision made during the design and refinement of Turf AI Booking.
>
> Each ADR explains what was decided, why, and what alternatives were rejected.

---

# ADR-001: No Web Dashboard for MVP

**Date:** 2026-07-24

**Decision:**

No web dashboard will be built for the MVP.

**Reason:**

Reduce development time and validate demand quickly.

**Consequences:**

- WhatsApp becomes the only UI.
- Owner interactions happen through AI.
- Faster onboarding.

---

# ADR-002: Customer Ownership Model

**Date:** 2026-07-24

**Decision:**

Customers are NOT scoped to a single business. In the `users` table, `business_id` is NULL for customers.

**Context:**

The original ERD required `business_id` on all users. But a customer (identified by WhatsApp phone number) should be able to book at any turf business. Scoping customers to one business would prevent cross-business bookings in a multi-tenant system.

**Resolution:**

- `users.business_id` is required for OWNER and MANAGER roles.
- `users.business_id` is NULL for CUSTOMER role.
- The customer's relationship to a business is through the `booking` table, which has both `customer_id` and `business_id`.
- Customer lookup is by phone number, not by business.

**Alternatives Rejected:**

- Separate `customer` table: Over-engineering for MVP. Creates a second user-like table with duplicate fields.
- `business_customer` join table: Adds complexity. The booking table already provides this relationship.

---

# ADR-003: Booking to Payment Relationship (1:N)

**Date:** 2026-07-24

**Decision:**

One booking can have many payment records. The relationship is 1:N.

**Context:**

The original ERD specified Booking → Payment as 1:1. However, payment retries, duplicate payments, and refunds require multiple payment records per booking. If a customer's first payment fails and they retry, the system creates a new payment record — not overwriting the old one.

**Resolution:**

- `payment.booking_id` is a foreign key to `booking.id`.
- Multiple payment rows may exist per booking.
- Only the latest payment with `status = SUCCESS` is authoritative.
- The booking is confirmed when any associated payment reaches SUCCESS.

**Alternatives Rejected:**

- Overwriting existing payment record on retry: Destroys audit history. Makes reconciliation impossible.

---

# ADR-004: Booking Status Simplification

**Date:** 2026-07-24

**Decision:**

Remove `INITIATED` status. The booking lifecycle is:

```
HOLD → PAYMENT_PENDING → CONFIRMED → COMPLETED
         ↓                    ↓
       EXPIRED             CANCELLED
                              ↓
                           NO_SHOW
```

**Context:**

The original ERD included both `INITIATED` and `PAYMENT_PENDING`, creating ambiguity. The booking conflict rule only checked `PAYMENT_PENDING` and `CONFIRMED`, meaning an `INITIATED` booking would not block the slot.

**Resolution:**

- `HOLD`: Booking hold created. Slot is reserved. Payment link not yet generated or customer has not started payment.
- `PAYMENT_PENDING`: Payment link generated. Awaiting payment.
- `CONFIRMED`: Payment verified. Booking is firm.
- `COMPLETED`: Booking time has passed and customer used the slot.
- `EXPIRED`: Hold or payment timed out. Slot released.
- `CANCELLED`: Cancelled by customer or owner.
- `NO_SHOW`: Customer did not arrive. Marked after booking time passes.

Conflict rule now checks: `status IN (HOLD, PAYMENT_PENDING, CONFIRMED)`.

**Alternatives Rejected:**

- Keeping INITIATED: Ambiguous purpose. Every booking starts as HOLD.

---

# ADR-005: Booking Hold Expiry Mechanism

**Date:** 2026-07-24

**Decision:**

Use lazy expiry combined with a scheduled cleanup task.

**Context:**

Booking holds expire after 10 minutes. The mechanism was unspecified.

**Resolution:**

1. **Primary (Lazy Expiry):** Every availability check and booking creation query includes `WHERE expires_at > NOW()` or `WHERE status != 'EXPIRED'`. Expired holds are invisible to business logic.
2. **Secondary (Cleanup Job):** A Spring Boot `@Scheduled` task runs every 2 minutes to find holds where `expires_at < NOW() AND status IN ('HOLD', 'PAYMENT_PENDING')`, marks them as `EXPIRED`, and releases the slot.

**Alternatives Rejected:**

- Redis TTL: Adds infrastructure complexity. Unnecessary for MVP with <50 bookings/day.
- Database scheduled events: Not portable across PostgreSQL versions and hosting providers.
- Event-driven expiry (message queue): Over-engineering for MVP.

---

# ADR-006: WhatsApp to Business Routing

**Date:** 2026-07-24

**Decision:**

Each turf business has one dedicated WhatsApp Business phone number. The system maps `whatsapp_phone_number_id` to `business_id`.

**Context:**

When a customer sends a WhatsApp message, the webhook includes the `phone_number_id` of the receiving WhatsApp Business account. The system must determine which business the message is for.

**Resolution (MVP):**

- The MVP supports one turf owner with one WhatsApp number.
- The `whatsapp_phone_number_id` is stored as a field on the `business` table.
- The webhook handler uses this field to resolve the business.

**Resolution (Future Multi-Tenant):**

- Multiple businesses each have their own WhatsApp Business number.
- The `business.whatsapp_phone_number_id` field handles the lookup.

**Alternatives Rejected:**

- Shared WhatsApp number for all businesses: Impossible. Meta requires separate numbers per business.
- Separate webhook endpoint per business: Unnecessary. One endpoint can route based on the phone_number_id in the payload.

---

# ADR-007: Remove n8n from MVP

**Date:** 2026-07-24

**Decision:**

n8n is removed from the MVP architecture. All automation is handled by Spring Boot.

**Context:**

n8n was referenced in the architecture for notifications, reminders, and scheduled reports. However, its responsibilities were never clearly defined, and it adds operational complexity.

**Resolution:**

- Booking reminders: Spring Boot `@Scheduled` task.
- Notification delivery: Spring Boot WhatsApp service.
- Scheduled reports: Spring Boot `@Scheduled` task + Apache POI.
- All workflows run within the Spring Boot application.

**Consequences:**

- One fewer service to deploy, monitor, and maintain.
- Simpler architecture for the pilot.
- n8n can be reintroduced post-MVP for complex multi-step workflows if needed.

**Alternatives Rejected:**

- Keeping n8n as optional: Creates ambiguity in the architecture. Developers won't know what to build where.

---

# ADR-008: Pricing Tiers Standardized

**Date:** 2026-07-24

**Decision:**

The pricing hypothesis is ₹999/month (Starter) and ₹1,999/month (Growth).

**Context:**

The business model doc specified ₹999/₹1,999. The roadmap doc specified ₹499/₹999. These are conflicting.

**Resolution:**

- ₹999/₹1,999 is the standard hypothesis from the business model.
- Early adopter or pilot pricing may offer discounts (₹499/₹999) but this is a promotional price, not the base price.
- All documents now reference ₹999/₹1,999 as the hypothesis.

---

# ADR-009: Pilot Size Standardized

**Date:** 2026-07-24

**Decision:**

The initial MVP pilot targets 1 turf owner in Kolhapur.

**Context:**

Different documents specified different pilot sizes: 1, 1–3, and 5 turf owners. This created confusion about MVP scope.

**Resolution:**

- MVP pilot: 1 turf owner.
- Post-MVP validation: Expand to 3 turf owners.
- Local expansion: Grow to 5–10 turf owners.
- All documents now consistently reference 1 turf owner for the MVP pilot.

---

# ADR-010: Cancellation and Refund Rules

**Date:** 2026-07-24

**Decision:**

Concrete cancellation rules defined for MVP:

- Customer cancels ≥ 2 hours before booking start: Full refund.
- Customer cancels < 2 hours before booking start: No refund.
- Owner cancels at any time: Full refund to customer.
- Booking with status COMPLETED: Cannot be cancelled.
- Booking with status NO_SHOW: Cannot be cancelled.
- Partial refunds: Not supported in MVP.

**Context:**

The cancellation conversation doc mentioned 100%, 50%, and 0% refund levels, but no document defined the rules for which percentage applies when. The business rules doc was truncated.

**Resolution:**

- The 2-hour cancellation window is the default. It is configurable per business in the future.
- For MVP, the 2-hour window is a system-wide default.
- 50% partial refund is removed from MVP scope to keep payment logic simple.

---

# ADR-011: Tool Name Convention

**Date:** 2026-07-24

**Decision:**

All AI tool names use camelCase.

**Context:**

Tool names were inconsistent across documents. Some used snake_case (`check_availability`, `get_business_info`), others used camelCase (`checkAvailability()`, `createBookingHold()`).

**Resolution:**

- All tool names follow camelCase: `checkAvailability`, `createBookingHold`, `createPaymentLink`, etc.
- This aligns with Java method naming conventions used in the Spring Boot backend.

---

# ADR-012: Report Storage Strategy

**Date:** 2026-07-24

**Decision:**

For MVP, Excel reports are stored as temporary files on the local filesystem.

**Context:**

The reporting docs mentioned "Cloud Storage → Temporary Download URL" but no concrete storage decision was made.

**Resolution:**

- Reports are generated to a local directory (e.g., `/data/reports/{business_id}/`).
- Reports are sent directly via WhatsApp document message after generation.
- Old reports are cleaned up after 7 days by a scheduled task.
- The `report` table stores metadata (business_id, type, generated_at, file_path).

**Alternatives Rejected:**

- Cloud storage (S3): Unnecessary for one turf owner. Adds AWS dependency.
- In-memory generation only: No ability to re-send reports if WhatsApp delivery fails.

---

---

# ADR-013: Booking State Machine Fix — NO_SHOW from CONFIRMED

**Date:** 2026-07-24

**Decision:**

Both `COMPLETED` and `NO_SHOW` are terminal states reached from `CONFIRMED`. The transition `COMPLETED → NO_SHOW` is removed.

**Context:**

The original state machine allowed `COMPLETED → NO_SHOW`, but these are mutually exclusive outcomes. COMPLETED means the customer used the slot. NO_SHOW means they did not arrive.

**Resolution:**

```text
CONFIRMED → COMPLETED (customer arrived, slot used)
CONFIRMED → NO_SHOW   (customer did not arrive)
```

Both are final states. Neither can transition further.

---

# ADR-014: Simplified Booking Hold

**Date:** 2026-07-24

**Decision:**

The `booking_hold` table stores only `(id, booking_id, expires_at, status, created_at)`. Slot data (turf_id, booking_date, start_time, end_time) is NOT duplicated — it lives only on the `booking` table.

**Context:**

The original design duplicated slot fields on both `booking` and `booking_hold`. This creates a risk of data inconsistency. Since `booking_hold.booking_id` is 1:1 with the booking, all slot data can be read from the booking.

**Consequences:**

- Conflict detection queries join through `booking` only.
- Partial unique index for conflict prevention applies to the `booking` table.
- Simpler schema, no denormalization risk.

---

# ADR-015: Webhook Replay Protection

**Date:** 2026-07-24

**Decision:**

All webhook handlers validate the event timestamp. Events older than 5 minutes are rejected.

**Context:**

Signature verification prevents tampering but not replay attacks. A captured valid payload can be replayed later with the same valid signature.

**Resolution:**

- WhatsApp webhook: check message timestamp from payload. Reject if older than 5 minutes.
- Razorpay webhook: check `created_at` from the event payload. Reject if older than 5 minutes.
- Combined with existing message deduplication (whatsapp_message_id) and payment deduplication (gateway_payment_id).

---

# ADR-016: Payment Webhook Grace Period

**Date:** 2026-07-24

**Decision:**

When a payment webhook arrives for a booking whose hold just expired (within the last 60 seconds), the system attempts to re-acquire the slot instead of immediately refunding.

**Context:**

A race condition exists between the hold expiry cleanup (every 2 min) and the Razorpay webhook. If the cleanup marks a hold as EXPIRED seconds before the payment webhook arrives, the customer loses their booking despite having paid.

**Resolution:**

1. Payment webhook handler receives successful payment.
2. If booking status is `EXPIRED` and `hold.expires_at` was within the last 60 seconds:
   - Check if the slot has been rebooked by another customer.
   - If slot is still free: reactivate hold, confirm booking.
   - If slot is taken: initiate refund.
3. If booking has been expired for more than 60 seconds: initiate refund.

---

# ADR-017: WhatsApp Template Messages for Proactive Outbound

**Date:** 2026-07-24

**Decision:**

All proactive outbound messages (sent outside an active conversation) must use WhatsApp-approved template messages.

**Context:**

Meta's WhatsApp Business Platform enforces a 24-hour messaging window. After 24 hours from the customer's last message, only pre-approved template messages can be sent. Booking reminders, confirmations after async webhook processing, and cancellation notifications may fall outside this window.

**Resolution:**

- Booking confirmation (after payment webhook): Template message.
- Booking reminder (2 hours before): Template message.
- Cancellation confirmation: Template message.
- Owner notifications: Template message.
- Responses within an active conversation: Free-form text (within 24h window).

Templates must be submitted to Meta for approval during Phase 4.

---

# ADR-018: Conversation Concurrency Control

**Date:** 2026-07-24

**Decision:**

Use `SELECT ... FOR UPDATE` on the conversation row before processing a message. Messages for the same conversation serialize. Messages for different conversations process in parallel.

**Context:**

When a customer sends rapid messages, the WhatsApp webhook fires multiple times. Without serialization, two AI calls could operate on stale conversation context, producing duplicate bookings or contradictory responses.

**Resolution:**

```text
Webhook received
    ↓
Start transaction
    ↓
SELECT conversation FOR UPDATE WHERE user_id = ? AND business_id = ?
    ↓
Process message (AI call, tool execution)
    ↓
Update conversation state
    ↓
Commit transaction
```

This is a simple pessimistic lock — no Redis, no distributed locking.

---

# ADR-019: Business Timezone

**Date:** 2026-07-24

**Decision:**

Each business stores its timezone. All timestamps in the database are UTC. Business rules evaluate times in the business timezone.

**Context:**

Operating hours, slot generation, cancellation windows, and advance booking calculations all depend on local time. The MVP targets Kolhapur (IST, UTC+5:30), but the schema should support future multi-timezone expansion.

**Resolution:**

- `business.timezone` field added (e.g., `Asia/Kolkata`).
- Database stores all timestamps in UTC.
- Service layer converts to business timezone for business rule evaluation.
- API responses include UTC timestamps. The AI formats local times for the customer.

---

# End of Document