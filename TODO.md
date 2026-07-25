# Turf AI Booking — Development TODO

> Version: 2.0
> Status: Development Roadmap

---

# Overall Progress

- [x] Phase 0 - Documentation & Architecture
- [x] Phase 1 - Project Setup
- [ ] Phase 2 - Database
- [x] Phase 3 - Booking Engine
- [ ] Phase 4 - WhatsApp Integration
- [ ] Phase 5 - AI Agent
- [ ] Phase 6 - Payment Gateway
- [ ] Phase 7 - Reports
- [ ] Phase 8 - Testing & Hardening
- [ ] Phase 9 - Deployment & Pilot Launch

---

# Phase 0 — Documentation & Architecture

## Documentation

- [x] Product Vision
- [x] Business Model
- [x] RBAC
- [x] Business Rules (complete)
- [x] Payment Rules
- [x] Owner Onboarding
- [x] WhatsApp Integration
- [x] AI Agent Architecture
- [x] Database ERD
- [x] Excel Reporting
- [x] Error Handling
- [x] Security Architecture
- [x] Roadmap
- [x] Conversation Flows
- [x] Architecture Decision Records

## Architecture Refinement

- [x] Customer ownership model (ADR-002)
- [x] Booking → Payment 1:N (ADR-003)
- [x] Booking status simplification (ADR-004)
- [x] Booking hold expiry mechanism (ADR-005)
- [x] WhatsApp routing (ADR-006)
- [x] Remove n8n from MVP (ADR-007)
- [x] Pricing tiers standardized (ADR-008)
- [x] Pilot size standardized (ADR-009)
- [x] Cancellation refund rules (ADR-010)
- [x] Tool name convention (ADR-011)
- [x] Report storage strategy (ADR-012)
- [x] Booking state machine fix (ADR-013)
- [x] Simplified booking hold (ADR-014)
- [x] Webhook replay protection (ADR-015)
- [x] Payment webhook grace period (ADR-016)
- [x] WhatsApp template messages (ADR-017)
- [x] Conversation concurrency control (ADR-018)
- [x] Business timezone (ADR-019)

### Milestone

✅ Documentation implementation-ready

---

# Phase 1 — Project Setup

## Repository

- [x] Create GitHub Repository
- [x] Configure README
- [x] Configure .gitignore
- [x] Create project structure

---

## Backend

- [x] Initialize Spring Boot 3.5
- [x] Java 17+ / Java 21 compatibility
- [x] Maven
- [x] Spring Web
- [x] Spring Security
- [x] Spring Data JPA
- [x] Validation
- [x] Lombok
- [x] PostgreSQL Driver
- [x] Flyway
- [x] Actuator

---

## Configuration

- [x] application.yml (dev, staging, prod profiles)
- [x] Environment Variables
- [x] Logging (structured JSON)
- [x] Global Exception Handler
- [x] Correlation ID Filter (MDC)
- [x] Base Entity classes

---

## Docker

- [x] docker-compose.yml (Spring Boot + PostgreSQL)
- [x] PostgreSQL Container

---

### Milestone

✅ Backend starts successfully

---

# Phase 2 — Database

## Flyway

- [x] Configure Flyway
- [x] Initial Migration (all core tables V1-V17)
- [x] Seed Data (repeatable migration R__seed_demo_data.sql for Green Pitch Kolhapur)

---

## Tables

- [x] Business
- [x] Users
- [x] Turf
- [x] Operating Hours
- [x] Pricing Rule
- [x] Booking
- [x] Booking Hold
- [x] Payment
- [x] Blocked Slot
- [x] Conversation
- [x] Conversation Message
- [x] Notification
- [x] Booking Audit
- [x] Payment Audit
- [x] Report
- [x] System Setting

---

## Repository Layer

- [x] Create JPA Entities
- [x] Repository Interfaces
- [x] Relationships
- [x] Indexes

---

### Milestone

✅ Database ready

---

# Phase 3 — Booking Engine

## Availability

- [x] Slot Generator (from operating hours)
- [x] Slot Validator
- [x] Availability Checker
- [x] Alternative Slot Suggestion

---

## Booking

- [x] Create Booking (status = HOLD)
- [x] Create Booking Hold (simplified — ADR-014)
- [x] Booking number generation (PostgreSQL SEQUENCE)
- [x] Confirm Booking (on payment success)
- [x] Cancel Booking (with cancellation window check)
- [x] Payment webhook grace period logic (ADR-016)

---

## Conflict Prevention

- [x] Pessimistic Locking (SELECT FOR UPDATE)
- [x] Duplicate Prevention
- [x] Transaction Management

---

## Hold Expiry (ADR-005)

- [x] Lazy expiry in queries (expires_at > NOW())
- [x] Scheduled cleanup task (@Scheduled, every 2 min)

---

## Concurrency (ADR-018)

- [x] Conversation lock (SELECT FOR UPDATE on conversation row)
- [x] Message deduplication (whatsapp_message_id)

---

## Timezone (ADR-019)

- [x] Business timezone field
- [x] Timezone-aware slot generation
- [x] Timezone-aware cancellation window check

---

## Business Rules

- [x] Booking Window (30 days advance)
- [x] Cancellation Window (2 hours)
- [x] Turf Operating Hours
- [x] Pricing Resolution (PEAK → WEEKEND → BASE)

---

### Milestone

✅ Booking API complete

---

# Phase 4 — WhatsApp Integration

## Meta Cloud API

- [x] Create Meta App
- [x] Configure Webhook
- [x] Verify Token
- [x] Access Token

---

## Webhook

- [x] Verify Webhook (GET)
- [x] Receive Messages (POST)
- [x] Signature Verification
- [x] Webhook replay protection — timestamp validation (ADR-015)
- [x] Business Routing (phone_number_id → business_id, ADR-006)
- [x] Parse Messages
- [x] Message Deduplication (by whatsapp_message_id)
- [x] Store Conversations

---

## Messaging

- [x] Send Text
- [x] Send Interactive Buttons
- [x] Send Template Messages
- [x] Submit templates to Meta for approval (ADR-017)

---

## Notifications (all via template messages — ADR-017)

- [x] Booking Confirmation
- [x] Booking Reminder (@Scheduled)
- [x] Cancellation
- [x] Owner Alerts

---

### Milestone

✅ WhatsApp connected

---

# Phase 5 — AI Agent

## AI

- [x] OpenAI / Gemini Integration
- [x] Prompt Management
- [x] System Prompt (Customer AI)
- [x] System Prompt (Owner AI)
- [x] Context Builder

---

## Customer Tools (camelCase — ADR-011)

- [x] checkAvailability
- [x] getAvailableTurfs
- [x] createBookingHold
- [x] createPaymentLink
- [x] getMyBookings
- [x] cancelBooking
- [x] getPricing
- [x] getLocation

---

## Owner Tools

- [x] getTodayBookings
- [x] getUpcomingBookings
- [x] blockSlot
- [x] unblockSlot
- [x] generateExcelReport
- [x] getRevenue
- [x] getBookingStatistics
- [x] updatePricing

---

## Memory

- [x] Conversation History (last 10 messages sliding window)
- [x] Context Window management
- [x] Session Management (30-min timeout)
- [x] Token budget per AI call (max 2000 tokens)
- [x] Token usage logging

---

## Tool Response Format

- [x] Standard success/error response structure
- [x] Error code mapping (SLOT_UNAVAILABLE, HOLD_EXPIRED, etc.)
- [x] AI prompt error handling instructions

---

### Milestone

✅ AI can complete booking

---

# Phase 6 — Payment Gateway

## Razorpay Payment Links

- [x] Create Payment Link (not Orders API)
- [x] Webhook
- [x] Signature Verification
- [x] Webhook replay protection — timestamp validation (ADR-015)

---

## Payment Logic

- [x] Multiple payment attempts per booking (ADR-003)
- [x] Success
- [x] Failed
- [x] Timeout / Expired
- [x] Refund

---

## Booking Confirmation

- [x] Verify Payment
- [x] Check Hold (not expired)
- [x] Confirm Booking
- [x] Release Hold (convert to CONFIRMED)

---

### Milestone

✅ Secure payment flow

---

# Phase 7 — Reports

## Excel

- [x] Apache POI
- [x] Daily Report
- [x] Weekly Report
- [x] Monthly Report

---

## Owner Reports

- [x] Revenue Sheet
- [x] Booking Sheet
- [x] Customer Sheet
- [x] Payment Sheet
- [x] Slot Utilization Sheet

---

## Delivery

- [x] WhatsApp Document
- [x] Scheduled generation (@Scheduled — ADR-007)
- [x] Local filesystem storage (ADR-012)

---

### Milestone

✅ Reports generated

---

# Phase 8 — Testing & Hardening

## Unit Tests

- [ ] Booking Service
- [ ] Payment Service
- [ ] Slot Service

---

## Integration Tests

- [ ] Booking API (Testcontainers)
- [ ] Payment Webhook (WireMock)
- [ ] WhatsApp Webhook
- [ ] AI Tool Calls

---

## Edge Cases

- [ ] Double Booking (concurrent)
- [ ] Duplicate Payment
- [ ] Duplicate Webhook
- [ ] Hold Expiry Race Condition
- [ ] Late Payment After Expiry

---

## Security Review

- [ ] RBAC enforcement
- [ ] Tenant isolation verification
- [ ] Webhook signature verification

---

### Milestone

✅ All critical flows tested

---

# Phase 9 — Deployment & Pilot Launch

## Infrastructure

- [ ] Railway / Render
- [ ] Docker Deployment
- [ ] PostgreSQL (managed)
- [ ] Environment Variables

---

## Monitoring

- [ ] Health Check (Actuator)
- [ ] Logging
- [ ] Error Alerts

---

## Security

- [ ] HTTPS
- [ ] Rate Limiting
- [ ] Secrets Management

---

## Pilot (1 turf owner — ADR-009)

- [ ] Onboard Turf Business
- [ ] Configure Pricing
- [ ] Configure Operating Hours
- [ ] Configure Payment Gateway
- [ ] Live Booking Test
- [ ] Live Payment Test
- [ ] Live Cancellation Test
- [ ] Excel Report Test
- [ ] Collect Owner Feedback

---

### Milestone

✅ First Live Customer Booking

---

# Future (Post-MVP)

- [ ] Multi-Turf Support
- [ ] Marathi AI
- [ ] Hindi AI
- [ ] Voice Booking
- [ ] Google Calendar Sync
- [ ] Dynamic Pricing
- [ ] Mobile App
- [ ] Web Dashboard
- [ ] Analytics
- [ ] Tournament Management
- [ ] Subscription Billing
- [ ] n8n Automation (if needed)

---

# MVP Definition of Done

The MVP is complete when:

- [ ] Customer books through WhatsApp
- [ ] AI checks availability
- [ ] Payment is collected
- [ ] Booking is confirmed
- [ ] Owner receives notification
- [ ] Excel report generated
- [ ] No double bookings
- [ ] Security checks passed
- [ ] Successfully used by first turf owner

---

# Progress Tracker

Completed Tasks: **~30 / 130** (documentation phase complete)

Current Sprint:

➡️ Phase 1 — Project Setup