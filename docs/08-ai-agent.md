# Turf AI Booking — AI Agent Architecture

**Document:** 08-ai-agent.md  
**Version:** 1.0  
**Status:** Architecture Approved  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the AI architecture for Turf AI Booking.

The AI is responsible for:

- Understanding natural language
- Managing conversations
- Calling backend tools
- Guiding customers
- Assisting turf owners

The AI is NOT responsible for:

- Booking validation
- Payment verification
- Database updates
- Authorization
- Business rules

These responsibilities belong to the backend.

---

# 2. AI Design Principles

The AI follows these principles:

1. AI understands language.
2. Backend owns business logic.
3. Database is never exposed to AI.
4. AI always uses tools.
5. AI never guesses.
6. AI never bypasses security.
7. AI must be deterministic where possible.

---

# 3. AI Architecture

WhatsApp
      │
      ▼
Spring Boot Gateway
      │
      ▼
AI Orchestrator
      │
      ├──────────────┐
      │              │
      ▼              ▼
Customer AI     Owner AI
      │              │
      └──────┬───────┘
             ▼
      Tool Gateway
             │
 ┌───────────┼────────────┐
 ▼           ▼            ▼
Booking   Payment   Reporting
             │
             ▼
        PostgreSQL

---

# 4. Why Multiple AI Agents?

Instead of one large AI:

Customer AI

handles customers.

Owner AI

handles owners.

Advantages:

- Smaller prompts
- Better accuracy
- Lower token cost
- Easier testing
- Better security

---

# 5. AI Orchestrator

The orchestrator decides:

Incoming Message

↓

Customer?

↓

Owner?

↓

Manager?

↓

Unknown?

↓

Select AI Agent

The AI itself does not determine authorization.

---

# 6. Customer AI Responsibilities

Customer AI may:

- Understand booking requests
- Understand cancellations
- Check availability
- Explain pricing
- Generate payment requests
- Answer FAQs

Customer AI cannot:

- Modify bookings directly
- Access revenue
- Block slots
- Change pricing

---

# 7. Owner AI Responsibilities

Owner AI may:

- View bookings
- View reports
- Block slots
- Manage availability
- Generate Excel
- View revenue

Owner AI cannot:

- Access another business
- Modify payment status
- Bypass booking rules

---

# 8. AI Workflow

Customer

↓

Natural Language

↓

Intent Detection

↓

Tool Selection

↓

Tool Execution

↓

Backend Response

↓

Natural Language Response

---

# 9. AI Memory

Conversation memory stores:

Conversation ID

User ID

Role

Current Intent

Booking Context

Language

Memory is short-lived.

Business data remains in PostgreSQL.

---

# 10. AI Context

Before every AI request:

Backend provides:

User

Role

Business

Current Booking

Conversation State

The AI does not query the database.

---

# 11. Prompt Structure

Every AI request contains:

System Prompt

↓

Role Prompt

↓

Conversation History

↓

Business Context

↓

Tool Definitions

↓

User Message

---

# 12. Customer System Prompt

Customer AI is instructed to:

- Be concise
- Never invent availability
- Always call tools
- Confirm before payment
- Never promise booking before confirmation

---

# 13. Owner System Prompt

Owner AI is instructed to:

- Be operational
- Provide summaries
- Call backend tools
- Never expose other businesses
- Never modify payment state

---

# 14. Tool Calling Principle

The AI never writes SQL.

Instead:

AI

↓

Tool

↓

Backend

↓

Database

↓

Tool Result

↓

AI

---

# 15. Tool Categories

Booking Tools

Payment Tools

Business Tools

Reporting Tools

Notification Tools

---

# 16. Customer Tools

Available tools:

- search_turfs
- check_availability
- create_booking_hold
- create_payment
- get_my_booking
- cancel_booking
- get_location

---

# 17. Owner Tools

Available tools:

- get_today_bookings
- get_upcoming_bookings
- block_slot
- unblock_slot
- generate_excel
- get_revenue
- get_statistics

---

# 18. AI Guardrails

The AI must never:

- Access database
- Execute SQL
- Generate fake booking IDs
- Confirm payments
- Refund money
- Modify audit logs
- Guess booking status

---

# 19. Function Calling

Example

Customer:

Book tomorrow at 7 PM

↓

AI

↓

check_availability()

↓

Backend

↓

Available

↓

AI

↓

Ask customer to continue.

---

# 20. Booking Flow

Customer

↓

Book Turf

↓

Tool

↓

Availability

↓

Hold

↓

Payment

↓

Webhook

↓

Booking Confirmed

AI only coordinates.

---

# 21. AI Error Handling

Tool Failure

↓

AI

↓

Friendly Error

↓

Retry Suggestion

Example:

"Sorry, I couldn't check availability right now."

---

# 22. AI Hallucination Policy

The AI must never:

Invent

- Prices
- Availability
- Booking IDs
- Payment Status
- Revenue

If data is unavailable:

The AI should clearly state that it cannot determine the answer.

---

# 23. AI Security

Every tool request contains:

User ID

Role

Business ID

Conversation ID

Backend validates all permissions.

---

# 24. Language Support

Initial:

English

Future:

Marathi

Hindi

Language preference stored per user.

---

# 25. Cost Optimization

AI should not answer deterministic questions.

Example:

"Is slot available?"

↓

Backend

NOT AI reasoning.

AI should focus on:

Intent

Language

Conversation

---

# 26. AI Retry Strategy

If the model fails:

Retry once.

If still failing:

Fallback response.

Escalate if necessary.

---

# 27. AI Logging

Store:

Conversation ID

Intent

Tool Calls

Latency

Tokens

Model

Success

Failure

No sensitive prompts stored unnecessarily.

---

# 28. Model Selection

Recommended:

Fast Model

↓

Customer conversations

Reasoning Model

↓

Owner analytics

Model selection may evolve over time.

---

# 29. AI Performance Metrics

Track:

Intent Accuracy

Tool Success Rate

Average Response Time

Average Tokens

Customer Satisfaction

Owner Satisfaction

Escalation Rate

---

# 30. AI Safety Principles

The AI must:

✓ Be truthful

✓ Be transparent

✓ Never fabricate

✓ Respect authorization

✓ Always defer to backend validation

---

# 31. Future Enhancements

Future capabilities:

- Voice conversations
- Image understanding
- Smart scheduling
- Revenue forecasting
- Dynamic pricing suggestions
- Customer segmentation
- Marketing campaigns

---

# 32. AI Interaction Examples

Customer:

"Book tomorrow evening."

↓

AI:

"What time would you prefer?"

---

Owner:

"Today's bookings."

↓

Tool

↓

Summary

↓

AI response.

---

# 33. AI Development Strategy

Phase 1

Intent Detection

↓

Phase 2

Tool Calling

↓

Phase 3

Conversation Memory

↓

Phase 4

Optimization

↓

Phase 5

Advanced AI

---

# 34. AI Principles Summary

The AI:

- Understands language
- Guides conversations
- Calls backend tools

The Backend:

- Owns business rules
- Owns security
- Owns database
- Owns payments
- Owns bookings

---

# 35. Next Document

The next document is:

docs/09-database-erd.md

This document defines:

- Complete ER Diagram
- PostgreSQL schema
- Tables
- Relationships
- Indexes
- Constraints
- Booking conflict prevention
- Transactions
- Optimistic/Pessimistic locking
- Multi-tenant architecture
- Audit tables
- Payment schema
- Excel export schema