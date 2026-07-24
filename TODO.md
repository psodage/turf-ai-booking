# Turf AI Booking — Development TODO

> Version: 1.0
> Status: Development Roadmap

---

# Overall Progress

- [ ] Phase 1 - Project Setup
- [ ] Phase 2 - Database
- [ ] Phase 3 - Booking Engine
- [ ] Phase 4 - WhatsApp Integration
- [ ] Phase 5 - AI Agent
- [ ] Phase 6 - Payment Gateway
- [ ] Phase 7 - Reports
- [ ] Phase 8 - Testing
- [ ] Phase 9 - Deployment
- [ ] Phase 10 - Pilot Launch

---

# Phase 1 — Project Setup

## Repository

- [ ] Create GitHub Repository
- [ ] Configure README
- [ ] Configure LICENSE
- [ ] Configure .gitignore
- [ ] Create project structure

---

## Backend

- [ ] Initialize Spring Boot
- [ ] Java 21
- [ ] Maven
- [ ] Spring Web
- [ ] Spring Security
- [ ] Spring Data JPA
- [ ] Validation
- [ ] Lombok
- [ ] PostgreSQL Driver
- [ ] Actuator

---

## Configuration

- [ ] application.yml
- [ ] Environment Variables
- [ ] Profiles
- [ ] Logging
- [ ] Exception Handler

---

## Docker

- [ ] Dockerfile
- [ ] docker-compose.yml
- [ ] PostgreSQL Container
- [ ] pgAdmin Container

---

### Milestone

✅ Backend starts successfully

---

# Phase 2 — Database

## PostgreSQL

- [ ] Install PostgreSQL
- [ ] Create Database
- [ ] Configure Connection

---

## Flyway

- [ ] Configure Flyway
- [ ] Initial Migration
- [ ] Seed Data

---

## Tables

- [ ] Business
- [ ] Turf
- [ ] Customer
- [ ] Booking
- [ ] Booking Hold
- [ ] Payment
- [ ] Slot
- [ ] Blocked Slot
- [ ] Pricing Rule
- [ ] Conversation
- [ ] Audit Log

---

## Repository Layer

- [ ] Create JPA Entities
- [ ] Repository Interfaces
- [ ] Relationships
- [ ] Indexes

---

### Milestone

✅ Database ready

---

# Phase 3 — Booking Engine

## Availability

- [ ] Slot Generator
- [ ] Slot Validator
- [ ] Availability Checker
- [ ] Alternative Slot Generator

---

## Booking

- [ ] Create Booking
- [ ] Booking Hold
- [ ] Confirm Booking
- [ ] Cancel Booking

---

## Conflict Prevention

- [ ] Row Locking
- [ ] Duplicate Prevention
- [ ] Transaction Management

---

## Business Rules

- [ ] Booking Window
- [ ] Cancellation Window
- [ ] Turf Timing
- [ ] Pricing Logic

---

### Milestone

✅ Booking API complete

---

# Phase 4 — WhatsApp Integration

## Meta Cloud API

- [ ] Create Meta App
- [ ] Configure Webhook
- [ ] Verify Token
- [ ] Access Token

---

## Webhook

- [ ] Verify Webhook
- [ ] Receive Messages
- [ ] Parse Messages
- [ ] Store Conversations

---

## Messaging

- [ ] Send Text
- [ ] Send Interactive Buttons
- [ ] Send Template Messages

---

## Notifications

- [ ] Booking Confirmation
- [ ] Booking Reminder
- [ ] Cancellation
- [ ] Owner Alerts

---

### Milestone

✅ WhatsApp connected

---

# Phase 5 — AI Agent

## AI

- [ ] OpenAI / Gemini Integration
- [ ] Prompt Management
- [ ] System Prompt
- [ ] Context Builder

---

## Tool Calling

- [ ] checkAvailability()
- [ ] createBooking()
- [ ] cancelBooking()
- [ ] createBookingHold()
- [ ] getPricing()
- [ ] getLocation()

---

## Owner Tools

- [ ] Today's Revenue
- [ ] Today's Bookings
- [ ] Weekly Report
- [ ] Block Slot

---

## Memory

- [ ] Conversation History
- [ ] Context Window
- [ ] Session Management

---

### Milestone

✅ AI can complete booking

---

# Phase 6 — Payment Gateway

## Razorpay

- [ ] Create Order
- [ ] Payment Link
- [ ] Webhook
- [ ] Signature Verification

---

## Payment Logic

- [ ] Pending
- [ ] Success
- [ ] Failed
- [ ] Timeout
- [ ] Refund

---

## Booking Confirmation

- [ ] Verify Payment
- [ ] Confirm Booking
- [ ] Release Hold

---

### Milestone

✅ Secure payment flow

---

# Phase 7 — Reports

## Excel

- [ ] Apache POI
- [ ] Daily Report
- [ ] Weekly Report
- [ ] Monthly Report

---

## Owner Reports

- [ ] Revenue Sheet
- [ ] Booking Sheet
- [ ] Cancellation Sheet

---

## Delivery

- [ ] WhatsApp Document
- [ ] Email (Future)

---

### Milestone

✅ Reports generated

---

# Phase 8 — Testing

## Unit Tests

- [ ] Booking Service
- [ ] Payment Service
- [ ] AI Service

---

## Integration Tests

- [ ] Booking API
- [ ] Payment API
- [ ] Webhook
- [ ] AI Tool Calls

---

## Edge Cases

- [ ] Double Booking
- [ ] Duplicate Payment
- [ ] Duplicate Webhook
- [ ] Hold Expiry

---

### Milestone

✅ All critical flows tested

---

# Phase 9 — Deployment

## Infrastructure

- [ ] Railway / Render
- [ ] Docker Deployment
- [ ] PostgreSQL
- [ ] Environment Variables

---

## Monitoring

- [ ] Health Check
- [ ] Logging
- [ ] Error Alerts

---

## Security

- [ ] HTTPS
- [ ] Rate Limiting
- [ ] Secrets

---

### Milestone

✅ Production Ready

---

# Phase 10 — Pilot Launch

## Turf Owner

- [ ] Onboard Turf
- [ ] Configure Pricing
- [ ] Configure Slots
- [ ] Configure Payment

---

## Testing

- [ ] Live Booking
- [ ] Live Payment
- [ ] Live Cancellation
- [ ] Excel Reports

---

## Feedback

- [ ] Customer Feedback
- [ ] Owner Feedback
- [ ] AI Improvements

---

### Milestone

✅ First Live Customer Booking

---

# Future

- [ ] Multi-Turf Support
- [ ] Marathi AI
- [ ] Hindi AI
- [ ] Voice Booking
- [ ] Google Calendar
- [ ] Google Sheets
- [ ] Dynamic Pricing
- [ ] Mobile App
- [ ] Web Dashboard
- [ ] Analytics
- [ ] Tournament Management
- [ ] Subscription Billing

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

Completed Tasks: **0 / 120**

Current Sprint:

➡️ Phase 1 — Project Setup