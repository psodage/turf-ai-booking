# Turf AI Booking — Customer Cancellation Conversation

**Document:** conversations/customer-cancellation.md
**Version:** 1.0
**Status:** Production Ready
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the complete customer booking cancellation journey through the WhatsApp AI Agent.

It specifies:

- Cancellation conversations
- Business validations
- Refund rules
- Backend tool calls
- AI responses
- Error handling
- State transitions

This document is the source of truth for all booking cancellation scenarios.

---

# 2. Objectives

The cancellation experience should be:

- Fast
- Transparent
- Fair
- Fully automated
- Audit-friendly

The AI must clearly communicate:

- Whether cancellation is allowed
- Whether a refund is applicable
- Expected refund timeline

---

# 3. Cancellation State Machine

BOOKING_CONFIRMED
        │
        ▼
CUSTOMER_REQUEST
        │
        ▼
VALIDATE_BOOKING
        │
        ▼
CHECK_CANCELLATION_POLICY
        │
        ├───────────────┐
        ▼               ▼
ALLOWED          NOT_ALLOWED
        │
        ▼
PROCESS_CANCELLATION
        │
        ▼
REFUND_CHECK
        │
        ├───────────────┐
        ▼               ▼
REFUNDABLE     NON_REFUNDABLE
        │
        ▼
NOTIFY_OWNER
        │
        ▼
UPDATE_REPORTS
        │
        ▼
CANCELLED

---

# 4. Happy Path

Customer

↓

Cancel Booking

↓

AI

↓

Find Booking

↓

Validate

↓

Eligible

↓

Cancel

↓

Refund

↓

Confirmation

---

# 5. Conversation Start

Customer

Cancel my booking

---

AI

Sure.

Please select the booking you wish to cancel.

---

# 6. Fetch Customer Bookings

Tool

getMyBookings()

Returns

Booking Number

Date

Time

Status

---

Example

1️⃣ BK-2026-00125

Tomorrow

7 PM

Confirmed

---

# 7. Customer Selection

Customer

1

↓

Tool

getBookingDetails()

---

# 8. Booking Validation

System validates

✓ Booking Exists

✓ Customer Owns Booking

✓ Status = CONFIRMED

✓ Booking Not Completed

✓ Booking Not Already Cancelled

---

If validation fails

Return friendly message.

---

# 9. Cancellation Policy Check

Tool

checkCancellationEligibility()

Checks

Current Time

↓

Booking Time

↓

Cancellation Window

↓

Business Rules

---

# 10. Cancellation Allowed

AI

Your booking is eligible for cancellation.

Refund Policy

100% Refund

Would you like to continue?

Buttons

✅ Yes

❌ No

---

# 11. Customer Confirms

Customer

Yes

↓

Backend

cancelBooking()

---

# 12. Booking Status Update

Booking Status

CONFIRMED

↓

CANCELLED

Audit entry created.

---

# 13. Refund Decision

Tool

calculateRefund()

Possible outcomes

100%

50%

0%

Future:

Dynamic refund policies.

---

# 14. Refund Processing

If refundable

↓

createRefund()

↓

Payment Gateway

↓

Refund Initiated

---

# 15. Customer Confirmation

AI

✅ Your booking has been cancelled.

Booking ID

BK-2026-00125

Refund

₹800

Expected within

5–7 business days.

---

# 16. Non-Refundable Cancellation

AI

Your booking has been cancelled.

As per the cancellation policy,

this booking is not eligible for a refund.

---

# 17. Cancellation Not Allowed

Example

Customer attempts cancellation

30 minutes before match.

AI

Sorry,

this booking can no longer be cancelled.

Please contact the turf owner if you need assistance.

---

# 18. Booking Already Cancelled

AI

This booking has already been cancelled.

No further action is required.

---

# 19. Booking Completed

AI

Completed bookings cannot be cancelled.

---

# 20. Booking Not Found

AI

I couldn't find that booking.

Please select a valid booking.

---

# 21. Invalid Booking Number

Customer

ABC123

AI

That doesn't appear to be a valid booking.

Please choose from the list provided.

---

# 22. Owner Notification

After successful cancellation

↓

Owner receives

Booking Cancelled

Customer Name

Date

Time

Refund Status

---

# 23. Excel Update

Cancelled booking appears in

Bookings Sheet

Status

CANCELLED

Revenue updated automatically.

---

# 24. Audit Logging

Create entry

Booking ID

Old Status

CONFIRMED

↓

New Status

CANCELLED

↓

Timestamp

↓

Cancelled By

Customer

---

# 25. Backend Tool Calls

| Tool | Purpose |
|------|----------|
| getMyBookings() | Fetch customer bookings |
| getBookingDetails() | Retrieve booking information |
| checkCancellationEligibility() | Validate cancellation rules |
| cancelBooking() | Cancel booking |
| calculateRefund() | Determine refund amount |
| createRefund() | Initiate payment gateway refund |
| notifyOwner() | Notify turf owner |
| updateReports() | Refresh reporting data |

---

# 26. Business Rules

The AI must never:

- Cancel another customer's booking
- Bypass cancellation window
- Guess refund amount
- Confirm refund before backend verification
- Delete booking records

Bookings are never deleted.

Only status changes.

---

# 27. Error Handling

Payment Gateway Down

↓

Refund queued

↓

Customer informed

---

Database Failure

↓

Rollback

↓

Booking remains confirmed

---

Notification Failure

↓

Retry

↓

Booking still cancelled

---

# 28. Edge Cases

Customer pays after refund initiated

↓

Duplicate cancellation request

↓

Booking already refunded

↓

Owner manually cancelled booking

↓

Refund webhook delayed

↓

Customer inactive during flow

↓

Booking expires during conversation

---

# 29. Success Criteria

Cancellation is successful when

✓ Booking validated

✓ Policy checked

✓ Booking status updated

✓ Audit entry created

✓ Refund initiated (if applicable)

✓ Owner notified

✓ Reports updated

✓ Customer notified

---

# 30. Future Enhancements

- Partial cancellations
- Reschedule instead of cancel
- AI-assisted retention offers
- Instant UPI refunds
- Cancellation analytics
- Self-service refund tracking

---

# 31. QA Test Scenarios

| Scenario | Expected Result |
|-----------|-----------------|
| Cancel within allowed window | Booking cancelled, refund initiated |
| Cancel outside allowed window | Cancellation denied |
| Cancel already cancelled booking | Inform customer |
| Cancel completed booking | Denied |
| Invalid booking number | Validation error |
| Payment gateway unavailable | Refund queued |
| Duplicate cancellation request | Process once only |
| Owner notification failure | Retry notification |
| Refund webhook delayed | Booking remains cancelled |

---

# End of Document