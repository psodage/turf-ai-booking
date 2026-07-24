# Turf AI Booking — Product Vision

**Document:** 01-product-vision.md  
**Version:** 1.0  
**Status:** Draft  
**Last Updated:** 2026-07-24  
**Initial Market:** Kolhapur, Maharashtra, India  

---

# 1. Product Overview

Turf AI Booking is a WhatsApp-based AI booking and management system designed for football turf businesses.

The system allows customers to interact with a turf through WhatsApp to:

- Get information about the turf
- Check available time slots
- View pricing
- Book a turf
- Make an online payment
- Receive booking confirmation
- View their booking
- Cancel a booking according to the turf's cancellation policy

Turf owners can interact with their own AI-powered WhatsApp assistant to:

- View today's bookings
- View upcoming bookings
- Check booking availability
- Check revenue
- Block time slots
- Unblock time slots
- Manage turf information
- Manage pricing
- View customer information
- Generate booking reports

The initial product will focus on football turf businesses in Kolhapur, Maharashtra.

The system will initially avoid building a large public marketplace or consumer mobile application. The primary interface for customers and turf owners will be WhatsApp.

---

# 2. Vision

To make football turf booking as simple as sending a WhatsApp message.

The long-term vision is to provide turf owners with an AI-powered digital assistant that can handle routine booking operations automatically, while allowing customers to book available turf slots quickly and securely.

The product should reduce manual booking work for turf owners and eliminate the uncertainty and fake bookings associated with informal booking methods.

---

# 3. Problem Statement

Many local football turf businesses manage bookings through:

- WhatsApp messages
- Phone calls
- Instagram messages
- Manual registers
- Excel sheets
- Google Sheets
- Personal calendars

This creates several problems.

## 3.1 Manual Booking Management

Turf owners may need to manually respond to every customer asking:

> "Is 7 PM available?"

> "What is the price?"

> "Can I book tomorrow?"

This consumes the owner's time.

---

## 3.2 Double Bookings

When multiple customers contact the owner at the same time, manually maintaining availability can lead to:

- Double bookings
- Incorrect availability information
- Human errors

---

## 3.3 Fake or Unverified Bookings

Customers may reserve a slot without actually intending to use it.

This can result in:

- Lost revenue
- Blocked slots
- Last-minute cancellations
- No-shows

The system should reduce this problem by requiring online payment or an appropriate advance payment before confirming a booking.

---

## 3.4 Slow Customer Experience

Customers may need to wait for the turf owner to respond before they know:

- Whether a slot is available
- How much it costs
- How to pay
- Whether their booking is confirmed

The AI assistant should provide immediate responses for common requests.

---

## 3.5 Lack of Business Insights

Turf owners may not have an easy way to understand:

- Daily bookings
- Upcoming bookings
- Revenue
- Peak hours
- Cancellation rates
- Customer activity

The system should make this information accessible through WhatsApp and downloadable reports.

---

# 4. Target Market

## Initial Geographic Market

The first target market is:

> Kolhapur City, Maharashtra, India

The initial goal is to validate the product with a small number of local football turf owners before expanding to other cities.

---

## Initial Target Customers

The primary business customers are:

- Football turf owners
- Football academy owners with bookable turf facilities
- Sports facility owners
- Multi-turf sports complexes

The first pilot should focus specifically on football turf businesses.

---

# 5. Target Users

The system has three primary user types.

## 5.1 Customer

A person who wants to book a football turf.

Customers interact primarily through WhatsApp.

They should be able to:

- Ask about a turf
- Check availability
- View pricing
- Select a date
- Select a time slot
- Make a payment
- Confirm a booking
- View booking details
- Cancel a booking according to policy

---

## 5.2 Turf Owner

The business owner or authorized manager of a turf.

The owner interacts primarily through WhatsApp with an owner-specific AI assistant.

The owner should be able to:

- View today's bookings
- View upcoming bookings
- Check revenue
- Block slots
- Unblock slots
- Update pricing
- View booking details
- Manage turf information
- Generate reports

---

## 5.3 System Administrator

The system administrator manages the overall platform.

The administrator should be able to:

- Onboard turf businesses
- Create and manage turf accounts
- Manage turf owners
- View system activity
- Monitor bookings
- Handle support issues
- Manage system configuration
- Manage integrations

The administrator is initially the product operator.

---

# 6. Core Product Concept

The core product is a WhatsApp-based booking assistant.

The basic customer workflow is:

Customer
    ↓
Sends WhatsApp message
    ↓
AI understands the request
    ↓
Checks turf configuration
    ↓
Checks availability
    ↓
Shows available slot
    ↓
Customer selects slot
    ↓
Temporary booking hold is created
    ↓
Customer receives payment link
    ↓
Customer completes payment
    ↓
Payment is verified
    ↓
Booking is confirmed
    ↓
Customer receives confirmation

---

# 7. Customer Experience

A typical customer conversation should look like:

Customer:

> Hi

AI:

> Hi 👋 Welcome to XYZ Football Turf.
> How can I help you today?

Customer:

> I want to book a turf tomorrow at 7 PM.

AI:

> Let me check availability.

The system checks the booking engine.

If available:

> Turf 1 is available tomorrow from 7 PM to 8 PM.
>
> Price: ₹800.
>
> Would you like to book it?

Customer:

> Yes.

AI:

> Great! I've temporarily reserved the slot for you for 10 minutes.
>
> Please complete the payment to confirm your booking.
>
> [Payment Link]

Customer completes payment.

AI:

> 🎉 Your booking is confirmed!
>
> Booking ID: TURF-12345
>
> Turf: Turf 1
> Date: 25 July 2026
> Time: 7 PM – 8 PM
> Amount Paid: ₹800
>
> Thank you for booking with us!

---

# 8. Owner Experience

The turf owner should be able to interact with an owner-specific AI assistant.

Example:

Owner:

> Today's bookings

AI:

> You have 12 bookings today.
>
> Total expected revenue: ₹9,600
>
> First booking: 5 PM
> Last booking: 10 PM

---

Owner:

> Block Turf 1 tomorrow at 7 PM.

AI:

> Turf 1 is currently available tomorrow from 7 PM to 8 PM.
>
> Would you like me to block this slot?

Owner:

> Yes.

AI:

> Done. Turf 1 has been blocked from 7 PM to 8 PM tomorrow.

---

Owner:

> Show me this month's revenue.

AI:

> July 2026:
>
> Total bookings: 184
> Gross revenue: ₹147,200
> Refunds: ₹8,000
> Net revenue: ₹139,200

---

# 9. Core Product Features

## 9.1 Turf Management

The system should store:

- Business name
- Turf name
- Turf type
- Address
- Google Maps location
- Contact information
- Opening hours
- Closing hours
- Amenities
- Rules
- Photos
- Pricing

---

## 9.2 Availability Management

The system should manage:

- Turf operating hours
- Available slots
- Booked slots
- Blocked slots
- Temporary booking holds
- Maintenance periods
- Owner-defined unavailable periods

---

## 9.3 Booking Management

The booking system should support:

- Booking creation
- Booking confirmation
- Booking cancellation
- Booking expiry
- Booking completion
- No-show tracking
- Booking history

---

## 9.4 Payment Management

The payment system should support:

- Payment initiation
- Payment status tracking
- Payment verification
- Payment failure handling
- Payment timeout
- Refund processing
- Payment records

The system should not consider a booking confirmed merely because a payment link was generated.

A booking should be confirmed only after successful payment verification.

---

## 9.5 Customer Management

The system should maintain customer information required for booking operations.

Possible information includes:

- Customer name
- WhatsApp number
- Booking history
- Total bookings
- Cancellation history
- Payment history

Only necessary information should be stored.

---

## 9.6 Owner AI Assistant

The owner AI assistant should allow owners to interact using natural language.

Example commands:

- "Show today's bookings."
- "What is my revenue this month?"
- "Block Turf 1 tomorrow at 7 PM."
- "How many bookings do I have tomorrow?"
- "Show cancelled bookings."
- "Generate this month's report."

---

## 9.7 Customer AI Assistant

The customer AI assistant should understand natural language requests such as:

- "I want to play tomorrow."
- "Is 7 PM available?"
- "How much does it cost?"
- "Book it."
- "Cancel my booking."
- "Where is the turf?"

The AI should use backend tools to perform actions.

The AI must not directly modify the database.

All important operations must go through validated backend business logic.

---

## 9.8 Excel Reports

The system should support generating reports containing information such as:

- Booking records
- Customer records
- Revenue
- Payment status
- Cancellation records

Excel reports should initially be generated on demand.

Real-time Excel synchronization is not required for the MVP.

---

# 10. WhatsApp as the Primary Interface

WhatsApp will be the primary communication channel for the MVP.

The system architecture will use the WhatsApp Business Platform / Cloud API.

High-level architecture:

Customer
    ↓
WhatsApp
    ↓
WhatsApp Business Platform
    ↓
Webhook
    ↓
Spring Boot Backend
    ↓
AI Agent
    ↓
Booking Engine
    ↓
PostgreSQL

The same infrastructure should support owner conversations.

The system should be designed so that WhatsApp can be replaced or supplemented by another channel in the future without rewriting the core booking engine.

---

# 11. Multi-Tenant Product Vision

The system should be designed as a multi-tenant platform from the beginning.

Each turf business should have isolated business data.

Conceptually:

Business A
    ├── Turf 1
    ├── Turf 2
    ├── Bookings
    └── Customers

Business B
    ├── Turf 1
    ├── Turf 2
    └── Bookings

Data belonging to Business A must never be exposed to Business B.

The system should use a shared application architecture with logical data isolation.

The MVP should not create a separate backend deployment for every turf owner.

---

# 12. MVP Scope

The first MVP should focus only on the core booking workflow.

## Included in MVP

- Turf business onboarding
- Turf configuration
- Customer WhatsApp interaction
- Owner WhatsApp interaction
- AI-powered conversation
- Turf availability checking
- Slot booking
- Temporary slot holds
- Booking conflict prevention
- Payment integration
- Payment verification
- Booking confirmation
- Booking cancellation
- Owner booking lookup
- Basic revenue information
- Excel report generation
- Basic notifications
- Booking audit history

---

# 13. MVP Non-Goals

The following features should NOT be built initially.

## Public Marketplace

The MVP will not initially provide:

- Public turf marketplace
- City-wide turf discovery
- Customer mobile app
- Customer web application
- Turf comparison
- Public reviews
- Ratings

These may be considered after validating the core product.

---

## Advanced CRM

The MVP will not initially include:

- Marketing campaigns
- Customer segmentation
- Loyalty programs
- Referral programs
- Automated promotional campaigns

---

## Advanced Analytics

The MVP will not initially include:

- Complex dashboards
- Predictive analytics
- AI revenue forecasting
- Advanced business intelligence

---

## Multiple Communication Channels

The MVP will focus primarily on WhatsApp.

Other channels such as:

- Instagram
- Facebook Messenger
- Telegram
- SMS

are not part of the initial MVP.

---

# 14. Booking Principles

The booking system must follow these principles.

## Principle 1: No Double Booking

The system must prevent two confirmed bookings from occupying the same turf and time range.

---

## Principle 2: Payment Before Confirmation

A booking should not become confirmed until the payment is successfully verified.

---

## Principle 3: Temporary Booking Hold

When a customer starts payment, the slot may be temporarily held.

Example:

10-minute payment window.

If payment is not completed:

    PAYMENT_PENDING
        ↓
    HOLD EXPIRES
        ↓
    SLOT AVAILABLE

---

## Principle 4: Backend Is the Source of Truth

The AI is not responsible for determining whether a slot is available.

The backend booking engine is the source of truth.

The correct flow is:

AI
    ↓
Availability Tool
    ↓
Booking Service
    ↓
Database

---

## Principle 5: AI Cannot Bypass Business Rules

The AI must not be able to:

- Confirm unavailable slots
- Change payment status
- Bypass payment requirements
- Cancel another customer's booking without authorization
- Access another business's data

All sensitive actions must be validated by the backend.

---

# 15. Success Metrics

The MVP should be considered successful if the following goals are achieved.

## Product Validation

- At least 1 real turf owner successfully onboarded
- At least 1 turf actively using the system
- At least 10 real bookings processed
- At least 5 successful real payments
- No confirmed double bookings

---

## Customer Experience

Target:

- Customer receives availability response quickly
- Booking process requires minimal human intervention
- Customer receives immediate confirmation after payment

---

## Owner Experience

The turf owner should be able to perform common tasks without manually checking a spreadsheet.

Examples:

> "Show today's bookings."

> "Block tomorrow 8 PM."

> "How much did I earn this week?"

---

## Business Validation

The product should demonstrate that turf owners are willing to use and eventually pay for the service.

Initial validation questions:

- Does the owner save time?
- Does the system reduce fake bookings?
- Does the owner trust the booking system?
- Does the owner prefer WhatsApp-based management?
- Is the owner willing to pay for the service?

---

# 16. Initial Pilot Strategy

The first pilot should focus on Kolhapur.

Target:

    1 turf owner
        ↓
    1 turf business
        ↓
    Real customer bookings
        ↓
    Real payment testing
        ↓
    Feedback
        ↓
    Product improvements

After validating the first turf:

    1 turf
        ↓
    3 turfs
        ↓
    5 turfs
        ↓
    10+ turfs
        ↓
    Expand beyond Kolhapur

The goal is not to build a large platform before validation.

The goal is to prove that the system solves a real problem for real turf owners.

---

# 17. Long-Term Vision

If the MVP succeeds, the product can evolve into a broader sports facility management and discovery platform.

Potential future features include:

- Public turf marketplace
- Customer web application
- Customer mobile application
- Turf discovery
- Turf search
- Location-based search
- Reviews and ratings
- Online turf comparison
- Multiple sports facilities
- Cricket grounds
- Badminton courts
- Basketball courts
- Sports academies
- Subscription management
- Advanced analytics
- Customer loyalty programs
- Multi-channel AI agents
- Voice-based booking
- AI-powered business insights

The long-term vision is:

> Build an AI-first operating system for local sports facility businesses.

However, these features should only be developed after validating the core WhatsApp booking product.

---

# 18. Product Principles

The project should follow these principles.

### 1. Validate Before Scaling

Do not build complex features before validating them with real turf owners.

### 2. WhatsApp First

Use WhatsApp as the primary customer and owner interface during the MVP.

### 3. Backend First

The booking engine and business rules must be reliable independently of the AI.

### 4. AI as an Interface

AI should understand natural language and call backend tools.

AI should not directly control critical business logic.

### 5. Payment-Verified Booking

A booking should only be confirmed after successful payment verification.

### 6. Prevent Double Bookings

The booking engine must enforce conflict prevention at the database and backend levels.

### 7. Multi-Tenant by Design

Each turf business must have isolated data.

### 8. Keep the MVP Simple

Avoid building a large marketplace or mobile application until the core workflow is validated.

### 9. Security First

Customer, owner, payment, and business data must be protected.

### 10. Build for Expansion

The architecture should allow future expansion to multiple cities and sports without requiring a complete rewrite.

---

# 19. High-Level System Architecture

The initial conceptual architecture is:

Customer
    │
    ▼
WhatsApp
    │
    ▼
WhatsApp Business Platform
    │
    ▼
Webhook
    │
    ▼
Spring Boot Backend
    │
    ├── Authentication & RBAC
    │
    ├── Business Management
    │
    ├── Turf Management
    │
    ├── Availability Service
    │
    ├── Booking Service
    │
    ├── Payment Service
    │
    ├── Customer Service
    │
    ├── Owner Service
    │
    └── Reporting Service
    │
    ▼
PostgreSQL

AI Agent
    │
    └── Calls validated backend tools

n8n
    │
    ├── Notifications
    ├── Reminders
    ├── Excel automation
    └── Background workflows

Payment Gateway
    │
    └── Payment verification via webhook

Excel
    │
    └── On-demand business reports