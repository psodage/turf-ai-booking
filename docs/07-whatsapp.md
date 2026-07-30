# Turf AI Booking — WhatsApp Integration

**Document:** 07-whatsapp.md  
**Version:** 3.0  
**Status:** Approved  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines how WhatsApp integrates with Turf AI Booking.

WhatsApp is the primary interface for:

- Customers
- Turf Owners
- Turf Managers

The system must support:

- Customer bookings
- Booking updates
- Payment links
- Owner management
- AI conversations
- Notifications
- Reports

For the MVP, no web dashboard is required.

---

# 2. WhatsApp Architecture

Customer / Owner
        │
        ▼
WhatsApp
        │
        ▼
Meta Cloud API
        │
        ▼
Webhook
        │
        ▼
Spring Boot API Gateway
        │
        ├── Resolve Business (phone_number_id → business_id, see ADR-006)
        ├── Authentication
        ├── Authorization
        ├── Logging
        ├── Rate Limiting
        └── AI Router
                │
                ├── Booking Service
                ├── Payment Service
                ├── Notification Service
                └── Report Service

Spring Boot handles all services. No n8n in MVP (see ADR-007).

---

# 3a. 24-Hour Messaging Window (ADR-017)

Meta enforces a 24-hour messaging window. After 24 hours from the customer's last message, only pre-approved **template messages** can be sent.

| Message Type | Delivery Method |
|-------------|----------------|
| Active conversation response | Free-form text (within 24h window) |
| Booking confirmation (after webhook) | Template message |
| Booking reminder (2h before) | Template message |
| Cancellation confirmation | Template message |
| Owner notification | Template message |

Template messages must be submitted to Meta for approval during Phase 4.

All proactive outbound messages (reminders, confirmations after async processing, notifications) MUST use templates.

---

# 3. Why Spring Boot First?

The backend must always receive the message first.

Reasons:

- Identity Verification
- Security
- Logging
- Rate Limiting
- Business Rules
- Duplicate Detection
- Tenant Isolation

The AI should never receive unauthenticated requests.

---

# 4. WhatsApp User Types

The system supports:

CUSTOMER

↓

Books turf

---

TURF_OWNER

↓

Manages business

---

TURF_MANAGER

↓

Manages daily operations

---

SYSTEM_ADMIN

↓

Platform support

---

# 5. User Identification

Users are identified using their WhatsApp number.

Example

+91XXXXXXXXXX

↓

Database Lookup

↓

User Found?

↓

Role

↓

Business

↓

Conversation Context

---

# 6. Unknown User Flow

Unknown Number

↓

No User Found

↓

Treat as Customer

↓

Create Temporary Profile

↓

Continue Conversation

Unknown users cannot perform owner operations.

---

# 7. Conversation Context

Every conversation stores:

Conversation ID

User ID

Business ID

Current Intent

Current Booking

Language

Last Activity

This enables multi-step conversations.

---

# 8. Conversation Example

Customer

Book tomorrow at 7 PM

↓

AI

Which turf?

↓

Customer

Turf 2

↓

AI

Checking availability...

↓

Backend

↓

Available

↓

AI

The slot is available for ₹800.

Would you like to continue?

---

# 9. Message Lifecycle

Incoming WhatsApp Message

↓

Webhook

↓

Verify Signature

↓

Authenticate User

↓

Load Context

↓

Intent Detection

↓

Business Logic

↓

AI Response

↓

Send WhatsApp Reply

---

# 10. Webhook Verification

Every incoming webhook must verify:

- Verify Token
- Request Signature
- Event Type

Invalid requests are rejected immediately.

---

# 11. Supported Message Types

The MVP supports:

- Text
- Interactive Buttons
- List Messages
- Location
- Image (future)

Not supported initially:

- Voice
- Video
- Documents

---

# 12. Text Messages

Examples

Book tomorrow

Show today's bookings

Cancel booking

Today's revenue

The AI interprets intent before calling backend tools.

---

# 13. Interactive Buttons

Buttons improve UX.

Example

Choose Action

[ Book Turf ]

[ My Bookings ]

[ Contact Owner ]

---

Example

Confirm Booking

[ Pay Now ]

[ Cancel ]

---

# 14. List Messages

Useful for selecting slots.

Example

Available Slots

06:00 PM

07:00 PM

08:00 PM

09:00 PM

Customer selects one.

---

# 15. Location Sharing

Owner Location

↓

Customer taps

↓

Google Maps Opens

Future:

Customer location may be used for navigation.

---

# 16. WhatsApp Session

The backend stores:

Conversation Start

↓

Messages

↓

Intent

↓

Booking Context

↓

Conversation End

---

# 17. AI Context

Before AI processes a message:

Backend provides:

User

Role

Business

Current Booking

Conversation State

AI never queries the database directly.

---

# 18. Duplicate Message Detection

WhatsApp may resend events.

Every incoming message includes:

Message ID

↓

Already Processed?

YES

↓

Ignore

This prevents duplicate bookings.

---

# 19. Idempotency

Every incoming event is processed once.

Event

↓

Message ID

↓

Exists?

↓

Ignore Duplicate

---

# 20. Customer Conversation Flow

Hi

↓

Welcome

↓

Book Turf

↓

Select Date

↓

Select Time

↓

Availability

↓

Payment

↓

Confirmation

---

# 21. Owner Conversation Flow

Hi

↓

Welcome Owner

↓

Today's Bookings

↓

Backend

↓

Booking Summary

↓

Owner Response

---

# 22. Owner Commands

Examples

Today's bookings

Tomorrow's bookings

Revenue

Block slot

Cancel booking

Send Excel report

Business statistics

---

# 23. Customer Commands

Examples

Book turf

Availability

Pricing

Cancel booking

My bookings

Location

Help

---

# 24. AI Tool Calls

Customer

Book tomorrow

↓

AI

↓

checkAvailability()

↓

Spring Boot

↓

Database

↓

Result

↓

AI Response

---

# 25. Notification Types

Customer

- Booking Confirmed
- Payment Failed
- Reminder
- Cancellation
- Refund

Owner

- New Booking
- Cancellation
- Daily Report
- Revenue Summary

---

# 26. Template Messages

Used outside the 24-hour session.

Examples

Booking Reminder

Payment Reminder

Refund Confirmation

Daily Report

Monthly Report

---

# 27. Session Messages

Inside active conversation:

Free-form responses allowed.

Outside session:

Use approved templates.

---

# 28. Error Handling

Example

Backend Down

↓

Customer

Sorry, we're experiencing technical issues.

Please try again.

---

Payment Failure

↓

Payment unsuccessful.

Please try again.

---

# 29. Unsupported Requests

Example

Customer

Book cricket turf

↓

AI

Currently we only support football turfs.

---

# 30. Rate Limiting

Prevent spam.

Example

50 requests

↓

1 minute

↓

Temporarily limit user

---

# 31. Media Support

Future support:

Images

Invoices

Booking Receipts

QR Codes

PDF Reports

---

# 32. Language Support

Initial:

English

Future:

Marathi

Hindi

Multi-language responses will depend on user preference.

---

# 33. AI Escalation

If AI cannot answer:

↓

Owner Notification

or

Human Follow-up

---

# 34. Conversation Timeout

Inactive conversations expire.

Default:

10 minutes

Context removed.

New conversation starts fresh.

---

# 35. Logging

Every message logs:

Message ID

Timestamp

Sender

Receiver

Intent

Processing Time

Status

---

# 36. Security

Never trust WhatsApp content.

Always validate:

Identity

Permissions

Booking

Payment

Business

---

# 37. Privacy

Store only necessary information.

Never expose:

Payment Secrets

Webhook Secrets

Internal IDs

Database Information

---

# 38. WhatsApp Status

Track:

Sent

Delivered

Read

Failed

Useful for reminders.

---

# 39. Retry Strategy

Outgoing message fails

↓

Retry

↓

Retry

↓

Retry

↓

Mark Failed

---

# 40. Daily Reports

Owner may request:

Today's bookings

↓

Excel Generated

↓

WhatsApp Document

---

# 41. Customer Booking Example

Customer

Book tomorrow at 8 PM

↓

AI

Checking availability...

↓

Backend

↓

Available

↓

Payment Link

↓

Payment Success

↓

Booking Confirmed

---

# 42. Owner AI Example

Owner

Today's revenue

↓

AI

↓

Backend

↓

₹18,400

↓

WhatsApp Response

---

# 43. MVP WhatsApp Features

✓ Booking

✓ Cancellation

✓ Availability

✓ Payments

✓ Booking Reminder

✓ Reports

✓ Owner AI

✓ Customer AI

✓ Notifications

---

# 44. Future Enhancements

- Voice Messages
- Voice Booking
- Image Understanding
- QR Check-in
- WhatsApp Catalog
- Multi-language AI
- Group Booking
- Broadcast Campaigns

---

# 45. Technical Principles

1. Spring Boot receives every message.
2. AI never directly accesses PostgreSQL.
3. Every message is authenticated.
4. Every tool call is authorized.
5. Duplicate messages are ignored.
6. Business rules execute before AI replies.
7. AI is an assistant, not the source of truth.

---

# 46. Next Document

The next document is:

docs/08-ai-agent.md

This document defines:

- AI architecture
- Tool calling
- Memory
- Prompt engineering
- AI workflows
- Customer AI
- Owner AI
- Guardrails
- Function calling
- Conversation memory
- Cost optimization
- AI security