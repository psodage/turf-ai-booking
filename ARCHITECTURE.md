# Turf AI Booking — Architecture

> Version: 2.0
> Status: Living Document

---

# 1. Overview

Turf AI Booking is a **WhatsApp-first AI Booking Assistant** for football turf owners.

Instead of building a marketplace, each turf owner gets their own AI assistant that:

- Handles customer conversations
- Checks slot availability
- Collects payments
- Confirms bookings
- Sends reminders
- Generates Excel reports
- Assists the owner through WhatsApp

The entire MVP is designed around **one WhatsApp number per turf owner**.

---

# 2. High-Level Architecture

```text
                    Customer
                        │
                        ▼
                 WhatsApp Chat
                        │
                        ▼
              WhatsApp Cloud API
                        │
                        ▼
                 Webhook Endpoint
                 (Spring Boot)
                        │
                        ▼
             Conversation Orchestrator
                        │
          ┌─────────────┼──────────────┐
          ▼             ▼              ▼
      AI Service   Booking Service  Payment Service
          │             │              │
          │             ▼              ▼
          │      PostgreSQL      Razorpay
          │
          ▼
      OpenAI / Gemini
                        │
                        ▼
                 Response Generator
                        │
                        ▼
                 WhatsApp Response
```

---

# 3. Core Components

## Backend

Spring Boot 3.5 (Java 21)

Responsibilities:

- REST APIs
- Business Logic
- AI Tool Execution
- Payment Verification
- Booking Engine
- Scheduled Tasks (reminders, hold expiry, report generation)

---

## Database

PostgreSQL

Stores:

- Businesses
- Turfs
- Users (Customers, Owners, Managers)
- Bookings
- Booking Holds
- Payments
- Conversations
- Reports
- Audit Logs

---

## AI Layer

Responsible for:

- Understanding customer intent
- Calling backend tools
- Generating responses

AI never directly accesses the database.

---

## WhatsApp

Primary user interface.

Both customers and owners communicate only through WhatsApp.

No mobile app is required for the MVP.

---

## Payment Gateway

Razorpay **Payment Links** (not Orders API)

Responsible for:

- Payment Link generation (URL shared via WhatsApp)
- Payment Verification
- Refunds
- Webhooks

Payment Links are the only model that works for WhatsApp-only flows. The customer opens the URL in their mobile browser, completes payment, and the webhook fires.

---

## Reporting

Apache POI

Generates:

- Daily Excel Reports
- Weekly Reports
- Monthly Reports

Reports are stored temporarily on the local filesystem and delivered through WhatsApp (see ADR-012).

---

# 4. WhatsApp to Business Routing (ADR-006)

Each turf business has a dedicated WhatsApp Business phone number.

```text
Incoming WhatsApp Message
        │
        ▼
Webhook contains phone_number_id
        │
        ▼
Lookup: business.whatsapp_phone_number_id = phone_number_id
        │
        ▼
Business identified
        │
        ▼
Route to AI Orchestrator with business context
```

For MVP (single turf owner): one phone_number_id maps to one business.

For future multi-tenant: each business row has its own `whatsapp_phone_number_id`.

---

# 5. Request Flow

## Customer Booking

```text
Customer → WhatsApp → Webhook → Conversation Service → AI Agent
    → checkAvailability() → Booking Service → Database
    → AI Response → WhatsApp
```

## Payment Flow

```text
Customer → Payment Link → Razorpay → Webhook
    → Payment Verification → Booking Confirmation
    → WhatsApp Notification
```

## Owner Flow

```text
Owner → WhatsApp → AI Agent → Owner Tool
    → Database → Business Data → WhatsApp Reply
```

---

# 6. Service Architecture

```text
Controller → DTO → Service → Repository → PostgreSQL
```

Rules:

- Controllers contain no business logic.
- Services implement business rules.
- Repositories only access data.
- Entities are never returned directly.
- Use constructor injection (never field injection).
- Validate all inputs.

---

# 7. AI Architecture

```text
Customer Message → Intent Detection → Conversation Context
    → AI Prompt → Tool Selection → Backend Tool → Tool Result
    → AI Response → WhatsApp
```

AI is responsible for conversation.

Backend is responsible for decisions.

Two separate AI agents:

- **Customer AI**: handles booking, pricing, availability, cancellation
- **Owner AI**: handles bookings view, revenue, reports, slot blocking

See: `docs/08-ai-agent.md` for details.

---

# 8. Backend Services

| Service | Responsibility |
|----------|----------------|
| BookingService | Booking lifecycle |
| PaymentService | Payments |
| CustomerService | Customer management |
| OwnerService | Owner operations |
| TurfService | Turf management |
| SlotService | Availability and slot generation |
| AIService | AI orchestration |
| ConversationService | Conversation history |
| ReportService | Excel reports |
| NotificationService | WhatsApp messaging |
| HoldExpiryService | Scheduled hold cleanup (ADR-005) |
| ReminderService | Scheduled booking reminders |

---

# 9. Booking Hold Expiry (ADR-005)

```text
Hold Created (expires_at = NOW() + 10 min)
        │
        ▼
Availability queries filter: expires_at > NOW()
        │
        ▼
Scheduled cleanup every 2 minutes:
  Find: expires_at < NOW() AND status IN (HOLD, PAYMENT_PENDING)
  Update: status = EXPIRED
  Release: slot available again
```

No Redis or message queue required for MVP.

### Grace Period (ADR-016)

If a payment webhook arrives within 60 seconds after hold expiry:
- Check if the slot has been rebooked.
- If free: reactivate hold, confirm booking.
- If taken: initiate refund.

---

# 10. Conversation Concurrency (ADR-018)

When a customer sends rapid messages, the webhook fires multiple times.

```text
Webhook received
    ↓
Start transaction
    ↓
SELECT conversation FOR UPDATE (lock this conversation)
    ↓
Process message (AI call, tool execution)
    ↓
Update conversation state
    ↓
Commit transaction
```

Messages for the same conversation serialize. Messages for different conversations process in parallel.

No Redis or distributed locking required.

---

# 11. Database Overview

Core Tables

- Business
- Users
- Turf
- Operating Hours
- Pricing Rule
- Booking
- Booking Hold
- Payment (1:N with Booking — ADR-003)
- Blocked Slot
- Conversation
- Conversation Message
- Notification
- Booking Audit
- Payment Audit
- Report
- System Setting

Customer users have NULL `business_id` (ADR-002).

Detailed ERD: `docs/09-database-erd.md`

---

# 12. Security Layers

```text
WhatsApp → Webhook Verification → User Identification
    → Authorization (RBAC) → Business Rules → Database → Audit Logging
```

Every request passes through every layer.

---

# 13. Business Rules

Critical rules:

- Never confirm booking before payment.
- Never allow double booking.
- Every booking belongs to one business.
- AI never guesses availability.
- AI never guesses pricing.
- AI never confirms payment.
- Booking Hold expires after 10 minutes.
- Cancellation allowed ≥ 2 hours before start.
- Conflict check includes HOLD, PAYMENT_PENDING, and CONFIRMED statuses.

See: `docs/04-business-rules.md`

---

# 14. WhatsApp Template Messages (ADR-017)

All proactive outbound messages must use approved template messages:

| Message Type | Delivery Method |
|-------------|----------------|
| Active conversation response | Free-form text (within 24h window) |
| Booking confirmation (webhook) | Template message |
| Booking reminder (2h before) | Template message |
| Cancellation confirmation | Template message |
| Owner notification | Template message |

Templates must be submitted to Meta during Phase 4.

---

# 15. Tool Error Response Format

All backend tools return a standard response to the AI:

### Success

```json
{
  "success": true,
  "data": { ... }
}
```

### Error

```json
{
  "success": false,
  "error_code": "SLOT_UNAVAILABLE",
  "message": "The requested slot is no longer available.",
  "suggestions": ["18:00", "20:00", "21:00"]
}
```

The AI prompt includes instructions for handling each error code gracefully.

---

# 16. Conversation Design

Customer conversations:

`docs/conversations/`

Includes:

- Booking
- Cancellation
- Payment Failure
- Owner Commands

---

# 17. Technology Stack

| Layer | Technology |
|--------|------------|
| Backend | Spring Boot 3.5 |
| Language | Java 21 |
| Build | Maven |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Migration | Flyway |
| AI | OpenAI / Gemini |
| Messaging | WhatsApp Cloud API |
| Payments | Razorpay Payment Links |
| Reports | Apache POI |
| Scheduling | Spring @Scheduled |
| Deployment | Docker |
| Cloud | Railway / Render |

---

# 18. API Design Conventions

All REST APIs follow these conventions:

### URL Structure

```
/api/v1/{resource}
/api/v1/{resource}/{id}
```

### HTTP Methods

| Method | Usage |
|--------|-------|
| GET | Read resource(s) |
| POST | Create resource |
| PUT | Full update |
| PATCH | Partial update |
| DELETE | Soft delete |

### Request/Response Format

- JSON only
- UTC timestamps (ISO 8601)
- UUIDs for all IDs
- Snake_case for JSON fields (matching database convention)

### Error Response Format

```json
{
  "error": {
    "code": "ERR-002",
    "message": "Slot is no longer available.",
    "timestamp": "2026-07-25T10:30:00Z",
    "correlationId": "abc-123"
  }
}
```

### Pagination

```
GET /api/v1/bookings?page=0&size=20&sort=createdAt,desc
```

### Webhook Endpoints

```
POST /webhook/whatsapp    (WhatsApp Cloud API)
GET  /webhook/whatsapp    (Webhook verification)
POST /webhook/razorpay    (Payment events)
```

---

# 19. Testing Strategy

### Frameworks

| Tool | Purpose |
|------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| Testcontainers | PostgreSQL integration tests |
| Spring Boot Test | Controller + service integration tests |
| WireMock | External API mocking (Razorpay, WhatsApp, AI) |

### Test Categories

| Category | Scope |
|----------|-------|
| Unit Tests | Service logic, business rules |
| Integration Tests | API endpoints, database queries |
| Booking Conflict Tests | Concurrent booking scenarios |
| Payment Tests | Webhook handling, signature verification |
| AI Tool Tests | Tool execution with mocked AI |
| Edge Case Tests | Double booking, hold expiry, duplicate payment |

### Coverage Target

- Critical paths (booking, payment, cancellation): 90%+
- Service layer: 80%+
- Controller layer: 70%+

---

# 20. Deployment Architecture (MVP)

```text
┌─────────────────────────────────┐
│         Cloud Platform          │
│     (Railway / Render)          │
│                                 │
│  ┌───────────────────────────┐  │
│  │    Spring Boot App        │  │
│  │    (Docker Container)     │  │
│  │                           │  │
│  │  ├── REST APIs            │  │
│  │  ├── Webhook Endpoints    │  │
│  │  ├── AI Service           │  │
│  │  ├── Scheduled Tasks      │  │
│  │  └── Report Generator     │  │
│  └───────────────────────────┘  │
│              │                  │
│  ┌───────────────────────────┐  │
│  │    PostgreSQL             │  │
│  │    (Managed Database)     │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │    File Storage           │  │
│  │    (Local / Persistent)   │  │
│  │    └── /data/reports/     │  │
│  └───────────────────────────┘  │
│                                 │
└─────────────────────────────────┘
         │            │
         ▼            ▼
   WhatsApp API   Razorpay API
```

### Local Development

```text
docker-compose.yml
  ├── Spring Boot (host / container)
  └── PostgreSQL (container)
```

### Environment Profiles

| Profile | Usage |
|---------|-------|
| dev | Local development |
| staging | Pre-production testing |
| prod | Production |

---

# 21. Project Structure

```text
turf-ai-booking/

├── backend/
│   └── (Spring Boot project — to be initialized)
│
├── docs/
│   ├── 01-product-vision.md
│   ├── 02-business-model.md
│   ├── 03-rbac.md
│   ├── 04-business-rules.md
│   ├── 05-payment-rules.md
│   ├── 06-owner-onboarding.md
│   ├── 07-whatsapp.md
│   ├── 08-ai-agent.md
│   ├── 09-database-erd.md
│   ├── 10-excel-report.md
│   ├── 11-error-handling.md
│   ├── 12-security.md
│   ├── 13-roadmap.md
│   └── conversations/
│       ├── customer-booking.md
│       ├── customer-cancellation.md
│       ├── owner-agent.md
│       └── payment-failure.md
│
├── frontend/          (Future — not in MVP)
│
├── .env.example
├── .gitignore
├── README.md
├── ARCHITECTURE.md
├── AGENTS.md
├── DECISIONS.md
├── TODO.md
└── PROJECT_STATUS.md
```

---

# 22. Design Principles

The project follows:

- Clean Architecture
- Layered Architecture
- Domain-Driven Design (Lightweight)
- SOLID Principles
- Dependency Injection (Constructor only)
- RESTful APIs

---

# 23. Development Workflow

```text
Documentation → Database → Backend → WhatsApp → AI → Payments → Reports → Testing → Deployment
```

Never skip phases.

---

# 24. Future Evolution

Phase 1: Single Turf → Phase 2: Multiple Turfs → Phase 3: Multi-City → Phase 4: AI Business Platform

---

# 25. Related Documentation

| Document | Purpose |
|----------|---------| 
| README.md | Project Overview |
| AGENTS.md | AI Development Guide |
| DECISIONS.md | Architecture Decision Records |
| TODO.md | Development Tasks |
| PROJECT_STATUS.md | Current Sprint |
| docs/ | Business Documentation |
| conversations/ | AI Conversation Flows |

---

# 26. Guiding Principle

> **The AI talks. The Backend decides. The Database stores.**

- AI handles conversations.
- Spring Boot enforces business rules.
- PostgreSQL stores the source of truth.
- WhatsApp is the only user interface for the MVP.

---

# End of Document