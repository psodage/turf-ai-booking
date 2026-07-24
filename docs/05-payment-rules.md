# Turf AI Booking — Payment Rules

**Document:** 05-payment-rules.md  
**Version:** 1.0  
**Status:** Draft  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the payment lifecycle for Turf AI Booking.

It covers:

- Payment initiation
- Payment authorization
- Payment verification
- Payment confirmation
- Payment failure
- Payment timeout
- Refunds
- Duplicate payments
- Late payments
- Idempotency
- Security
- Reconciliation

The payment system is responsible only for payment processing.

The Booking Service remains the single source of truth for booking confirmation.

---

# 2. Payment Principle

The booking engine owns bookings.

The payment engine owns payments.

The payment gateway owns transaction execution.

Example:

Customer
    ↓
Booking Service
    ↓
Create Booking Hold
    ↓
Payment Service
    ↓
Payment Gateway
    ↓
Webhook
    ↓
Payment Service
    ↓
Booking Service
    ↓
Confirm Booking

---

# 3. Payment Requirement

For the MVP:

> Every booking requires successful payment before confirmation.

No unpaid confirmed bookings are allowed.

Possible future feature:

- Pay at Turf

Not supported in MVP.

---

# 4. Payment Flow

Customer
    ↓
Select Slot
    ↓
Booking Hold Created
    ↓
Payment Order Created
    ↓
Customer Pays
    ↓
Gateway Processes Payment
    ↓
Webhook Received
    ↓
Signature Verified
    ↓
Payment Updated
    ↓
Booking Confirmed

---

# 5. Booking Hold Before Payment

The booking hold must exist before payment begins.

Example:

Booking Hold

Status:

HELD

Expires:

10 Minutes

Only after the hold exists should the payment link be generated.

---

# 6. Payment Order

Every payment attempt creates a Payment Order.

Example

Payment Order ID:
PAY-001

Booking ID:
BOOK-001

Amount:
₹800

Status:
CREATED

---

# 7. Payment Status

Possible statuses:

CREATED

↓

PENDING

↓

SUCCESS

↓

FAILED

↓

EXPIRED

↓

REFUNDED

↓

PARTIALLY_REFUNDED

Each payment has exactly one current status.

---

# 8. Payment Lifecycle

CREATED
    ↓
PENDING
    ↓
SUCCESS

Alternative:

PENDING
    ↓
FAILED

or

PENDING
    ↓
EXPIRED

or

SUCCESS
    ↓
REFUNDED

---

# 9. Payment Amount

The payment amount is locked when the payment order is created.

Example

Slot Price:

₹900

Owner changes price:

₹1000

Existing payment:

₹900

New bookings:

₹1000

Price changes never affect existing payment orders.

---

# 10. Payment Verification

The system must never trust the client.

Wrong:

Customer

"I paid."

↓

Booking Confirmed

Correct:

Gateway

↓

Webhook

↓

Verify Signature

↓

Verify Amount

↓

Verify Transaction

↓

Confirm Payment

↓

Booking Service

---

# 11. Webhook Principle

The webhook is the authoritative source of payment completion.

Customer success page is NOT authoritative.

Correct:

Gateway

↓

Webhook

↓

Booking Confirmation

Wrong:

Browser Redirect

↓

Booking Confirmation

---

# 12. Webhook Security

Every webhook must verify:

- Signature
- Secret
- Timestamp
- Event Type

Invalid signature:

Reject immediately.

---

# 13. Payment Confirmation Rules

Payment becomes SUCCESS only if:

- Signature valid
- Amount correct
- Currency correct
- Payment ID unique
- Booking exists
- Booking Hold active

Otherwise:

Manual review or refund.

---

# 14. Payment Idempotency

Duplicate webhooks must not duplicate work.

Gateway

↓

SUCCESS

↓

SUCCESS

↓

SUCCESS

Booking still confirmed only once.

---

# 15. Duplicate Payment Protection

A payment transaction ID must be unique.

Example

Transaction

TXN12345

Already processed?

YES

↓

Ignore

---

# 16. Booking Confirmation Rules

Payment Service cannot directly update booking.

Instead:

Payment Success

↓

Booking Service

↓

Check Hold

↓

Check Slot

↓

Confirm Booking

Only Booking Service may change booking status.

---

# 17. Hold Expired Before Payment

Scenario

7:00

Hold Created

↓

7:10

Hold Expired

↓

7:12

Customer Pays

Process

Payment Success

↓

Booking Service

↓

Hold Expired?

↓

YES

↓

Booking NOT confirmed

↓

Refund Initiated

This prevents overselling.

---

# 18. Slot Already Booked

Example

Customer A

Hold

↓

Expired

↓

Customer B books

↓

Confirmed

↓

Customer A pays later

Result

Payment accepted by gateway

↓

Booking impossible

↓

Refund

---

# 19. Payment Failure

Reasons include:

- Card Declined
- UPI Failed
- Timeout
- Insufficient Balance
- User Cancelled

Booking remains:

PAYMENT_FAILED

Hold expires automatically.

---

# 20. Payment Timeout

Default:

10 Minutes

After timeout:

Payment

↓

Expired

↓

Booking Hold Released

↓

Slot Available

---

# 21. Refund Rules

Refund may occur because:

- Owner cancellation
- Late payment
- Duplicate payment
- Manual resolution

Refunds should always reference:

Booking ID

Payment ID

Refund ID

Reason

---

# 22. Refund Status

Possible values:

NOT_REQUIRED

REQUESTED

PROCESSING

SUCCESS

FAILED

---

# 23. Partial Refund

Future feature.

Example

Booking:

₹1000

Refund:

₹500

Remaining:

₹500

Not required in MVP.

---

# 24. Duplicate Customer Clicks

Customer presses Pay 5 times.

Backend should return:

Existing Payment Order

NOT

Create 5 payment orders.

---

# 25. Payment Order Expiration

Expired payment orders cannot be reused.

Example

Created:

7:00

Expired:

7:10

Customer opens old payment link

↓

Reject

---

# 26. Currency

Initial MVP:

INR only.

Future:

Multi-currency.

---

# 27. Payment Gateway

Gateway should support:

- UPI
- Cards
- Net Banking
- Wallets

Recommended:

Razorpay

Future support:

PhonePe
Cashfree
PayU

---

# 28. Payment Audit Log

Every payment event should be logged.

Example

Payment Created

↓

Payment Pending

↓

Webhook Received

↓

Signature Verified

↓

Payment Success

↓

Booking Confirmed

---

# 29. Payment Record

Every payment stores:

- Payment ID
- Booking ID
- Business ID
- Customer ID
- Amount
- Currency
- Gateway
- Gateway Order ID
- Gateway Payment ID
- Status
- Created At
- Updated At

---

# 30. Reconciliation

Daily reconciliation should compare:

Gateway Payments

vs

Database Payments

Missing records should be flagged.

---

# 31. Owner View

Owner may see:

- Booking
- Amount
- Payment Status
- Payment Time

Owner cannot manually change payment status.

---

# 32. Customer View

Customer may view:

- Amount
- Status
- Booking ID
- Payment Time

Customer cannot edit payment information.

---

# 33. AI Rules

Customer AI may:

- Generate payment link
- Check payment status
- Explain payment process

AI cannot:

- Mark payment successful
- Mark refund complete
- Change payment amount

---

# 34. Security Rules

Never trust:

- Browser
- WhatsApp Message
- Customer Input

Trust only:

- Verified Webhook
- Backend Validation

---

# 35. Failed Webhook Retry

If webhook processing fails:

Gateway retries.

Backend must safely process repeated events.

---

# 36. Payment Service Responsibilities

Payment Service is responsible for:

- Creating payment orders
- Receiving webhooks
- Signature validation
- Updating payment status

It is NOT responsible for confirming bookings.

---

# 37. Booking Service Responsibilities

Booking Service is responsible for:

- Creating booking holds
- Slot locking
- Conflict checking
- Booking confirmation
- Booking cancellation

---

# 38. Notification Flow

Payment Success

↓

Booking Confirmed

↓

Customer WhatsApp

↓

Owner WhatsApp

Only after booking confirmation.

---

# 39. Failure Notification

Customer should receive:

❌ Payment Failed

Your booking has not been confirmed.

Please try again.

---

# 40. Success Notification

Customer receives:

✅ Payment Successful

Booking Confirmed

Booking ID

Date

Time

Location

---

# 41. Payment Sequence Diagram

Customer
    ↓
Booking Service
    ↓
Create Hold
    ↓
Payment Service
    ↓
Gateway
    ↓
Customer Pays
    ↓
Gateway
    ↓
Webhook
    ↓
Payment Service
    ↓
Verify Signature
    ↓
Booking Service
    ↓
Confirm Booking
    ↓
Customer
    ↓
Owner

---

# 42. MVP Payment Rules Summary

For MVP:

- Full payment required
- No Cash on Arrival
- One payment per booking
- 10-minute payment window
- Webhook-based confirmation
- Duplicate payment protection
- Automatic refund for late payments
- Payment amount locked
- Razorpay Integration
- WhatsApp notifications
- Audit logging enabled

---

# 43. Future Enhancements

- Partial payments
- Advance payment percentage
- Security deposit
- Split payments
- Corporate billing
- Subscription billing for owners
- Automatic settlement reports

---

# 44. Next Document

The next document is:

`docs/06-owner-onboarding.md`

This document defines:

- Turf owner registration
- Business verification
- WhatsApp verification
- AI agent activation
- Payment gateway setup
- Initial business configuration
- Pricing setup
- Operating hours setup
- Excel integration
- Owner training workflow