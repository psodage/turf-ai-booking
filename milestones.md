# Turf AI Booking — Milestone Plan

**Project:** Turf AI Booking  
**Phase:** Implementation  
**Total Milestones:** 18  
**Estimated Duration:** ~38 working days  

> Each milestone is 1–3 days, independently testable, and ends with a working feature.  
> Milestones build on each other but each one has a clear, verifiable end state.

---

## Phase 1 — Foundation

---

### M01 · Project Boots ✦ 1 day

**Goal:** A developer can clone the repo, run one command, and see a healthy Spring Boot app.

**Tasks:**
- Initialize Spring Boot 3.5 project in `backend/`
- Configure Maven with all dependencies (Web, JPA, Security, Validation, Actuator, Flyway, PostgreSQL, Lombok)
- Set up `application.yml` with `dev`, `staging`, `prod` profiles
- Create `docker-compose.yml` with PostgreSQL container
- Expand `AGENTS.md` with package structure and naming conventions
- Expand `.env.example` with all required variables
- Add local setup instructions to `README.md`
- Delete empty `n8n/` directory

**Definition of Done:**
```
docker-compose up -d
./mvnw spring-boot:run --spring.profiles.active=dev
GET /actuator/health → 200 {"status": "UP"}
```

---

### M02 · Developer Standards ✦ 1 day

**Goal:** Every coding decision for the project has a documented answer. No developer stops to guess.

**Tasks:**
- Create `BaseEntity` (`@MappedSuperclass` with UUID id, `created_at`, `updated_at`, `@PrePersist`)
- Create `GlobalExceptionHandler` (`@ControllerAdvice`) — returns the standard error JSON format for all exceptions
- Create `CorrelationIdFilter` — adds `correlationId` to every request/response and MDC
- Create the exception class hierarchy:
  - `BookingNotFoundException`, `SlotUnavailableException`, `HoldExpiredException`
  - `CancellationDeniedException`, `OutsideOperatingHoursException`
  - `PaymentVerificationException`, `DuplicatePaymentException`
  - `UnauthorizedBusinessAccessException`, `WebhookSignatureException`, `WebhookReplayException`
- Configure structured JSON logging (Logstash encoder or equivalent)

**Definition of Done:**
```
POST /api/v1/test/invalid-request (with bad body)
→ 422 { "error": { "code": "VALIDATION_FAILED", "fields": [...] } }

Trigger SlotUnavailableException
→ 409 { "error": { "code": "SLOT_UNAVAILABLE", ... } }

Log line includes correlationId, timestamp, level as JSON fields.
```

---

## Phase 2 — Database

---

### M03 · Database Schema ✦ 2 days

**Goal:** All 16 tables exist in PostgreSQL with correct constraints, indexes, and relationships.

**Tasks:**
- Write Flyway migrations in order (V1–V17):
  - V1: `business` (with `timezone` field)
  - V2: `users` (nullable `business_id` for customers)
  - V3: `turf`
  - V4: `operating_hours`
  - V5: `pricing_rule`
  - V6: `booking_number_seq` (PostgreSQL SEQUENCE)
  - V7: `booking` (with `cancelled_at`, `cancelled_by`)
  - V8: `booking_hold` (simplified — ADR-014)
  - V9: `payment`
  - V10: `blocked_slot`
  - V11: `conversation`
  - V12: `conversation_message`
  - V13: `notification`
  - V14: `booking_audit`
  - V15: `payment_audit`
  - V16: `report`
  - V17: `system_setting`
- Add partial unique index on `booking (turf_id, booking_date, start_time, end_time) WHERE status IN ('HOLD', 'PAYMENT_PENDING', 'CONFIRMED')`
- Add all indexes from ERD section 28

**Definition of Done:**
```
./mvnw spring-boot:run
→ Flyway runs 17 migrations with no errors
→ All tables exist in PostgreSQL with correct columns
→ psql: \d booking shows the partial unique index
→ psql: \d booking_hold shows simplified schema (no slot fields)
```

---

### M04 · JPA Entities & Repositories ✦ 2 days

**Goal:** All entities are mapped, all repositories are ready. The data layer is complete.

**Tasks:**
- Create JPA entities for all 16 tables (extending `BaseEntity`)
- Create Spring Data JPA repositories for each entity
- Map all relationships (`@OneToMany`, `@ManyToOne`, `@OneToOne`)
- Write seed data migration `V18__seed_dev_data.sql`:
  - 1 business (Kolhapur turf, IST timezone)
  - 1 owner user
  - 1 turf (5v5)
  - 7 operating hours rows (Mon–Sun, 06:00–23:00)
  - 3 pricing rules (BASE, WEEKEND, PEAK)
- Verify `@Column` constraints match DDL (nullable, unique, length)

**Definition of Done:**
```
./mvnw test (repository layer tests with Testcontainers)
→ businessRepository.findById(seedId) → returns seeded business
→ bookingRepository.findByTurfId(turfId) → returns empty list
→ All relationships load without LazyInitializationException in tests
```

---

## Phase 3 — Booking Engine

---

### M05 · Slot Availability ✦ 2 days

**Goal:** The system can answer "Is turf X available on date Y at time Z?"

**Tasks:**
- `SlotService.generateSlots(turf, date)` — generates 60-min slots from operating hours
- `SlotService.getAvailableSlots(turf, date)` — filters out booked/blocked/held/expired slots
- `AvailabilityService.isSlotAvailable(turfId, date, startTime, endTime)` — single slot check
- `AvailabilityService.suggestAlternatives(turfId, date, startTime)` — returns up to 3 nearby slots
- Conflict check: queries bookings WHERE status IN ('HOLD', 'PAYMENT_PENDING', 'CONFIRMED') AND expires_at > NOW()
- Timezone conversion using `business.timezone` (ADR-019)

**Definition of Done:**
```
GET /api/v1/turfs/{turfId}/availability?date=2026-07-26
→ 200 { "slots": [{ "startTime": "06:00", "endTime": "07:00", "available": true }, ...] }

Manually insert a CONFIRMED booking for 7PM slot
GET /api/v1/turfs/{turfId}/availability?date=2026-07-26
→ 7PM slot shows available: false
→ Suggestions include 6PM and 8PM
```

---

### M06 · Booking Hold & Lifecycle ✦ 2 days

**Goal:** A booking can be created (HOLD) and transition through its complete state machine.

**Tasks:**
- `BookingService.createHold(customerId, turfId, date, startTime)`:
  - Check slot available (pessimistic lock: `SELECT ... FOR UPDATE`)
  - Create booking (status=HOLD)
  - Create booking_hold (expires_at = NOW() + 10 min)
  - Lock price from pricing rules
  - Generate booking_number from sequence (`BK-2026-00001`)
- `BookingService.confirmBooking(bookingId)` — HOLD → CONFIRMED
- `BookingService.expireBooking(bookingId)` — HOLD/PAYMENT_PENDING → EXPIRED
- `BookingService.cancelBooking(bookingId, cancelledBy)`:
  - Validate cancellation window (≥ 2 hours before start, in business timezone)
  - CONFIRMED → CANCELLED
  - Set `cancelled_at`, `cancelled_by`
  - Write `booking_audit` entry for every transition
- Owner cancel: bypasses 2-hour window check
- `HoldExpiryService` (`@Scheduled` every 2 min): mark expired holds

**Definition of Done:**
```
POST /api/v1/bookings/hold (turfId, date, startTime)
→ 201 { bookingId, bookingNumber "BK-2026-00001", status "HOLD", expiresAt, price }

Same request again (concurrent)
→ 409 { "error": { "code": "SLOT_UNAVAILABLE" } }

POST /api/v1/bookings/{id}/cancel (3h before start)
→ 200 { status: "CANCELLED" }

POST /api/v1/bookings/{id}/cancel (1h before start)
→ 422 { "error": { "code": "CANCELLATION_DENIED" } }

Wait 10 minutes (or mock time): hold auto-expires, slot becomes available again.
```

---

### M07 · Slot Blocking ✦ 1 day

**Goal:** An owner can block and unblock slots. Blocked slots appear unavailable to customers.

**Tasks:**
- `BlockedSlotService.blockSlot(turfId, date, startTime, endTime, reason, blockedBy)`:
  - Reject if slot has HOLD, PAYMENT_PENDING, or CONFIRMED booking
- `BlockedSlotService.unblockSlot(blockedSlotId, ownerId)`
- Update availability query to exclude blocked slots
- Authorization: only OWNER/MANAGER of that business can block

**Definition of Done:**
```
POST /api/v1/turfs/{turfId}/blocked-slots
→ 201 (blocked slot created)

GET /api/v1/turfs/{turfId}/availability?date=...
→ Blocked slot shows available: false

POST /api/v1/bookings/hold (same slot)
→ 409 SLOT_UNAVAILABLE

DELETE /api/v1/turfs/{turfId}/blocked-slots/{id}
→ 200 (unblocked)

GET /api/v1/turfs/{turfId}/availability?date=...
→ Slot shows available: true again
```

---

## Phase 4 — WhatsApp

---

### M08 · Webhook Foundation ✦ 2 days

**Goal:** The system receives WhatsApp messages, identifies the business and user, and returns an echo response.

**Tasks:**
- `GET /webhook/whatsapp` — Meta verification (verify_token check)
- `POST /webhook/whatsapp` — receive messages:
  - Verify `X-Hub-Signature-256` (HMAC-SHA256 against `WHATSAPP_APP_SECRET`)
  - Validate message timestamp — reject if > 5 minutes old (ADR-015)
  - Deduplicate by `whatsapp_message_id`
  - Route business via `phone_number_id` → `business_id` (ADR-006)
  - Identify or create user by `from` phone number
  - Apply conversation lock: `SELECT conversation FOR UPDATE` (ADR-018)
  - Echo message back via WhatsApp send API
- `WhatsAppClient.sendMessage(phoneNumberId, to, text)` — sends a text reply
- `WhatsAppClient.sendTemplateMessage(phoneNumberId, to, templateName, params)` — template sender

**Definition of Done:**
```
Configure Meta webhook to point to your ngrok URL
Customer sends "Hello" on WhatsApp
→ System receives webhook
→ Logs show: businessId resolved, userId created/found, correlationId present
→ Customer receives echo: "Hello" sent back

Send same message twice (duplicate whatsapp_message_id)
→ Second is silently dropped (no double echo)

Send message with timestamp > 5 min old (manually set)
→ Webhook returns 400, message not processed
```

---

### M09 · Conversation Context ✦ 1 day

**Goal:** Every message arrives with full context — who is speaking, what business, what they were doing.

**Tasks:**
- `ConversationService.getOrCreate(userId, businessId)` — finds or creates active conversation
- `ConversationService.saveMessage(conversationId, sender, content, whatsappMessageId)`
- `ConversationService.buildContext(conversationId)` — returns last 10 messages + current intent + active booking ID
- Conversation timeout: if `last_activity` > 30 minutes ago, start fresh
- `ConversationService.closeExpired()` — `@Scheduled` to close stale conversations

**Definition of Done:**
```
Customer sends 3 messages
→ All 3 saved in conversation_message table
→ ConversationContext contains last 3 messages, correct userId and businessId

Customer is silent for 31 minutes
→ Conversation status set to EXPIRED
→ Next message creates a new ACTIVE conversation
```

---

## Phase 5 — AI Agent

---

### M10 · AI Integration Foundation ✦ 2 days

**Goal:** A message enters the AI pipeline, goes through intent detection, and returns a coherent text response.

**Tasks:**
- Choose and configure AI library (Spring AI or direct SDK)
- Create `AIOrchestrator` — routes to CustomerAI or OwnerAI based on user role
- Build `ConversationContextBuilder` — assembles system prompt:
  1. Role definition (business name, address)
  2. Conversation rules (never guess, use tools)
  3. Error code handling instructions
  4. Last 10 messages
  5. Current user message
- Enforce token budget (max 2000 tokens per call)
- Log token usage per conversation
- `CustomerAI.respond(context)` → returns AI text response
- Return AI response via WhatsApp

**Definition of Done:**
```
Customer sends "What are your available slots tomorrow?"
→ AI receives context (business name, conversation history)
→ AI responds with a natural language reply
→ Response is sent back via WhatsApp
→ Token count logged for the call

No tool calls yet — AI can only do free-form conversation at this stage.
```

---

### M11 · Customer Booking Flow (AI + Tools) ✦ 3 days

**Goal:** A customer can fully book a turf through WhatsApp using natural language.

**Tasks:**
- Register AI tools (Customer AI):
  - `checkAvailability(date, startTime)` → returns available slots
  - `getAvailableTurfs()` → returns turf list with pricing
  - `createBookingHold(turfId, date, startTime)` → creates HOLD, returns hold details
  - `createPaymentLink(bookingId)` → creates Razorpay Payment Link, returns URL
  - `getMyBookings()` → returns customer's upcoming bookings
  - `getPricing(turfId)` → returns pricing rules
  - `getLocation()` → returns Google Maps link
- All tools validate `ToolContext` (userId, businessId, role) before executing
- AI tool errors return standard `{success, error_code, message, suggestions}` format
- Test complete happy-path conversation:
  > "Book tomorrow at 7 PM" → availability check → confirm slot → hold created → payment link sent

**Definition of Done:**
```
WhatsApp conversation (real or simulated):
  Customer: "Hi, I want to book a slot tomorrow evening"
  AI: "Sure! We have slots available tomorrow. Here are the options: ..."
  Customer: "7 PM please"
  AI: (calls createBookingHold) "Perfect! I've reserved the 7–8 PM slot for you.
       Price: ₹800. Please pay within 10 minutes: [payment link]"
  
→ booking row exists in DB with status=HOLD
→ booking_hold row exists with expires_at = now + 10min
→ payment row exists with status=CREATED
```

---

### M12 · Owner AI Flow ✦ 2 days

**Goal:** The turf owner can ask questions and get answers through WhatsApp.

**Tasks:**
- Register AI tools (Owner AI):
  - `getTodayBookings()` → returns today's confirmed bookings
  - `getUpcomingBookings(days)` → next N days
  - `blockSlot(date, startTime, endTime, reason)` → blocks slot
  - `unblockSlot(slotId)` → unblocks
  - `getRevenue(period)` → today/week/month revenue total
  - `getBookingStatistics(period)` → counts by status
  - `updatePricing(turfId, type, amount)` → updates pricing rule
- `generateExcelReport` handled in M16

**Definition of Done:**
```
Owner WhatsApp conversation:
  Owner: "Show me today's bookings"
  AI: (calls getTodayBookings) "You have 3 bookings today:
      1. Rahul Patil — 7 PM (Confirmed)
      2. ..."

  Owner: "Block 9 PM tomorrow for maintenance"
  AI: (calls blockSlot) "Done. 9–10 PM tomorrow is blocked for maintenance."

→ blocked_slot row exists in DB
→ GET availability for that slot shows it unavailable
```

---

## Phase 6 — Payments

---

### M13 · Payment Link & Webhook ✦ 2 days

**Goal:** A payment link is sent to the customer, and a successful payment confirms the booking.

**Tasks:**
- `PaymentService.createPaymentLink(booking)`:
  - Call Razorpay Payment Links API
  - Store payment record (status=CREATED)
  - Return payment link URL
- `POST /webhook/razorpay`:
  - Verify Razorpay webhook signature
  - Validate event timestamp (> 5 min → 400, ADR-015)
  - Deduplicate by `gateway_payment_id`
  - Handle event `payment_link.paid`
  - Update payment status → SUCCESS
  - Confirm booking: HOLD/PAYMENT_PENDING → CONFIRMED
  - Release booking hold (status → CONVERTED)
  - Send booking confirmation template message to customer
  - Send owner notification template message

**Definition of Done:**
```
Full booking + payment flow:
1. AI creates hold + payment link
2. Manually trigger Razorpay webhook (test mode): payment_link.paid
3. → payment.status = SUCCESS
4. → booking.status = CONFIRMED
5. → booking_hold.status = CONVERTED
6. → Customer receives WhatsApp template: "Your booking BK-2026-00001 is confirmed!"
7. → Owner receives WhatsApp template: "New booking: Rahul Patil, 7 PM"

Send same webhook twice (same gateway_payment_id)
→ Second is ignored (booking stays CONFIRMED, no double processing)
```

---

### M14 · Payment Failure & Grace Period ✦ 2 days

**Goal:** All payment failure paths are handled correctly. No customer loses money due to timing edge cases.

**Tasks:**
- Handle `payment.failed` webhook event:
  - Update payment → FAILED
  - Booking stays in PAYMENT_PENDING (customer can retry)
  - Notify customer: payment failed, here is a new link (if hold still valid)
- Handle hold-expired-before-webhook case (ADR-016):
  - If booking is EXPIRED and hold expired within 60 seconds:
    - Check if slot was rebooked
    - If free: reactivate hold, confirm booking
    - If taken: initiate Razorpay refund
  - If expired > 60 seconds: initiate refund
- `PaymentService.initiateRefund(paymentId, reason)` → Razorpay Refund API
- Handle refund webhook events → update `refund_status`
- Handle duplicate payment (booking already CONFIRMED): immediate refund of duplicate amount

**Definition of Done:**
```
Simulate payment failure:
→ Razorpay test: payment_link.paid with FAILED status
→ Customer receives: "Payment failed. Here's a new link: [url]"
→ booking.status stays PAYMENT_PENDING

Simulate grace period:
→ Manually expire booking hold
→ Send payment success webhook within 60 seconds
→ Slot still free: booking confirmed, customer gets confirmation
→ Slot rebooked: refund initiated

Simulate expired payment (> 10 min, > 60s grace):
→ payment_link.paid arrives late
→ Refund initiated automatically
→ booking.status stays EXPIRED
```

---

## Phase 7 — Cancellation & Reports

---

### M15 · Cancellation & Refund Flow ✦ 1 day

**Goal:** A customer can cancel through WhatsApp. The cancellation window is enforced. Refunds are processed.

**Tasks:**
- Register `cancelBooking(bookingId)` AI tool for customers:
  - Validate: customer owns this booking
  - Validate: booking is CONFIRMED
  - Validate: current time is ≥ 2 hours before start (in business timezone, ADR-019)
  - CONFIRMED → CANCELLED
  - Set `cancelled_at`, `cancelled_by`
  - Write `booking_audit` entry
  - Initiate Razorpay refund
  - Send cancellation confirmation template to customer
- AI guides rescheduling as cancel + rebook (documented in business rules)

**Definition of Done:**
```
WhatsApp conversation:
  Customer: "Cancel my booking for tomorrow 7 PM"
  AI: (calls cancelBooking) "Your booking BK-2026-00001 has been cancelled.
      Your refund of ₹800 will be processed in 3-5 business days."

→ booking.status = CANCELLED
→ payment.refund_status = REQUESTED
→ Razorpay refund API called

Attempt cancellation 1 hour before:
  AI: "I'm sorry, cancellations are only allowed at least 2 hours before the booking.
       Your booking cannot be cancelled at this time."
→ booking.status unchanged
```

---

### M16 · Excel Reports ✦ 2 days

**Goal:** The owner can request a report via WhatsApp and receive an Excel file.

**Tasks:**
- `ReportService.generateDailyReport(businessId, date)`:
  - Queries bookings, payments, customers for that date
  - Builds Excel workbook with Apache POI:
    - Sheet 1: Bookings (booking number, customer, turf, time, price, status)
    - Sheet 2: Payments (booking ref, amount, method, status, refund)
    - Sheet 3: Summary (total bookings, confirmed, cancelled, revenue)
  - Saves to `/data/reports/` with filename `{businessId}-{date}.xlsx`
  - Stores metadata in `report` table
- `WhatsAppClient.sendDocument(to, filePath, filename)` — sends Excel as WhatsApp document
- Register `generateExcelReport(period)` AI tool for owners
- `@Scheduled` — auto-generate daily report at 10 PM
- `@Scheduled` — delete reports older than 7 days (ADR-012)

**Definition of Done:**
```
Owner WhatsApp:
  Owner: "Send me today's report"
  AI: (calls generateExcelReport) "Generating your report..."
  → Owner receives Excel file on WhatsApp

Open Excel:
→ Sheet 1 has today's bookings with correct data
→ Sheet 3 shows correct revenue total

Verify scheduled generation:
→ Mock 10 PM trigger → report generated automatically for seed business
→ Files older than 7 days deleted from /data/reports/
```

---

## Phase 8 — Reminders & Notifications

---

### M17 · Scheduled Reminders ✦ 1 day

**Goal:** Customers automatically receive a reminder 2 hours before their booking.

**Tasks:**
- `ReminderService` (`@Scheduled` hourly):
  - Query CONFIRMED bookings where `start_time` is between 2h and 3h from now
  - For each: send booking reminder template message
  - Mark notification as SENT in `notification` table
  - Handle FAILED sends (log + mark as FAILED, retry_count++)
- No-show handling: `@Scheduled` after booking end time — owner can mark NO_SHOW via AI tool
  - `markNoShow(bookingId)` → CONFIRMED → NO_SHOW (from CONFIRMED, ADR-013)

**Definition of Done:**
```
Insert CONFIRMED booking with start_time = now + 2h 30min
→ Trigger scheduler manually
→ Customer receives WhatsApp template reminder: "Your booking is in 2 hours!"
→ notification.status = SENT

Trigger again for same booking
→ Not sent again (already SENT)
```

---

## Phase 9 — Testing & Deployment

---

### M18 · Critical Path Tests ✦ 3 days

**Goal:** All critical paths have automated tests. The system is verified against every documented failure mode.

**Tasks:**
Write tests for each critical path (Testcontainers for DB, WireMock for Razorpay/WhatsApp/AI):

- **Concurrent booking test:** Two threads book same slot simultaneously → only one succeeds
- **Hold expiry test:** Mock clock past 10 min → slot available again
- **Grace period test:** Hold expires, payment webhook within 60s → slot free → confirmed
- **Grace period test:** Hold expires, payment webhook within 60s → slot taken → refunded
- **Duplicate webhook test:** Same payment event twice → processed once
- **Replay protection test:** Webhook with old timestamp → 400
- **Cancellation window tests:** ≥2h before → success, <2h before → 422
- **Tenant isolation test:** Owner A attempts Business B booking → 403
- **Webhook signature test:** Invalid signature → 401
- **Context window test:** 15-message conversation → only last 10 in AI context
- **Deduplication test:** Same WhatsApp message ID twice → second dropped

**Definition of Done:**
```
./mvnw test
→ All tests pass
→ Coverage: BookingService 90%+, PaymentService 90%+, SlotService 90%+
→ Zero failing tests
→ Zero flaky tests (run 3 times)
```

---

### M19 · Production Deployment ✦ 2 days

**Goal:** The system is running in production, reachable by Meta and Razorpay webhooks.

**Tasks:**
- Create `Dockerfile` for Spring Boot app
- Deploy to Railway or Render
- Connect managed PostgreSQL
- Set all environment variables via platform secrets
- Configure HTTPS (platform-provided)
- Set up Meta webhook to point to production URL
- Verify webhook verification (GET `/webhook/whatsapp`)
- Configure Razorpay webhook to production URL (test mode first)
- Set up health check on platform: `GET /actuator/health`
- Smoke test: send a message on WhatsApp → AI responds

**Definition of Done:**
```
1. Platform health check: PASSING
2. GET https://your-domain.com/actuator/health → 200
3. Meta webhook verification: SUCCESS
4. Send WhatsApp message to production number → AI responds
5. Complete booking flow in production (Razorpay test mode)
6. Booking appears in database
7. Payment confirmed via test webhook
```

---

### M20 · Pilot Launch ✦ 1–2 days

**Goal:** One real turf owner is onboarded. The first real customer booking is made.

**Tasks:**
- Create seed data for the real pilot business (via admin tool or SQL)
- Configure the business's real operating hours and pricing
- Connect the owner's real Razorpay account (live mode)
- Register and get Meta WhatsApp template messages approved
- Switch Razorpay from test to live mode
- Owner sends first message: walk through AI assistant
- Conduct a real booking with a test customer (phone, payment)
- Conduct a real cancellation
- Generate and deliver the first real daily report
- Collect owner feedback

**Definition of Done:**
```
A real customer:
1. Messages the turf's WhatsApp number
2. Checks availability
3. Receives a payment link
4. Pays via UPI
5. Receives booking confirmation on WhatsApp
6. Receives reminder 2 hours before
7. Visits the turf

The owner:
1. Receives booking notification
2. Asks AI for today's bookings
3. Receives and reviews the daily Excel report

Zero double bookings.
Zero missed webhooks.
```

---

## Timeline Summary

| Milestone | Name | Days | Phase |
|-----------|------|------|-------|
| M01 | Project Boots | 1 | Foundation |
| M02 | Developer Standards | 1 | Foundation |
| M03 | Database Schema | 2 | Database |
| M04 | JPA Entities & Repositories | 2 | Database |
| M05 | Slot Availability | 2 | Booking Engine |
| M06 | Booking Hold & Lifecycle | 2 | Booking Engine |
| M07 | Slot Blocking | 1 | Booking Engine |
| M08 | Webhook Foundation | 2 | WhatsApp |
| M09 | Conversation Context | 1 | WhatsApp |
| M10 | AI Integration Foundation | 2 | AI Agent |
| M11 | Customer Booking Flow | 3 | AI Agent |
| M12 | Owner AI Flow | 2 | AI Agent |
| M13 | Payment Link & Webhook | 2 | Payments |
| M14 | Payment Failure & Grace Period | 2 | Payments |
| M15 | Cancellation & Refund | 1 | Reports |
| M16 | Excel Reports | 2 | Reports |
| M17 | Scheduled Reminders | 1 | Notifications |
| M18 | Critical Path Tests | 3 | Testing |
| M19 | Production Deployment | 2 | Deployment |
| M20 | Pilot Launch | 1–2 | Pilot |

**Total: 36–38 working days**

---

## Critical Path

The milestones that must not slip — if any of these are delayed, everything after them delays:

```
M01 → M03 → M05 → M06 → M08 → M11 → M13 → M18 → M19 → M20
```

M11 (Customer Booking AI) is the hardest milestone. Budget extra time there.  
M18 (Tests) should not be skipped — it is what makes M19 safe to deploy.

---

## Dependency Map

```
M01 (boots)
  └── M02 (standards)
        └── M03 (schema)
              └── M04 (entities)
                    ├── M05 (availability)
                    │     └── M06 (hold)
                    │           └── M07 (blocking)
                    │                 └── M08 (webhook)
                    │                       └── M09 (conversation)
                    │                             └── M10 (AI foundation)
                    │                                   ├── M11 (customer AI)
                    │                                   │     └── M12 (owner AI)
                    │                                   └── M11 → M13 (payments)
                    │                                               └── M14 (failure)
                    │                                                     └── M15 (cancel)
                    │                                                           └── M16 (reports)
                    │                                                                 └── M17 (reminders)
                    │                                                                       └── M18 (tests)
                    │                                                                             └── M19 (deploy)
                    │                                                                                   └── M20 (pilot)
```
