# Turf AI Booking — Owner AI Agent Conversation

**Document:** conversations/owner-agent.md
**Version:** 2.0
**Status:** Approved
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines how the Owner AI Agent interacts with turf owners through WhatsApp.

The Owner AI acts as a virtual business assistant capable of:

- Managing bookings
- Checking today's schedule
- Blocking slots
- Updating pricing
- Viewing revenue
- Generating reports
- Answering business queries
- Configuring business settings

This document is the source of truth for all owner conversations.

---

# 2. Objectives

The Owner AI should:

- Reduce manual work
- Eliminate dashboards for daily tasks
- Respond in under 5 seconds
- Execute only authorized actions
- Always verify critical operations

---

# 3. Owner Agent State Machine

START
  │
  ▼
AUTHENTICATE_OWNER
  │
  ▼
IDENTIFY_INTENT
  │
  ├────────────┬────────────┬────────────┬────────────┐
  ▼            ▼            ▼            ▼
Bookings    Revenue     Reports     Settings
  │            │            │            │
  └────────────┴────────────┴────────────┘
                │
                ▼
          Execute Tool
                │
                ▼
      Confirm Result
                │
                ▼
               END

---

# 4. Supported Intents

The AI supports:

- Today's bookings
- Upcoming bookings
- Booking details
- Revenue summary
- Block slots
- Unblock slots
- Update pricing
- Generate reports
- Business information
- Customer lookup
- Help

---

# 5. Conversation Start

Owner

Hi

---

AI

👋 Welcome back, {{Owner Name}}

How can I help you today?

Examples:

📅 Today's bookings

💰 Today's revenue

📊 Generate report

🚫 Block a slot

💵 Update pricing

---

# 6. Today's Bookings

Owner

Today's bookings

---

Tool

getTodayBookings()

---

AI

Today's Schedule

• 6 PM - Rohit (Paid)

• 7 PM - Amit (Paid)

• 8 PM - Available

• 9 PM - Rahul (Pending)

---

# 7. Booking Details

Owner

Show booking for 7 PM

---

Tool

getBookingDetails()

---

AI

Booking Details

Customer: Amit

Phone: ********45

Date: 25 July

Time: 7 PM

Status: Confirmed

Payment: Paid

Amount: ₹800

---

# 8. Revenue Summary

Owner

Today's revenue

---

Tool

getRevenue()

---

AI

Today's Revenue

Confirmed Bookings: 6

Total Revenue: ₹4,800

Pending Payments: ₹800

Cancelled Bookings: 1

---

# 9. Weekly Revenue

Owner

This week's revenue

---

Tool

getWeeklyRevenue()

---

AI

Weekly Summary

Revenue: ₹28,400

Bookings: 34

Average Daily Revenue: ₹4,057

---

# 10. Monthly Revenue

Owner

This month's revenue

---

Tool

getMonthlyRevenue()

---

AI

Monthly Summary

Revenue: ₹1,12,000

Bookings: 146

Occupancy: 82%

---

# 11. Block Slot

Owner

Block tomorrow 7 PM

---

Tool

blockSlot()

---

Validation

✓ Owner Authorized

✓ Slot Exists

✓ No Confirmed Booking

---

AI

Slot blocked successfully.

Tomorrow

7 PM

No further bookings will be accepted.

---

# 12. Slot Already Booked

Owner

Block today 7 PM

---

Tool

blockSlot()

---

AI

This slot already has a confirmed booking.

Please cancel the booking before blocking the slot.

---

# 13. Unblock Slot

Owner

Unblock tomorrow 7 PM

---

Tool

unblockSlot()

---

AI

Slot is now available for booking.

---

# 14. Update Pricing

Owner

Weekend price is ₹1000

---

Tool

updatePricing()

---

AI

Pricing updated successfully.

Weekend Rate

₹1000

---

# 15. Generate Daily Report

Owner

Send today's report

---

Tool

generateDailyExcel()

---

AI

Generating report...

---

Tool

sendExcel()

---

AI

✅ Report sent successfully.

---

# 16. Generate Weekly Report

Owner

Weekly report

---

Tool

generateWeeklyExcel()

---

AI

Preparing your report...

Report sent.

---

# 17. Customer Lookup

Owner

Show Rohit's booking

---

Tool

searchCustomerBooking()

---

AI

Customer

Rohit

Upcoming Booking

Tomorrow

7 PM

Paid

---

# 18. Business Information

Owner

What are my operating hours?

---

Tool

getBusinessSettings()

---

AI

Operating Hours

6 AM - 11 PM

---

# 19. Booking Statistics

Owner

Booking statistics

---

Tool

getBookingStatistics()

---

AI

Today's Stats

Bookings: 8

Cancelled: 1

Available Slots: 4

Occupancy: 75%

---

# 20. AI Recommendations

Owner

Any suggestions?

---

Tool

generateBusinessInsights()

---

AI

Suggestions

• Weekend evenings are fully booked.

• Tuesday has low occupancy.

• Consider offering weekday discounts.

---

# 21. Help

Owner

Help

---

AI

I can help you with:

📅 Today's bookings

💰 Revenue

📊 Reports

🚫 Block slots

💵 Pricing

📈 Business insights

---

# 22. Authentication

Every request validates:

✓ WhatsApp Number

✓ Owner Role

✓ Business ID

Unauthorized users cannot access owner functions.

---

# 23. Backend Tool Calls

| Tool | Purpose |
|------|----------|
| getTodayBookings | List today's bookings |
| getBookingDetails | Booking information |
| getRevenue | Revenue summary |
| getWeeklyRevenue | Weekly revenue |
| getMonthlyRevenue | Monthly revenue |
| blockSlot | Block slot |
| unblockSlot | Unblock slot |
| updatePricing | Update pricing |
| generateExcelReport | Generate report |
| searchCustomerBooking | Customer lookup |
| getBusinessSettings | Business configuration |
| getBookingStatistics | Occupancy statistics |

---

# 24. Business Rules

The AI must never:

- Delete bookings
- Edit confirmed payments
- Access another business
- Reveal customer payment details
- Modify historical reports
- Override business rules

All actions require backend validation.

---

# 25. Error Handling

Database unavailable

↓

AI

I'm unable to retrieve your business data right now.

Please try again shortly.

---

Excel generation failed

↓

Retry

↓

If failed

↓

Notify owner

---

Unauthorized request

↓

Access denied

↓

Audit log created

---

# 26. Audit Logging

Every owner action records:

- Timestamp
- Owner ID
- Business ID
- Action
- Parameters
- Result
- Correlation ID

---

# 27. Edge Cases

- Slot already blocked
- Slot already booked
- Invalid pricing
- Report generation failure
- Revenue unavailable
- Customer not found
- Duplicate commands
- WhatsApp retry event

---

# 28. Success Criteria

Owner interaction is successful when:

✓ Request authenticated

✓ Intent identified

✓ Tool executed

✓ Business rules validated

✓ Audit log created

✓ Owner receives confirmation

---

# 29. Future Enhancements

- Voice commands
- Marathi language support
- Hindi language support
- AI revenue forecasting
- Demand prediction
- Dynamic pricing
- Tournament management
- Staff management
- Expense tracking
- Google Calendar sync
- Google Sheets sync

---

# 30. Sample Owner Commands

| Owner Message | AI Action |
|---------------|-----------|
| Today's bookings | Show today's schedule |
| Tomorrow's bookings | Show tomorrow's bookings |
| Today's revenue | Revenue summary |
| Weekly revenue | Weekly analytics |
| Monthly revenue | Monthly analytics |
| Block tomorrow 7 PM | Block slot |
| Unblock tomorrow 7 PM | Make slot available |
| Weekend price ₹1000 | Update pricing |
| Generate report | Create Excel report |
| Show Rohit's booking | Search booking |
| Business hours | Display operating hours |
| Help | Show supported commands |

---

# End of Document