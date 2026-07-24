# Turf AI Booking — Owner Onboarding

**Document:** 06-owner-onboarding.md  
**Version:** 2.0  
**Status:** Approved  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines how a football turf business is onboarded into the Turf AI Booking platform.

The onboarding process ensures:

- Business identity is established
- Owner identity is verified
- WhatsApp is connected
- AI Agent is activated
- Payment gateway is configured
- Booking engine is initialized
- Pricing is configured
- Operating hours are configured
- Reports are enabled

The onboarding process should be simple enough to complete in less than 15 minutes.

---

# 2. MVP Philosophy

The MVP focuses on rapid onboarding.

Target:

> A turf owner should be able to start accepting bookings within 15 minutes.

No technical knowledge should be required.

The owner should only need:

- Smartphone
- WhatsApp
- Bank Account
- UPI ID (optional initially)

---

# 3. High-Level Onboarding Flow

Owner Interested
        ↓
Business Created
        ↓
Owner Registered
        ↓
WhatsApp Verified
        ↓
Business Configured
        ↓
Pricing Configured
        ↓
Operating Hours Configured
        ↓
Payment Gateway Connected
        ↓
AI Agent Activated
        ↓
Ready for Booking

---

# 4. Business Registration

Every turf business must have a Business Profile.

Required fields:

- Business Name
- Business Address
- City
- State
- Pincode
- Google Maps Link
- Contact Number

Optional fields:

- GST Number
- Business Registration Number
- Website
- Instagram

---

# 5. Owner Registration

Each business must have exactly one Owner during MVP.

Required Information:

- Full Name
- Mobile Number
- WhatsApp Number
- Email (optional)
- Preferred Language

Example

Name:

Rahul Patil

WhatsApp:

+91XXXXXXXXXX

Language:

English

---

# 6. Business Verification

During MVP:

Business verification is manual.

Administrator verifies:

- Business exists
- Contact number belongs to owner
- Turf location is genuine

Future:

Business document verification.

---

# 7. WhatsApp Verification

Owner sends:

"Hi"

↓

System checks registered number

↓

Number Found?

YES

↓

Owner Verified

If not registered:

System informs:

"Your number is not registered as a Turf Owner."

---

# 8. Business Configuration

The owner provides:

- Turf Name
- Number of Turfs
- Turf Type

Example

Business

ABC Football Arena

↓

Turf 1

5v5

↓

Turf 2

7v7

Each turf is created separately.

---

# 9. Operating Hours Setup

Owner configures operating hours **per turf** (not per business).

Each turf has separate hours for each day of the week.

Opening Time

Closing Time

Example

Monday

06:00 AM

↓

11:00 PM

Weekend hours may differ.

---

# 10. Slot Configuration

Default:

60-minute slots

Example

06:00–07:00

07:00–08:00

08:00–09:00

Owner may change slot duration in future versions.

---

# 11. Pricing Configuration

Owner enters:

Weekday Price

Weekend Price

Peak Hour Price

Example

Weekday

₹800

Weekend

₹1000

Peak

₹1200

---

# 12. Amenities Setup

Owner selects available amenities.

Examples:

- Parking
- Washroom
- Drinking Water
- Flood Lights
- Changing Room
- Seating Area
- Cafeteria

These appear in customer responses.

---

# 13. Business Rules Setup

Owner configures:

- Advance Booking Days
- Cancellation Window
- Booking Hold Duration
- Reminder Time

Default values:

Advance Booking:

30 Days

Cancellation:

2 Hours

Hold:

10 Minutes

Reminder:

2 Hours

---

# 14. Payment Gateway Setup

Owner connects payment account.

Required:

- Gateway Account
- Settlement Account
- UPI ID (optional)

Future:

Owner Portal for payment configuration.

---

# 15. WhatsApp AI Activation

After onboarding:

AI Agent becomes active.

Customer sends:

"Book Turf"

↓

AI responds

↓

Checks availability

↓

Creates booking

↓

Requests payment

---

# 16. Owner AI Activation

Owner can immediately use WhatsApp.

Examples

"Today's bookings"

"Tomorrow's revenue"

"Block Turf 2 at 7 PM"

"How many bookings this week?"

---

# 17. Excel Integration

For MVP:

Every booking is stored in Excel.

Sheets:

Bookings

Payments

Customers

Daily Summary

Monthly Summary

Excel is generated automatically.

---

# 18. Customer Data

Owner may view:

- Customer Name
- WhatsApp Number
- Booking History
- Payment Status

Owner cannot edit historical bookings.

---

# 19. Welcome Message

After onboarding:

Welcome to Turf AI Booking!

Your AI Booking Assistant is now active.

Customers can now:

✅ Check Availability

✅ Book Slots

✅ Pay Online

✅ Receive Booking Confirmation

---

# 20. Owner Training

Owner receives:

Quick Start Guide

Topics:

- Booking Flow
- Payment Flow
- Cancellation
- Reports
- AI Commands

Training should take less than 10 minutes.

---

# 21. AI Owner Commands

Examples

Bookings

- Show today's bookings
- Show tomorrow's bookings

Revenue

- Today's revenue
- Monthly revenue

Availability

- Is Turf 1 free tomorrow at 8 PM?

Operations

- Block Turf 2 tomorrow
- Unblock Turf 1

Reports

- Send today's Excel report
- Send monthly report

---

# 22. Initial Data Created

When onboarding completes:

Business

↓

Owner

↓

Turfs

↓

Operating Hours

↓

Pricing Rules

↓

Default Slot Configuration

↓

WhatsApp Identity

↓

AI Configuration

↓

Excel Workspace

---

# 23. Offboarding

If owner stops subscription:

Business

↓

Disabled

↓

Bookings preserved

↓

AI disabled

↓

WhatsApp inactive

Data retained according to retention policy.

---

# 24. Re-Onboarding

Returning owner:

Verify WhatsApp

↓

Reactivate Business

↓

Restore Configuration

↓

AI Activated

No need to recreate the business.

---

# 25. Multi-Turf Support

A single owner may manage multiple turfs.

Example

ABC Sports Arena

├── Turf 1

├── Turf 2

├── Turf 3

All managed through one WhatsApp account.

---

# 26. Future Enhancements

Future onboarding may include:

- Self-service registration
- KYC verification
- GST validation
- QR Code generation
- Digital agreements
- Owner dashboard
- Multiple managers
- Subscription billing
- Auto payment gateway setup

---

# 27. Success Criteria

Owner onboarding is successful when:

✓ Business created

✓ Owner verified

✓ WhatsApp verified

✓ Turf configured

✓ Pricing configured

✓ Operating hours configured

✓ Payment gateway connected

✓ AI activated

✓ Test booking completed

---

# 28. Onboarding Checklist

| Step | Status |
|-------|--------|
| Business Created | ☐ |
| Owner Registered | ☐ |
| WhatsApp Verified | ☐ |
| Turf Created | ☐ |
| Operating Hours Set | ☐ |
| Pricing Configured | ☐ |
| Payment Gateway Connected | ☐ |
| AI Activated | ☐ |
| Test Booking Successful | ☐ |

---

# 29. MVP Assumptions

The MVP assumes:

- One owner per business
- One WhatsApp number per owner
- Manual business verification
- Manual payment gateway setup
- Excel-based reporting
- WhatsApp-first management
- No web dashboard required

These assumptions can be relaxed in future versions.

---

# 30. Next Document

The next document is:

`docs/07-whatsapp.md`

This document will define:

- WhatsApp Business integration
- Webhook architecture
- Message lifecycle
- Interactive buttons
- Templates
- Session management
- Media handling
- Duplicate message detection
- Retry mechanisms
- WhatsApp security
- Customer conversation flows
- Owner conversation flows
- Rate limiting