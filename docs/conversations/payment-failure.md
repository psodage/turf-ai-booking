# Turf AI Booking — Payment Failure Conversation

**Document:** conversations/payment-failure.md
**Version:** 1.0
**Status:** Production Ready
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines how the AI Agent handles payment failures during the booking process.

It covers:

- Payment failures
- Payment timeouts
- Gateway errors
- Retry flows
- Booking hold expiry
- Refund scenarios
- Duplicate payments
- Webhook failures
- Customer communication

This document is the source of truth for all payment-related conversations.

---

# 2. Objectives

The payment experience should be:

- Secure
- Transparent
- Recoverable
- User-friendly
- Fully auditable

The AI must never confirm a booking until payment has been verified.

---

# 3. Payment State Machine

BOOKING_HOLD_CREATED
        │
        ▼
PAYMENT_PENDING
        │
 ┌──────┼──────────────┬─────────────┐
 ▼      ▼              ▼             ▼
SUCCESS FAILURE     TIMEOUT    GATEWAY_ERROR
 │        │              │             │
 ▼        ▼              ▼             ▼
VERIFY   RETRY     HOLD_EXPIRED    RETRY
 │        │              │             │
 ▼        ▼              ▼             ▼
CONFIRM  CANCEL      RELEASE_SLOT  CANCEL
BOOKING  BOOKING

---

# 4. Happy Path

Customer

↓

Booking Hold

↓

Payment Link

↓

Payment Success

↓

Webhook Verification

↓

Booking Confirmed

↓

Notification Sent

---

# 5. Payment Link Generated

AI

Your booking has been reserved for **10 minutes**.

Please complete your payment using the secure link below.

🔗 Payment Link

---

# 6. Customer Does Not Pay

Booking Hold

↓

10 Minutes

↓

Expired

↓

Release Slot

↓

AI

Your booking session has expired.

The slot is now available for other customers.

Please start a new booking if you still wish to play.

---

# 7. Payment Failed

Possible reasons:

- Insufficient balance
- Wrong UPI PIN
- Bank declined transaction
- Card declined
- User cancelled payment

---

AI

❌ Your payment was not successful.

No amount has been deducted from your account.

Would you like to try again?

Buttons

- Retry Payment
- Cancel Booking

---

# 8. Customer Retries Payment

Customer

Retry Payment

↓

Backend

Generate New Payment Link

↓

AI

Here is your new payment link.

This link is valid for 10 minutes.

---

# 9. Payment Gateway Timeout

Gateway

↓

No response

↓

Payment status unknown

↓

AI

We're checking your payment status.

Please wait a moment.

Do not make another payment yet.

---

Backend

verifyPaymentStatus()

---

Possible results

- Paid
- Failed
- Pending

---

# 10. Payment Still Pending

AI

Your payment is still being processed.

We'll notify you automatically once the payment is confirmed.

---

# 11. Payment Success

Webhook Received

↓

Verify Signature

↓

Verify Amount

↓

Verify Order ID

↓

Update Payment

↓

Confirm Booking

↓

AI

🎉 Payment received successfully!

Your booking has been confirmed.

Booking ID:

BK-2026-00125

---

# 12. Payment Success After Hold Expired

Scenario

Booking Hold

↓

Expired

↓

Customer Pays Later

---

Backend

Reject Booking Confirmation

↓

Create Refund Request

↓

AI

Your payment was received after the booking session expired.

A refund has been initiated.

Expected timeline:

5–7 business days.

---

# 13. Duplicate Payment

Customer accidentally pays twice.

↓

Backend detects duplicate transaction.

↓

Second payment marked as duplicate.

↓

Refund initiated automatically.

---

AI

We received two payments for the same booking.

The extra payment will be refunded automatically.

---

# 14. Invalid Payment Amount

Example

Expected

₹800

Received

₹500

↓

Reject Payment

↓

AI

The payment amount does not match the booking amount.

Please contact support if money has been deducted.

---

# 15. Invalid Payment Signature

Webhook received

↓

Signature validation fails

↓

Reject webhook

↓

Log security event

↓

Booking remains unpaid.

---

# 16. Payment Webhook Delayed

Customer pays

↓

Webhook delayed

↓

AI

We've received your payment request.

Your booking will be confirmed shortly.

You don't need to pay again.

---

# 17. Duplicate Webhook Event

Gateway sends

Payment Success

↓

Again

↓

Again

Backend processes only the first event.

Duplicate events ignored.

---

# 18. Customer Closes Browser

Payment page closed

↓

Booking still on hold

↓

AI

You haven't completed your payment yet.

You can continue using the same payment link until it expires.

---

# 19. Bank Server Down

AI

Your bank appears to be temporarily unavailable.

Please try again later or use another payment method.

---

# 20. Payment Cancelled by Customer

Customer exits payment page.

↓

AI

Your payment was cancelled.

Your booking will remain reserved until the payment link expires.

---

# 21. Refund Initiated

AI

Your refund has been initiated successfully.

Refund Amount

₹800

Expected Timeline

5–7 business days

---

# 22. Refund Failed

Gateway Error

↓

Retry Refund

↓

Manual Review

↓

AI

We're experiencing a delay while processing your refund.

Our team has been notified.

---

# 23. Backend Tool Calls

| Tool | Purpose |
|------|----------|
| createPaymentLink() | Generate payment URL |
| verifyPaymentStatus() | Verify payment state |
| verifyWebhookSignature() | Validate webhook |
| confirmBooking() | Confirm booking |
| releaseBookingHold() | Release slot |
| createRefund() | Initiate refund |
| retryRefund() | Retry failed refund |
| notifyCustomer() | Send WhatsApp updates |
| notifyOwner() | Notify turf owner |
| auditPaymentEvent() | Store payment logs |

---

# 24. Business Rules

The AI must never:

- Confirm unpaid bookings
- Assume payment success
- Trust browser redirects
- Trust screenshots
- Trust customer claims

Only verified payment gateway webhooks can confirm a booking.

---

# 25. Error Handling

Gateway unavailable

↓

Retry

↓

If still unavailable

↓

Notify customer

---

Database unavailable

↓

Rollback transaction

↓

Keep booking hold active

---

Notification failure

↓

Retry

↓

Log failure

---

# 26. Audit Logging

Every payment event records:

- Timestamp
- Booking ID
- Customer ID
- Payment ID
- Order ID
- Amount
- Currency
- Status
- Correlation ID

---

# 27. Edge Cases

- Customer refreshes payment page
- Customer pays after expiry
- Duplicate payment
- Duplicate webhook
- Gateway timeout
- Partial payment
- Invalid signature
- Wrong payment amount
- Network interruption
- Refund delay

---

# 28. QA Test Scenarios

| Scenario | Expected Result |
|----------|-----------------|
| Payment succeeds | Booking confirmed |
| Payment fails | Retry option shown |
| Payment timeout | Verify status |
| Hold expires | Slot released |
| Payment after expiry | Refund initiated |
| Duplicate payment | Second payment refunded |
| Duplicate webhook | Ignore duplicate |
| Invalid signature | Reject webhook |
| Gateway unavailable | Retry and notify |
| Customer closes payment page | Hold remains active |

---

# 29. Success Criteria

A payment flow is successful when:

✓ Payment verified

✓ Booking confirmed

✓ Owner notified

✓ Customer notified

✓ Audit log created

✓ Reports updated

No booking is confirmed without successful payment verification.

---

# 30. Future Enhancements

- Multiple payment gateways
- Wallet payments
- EMI support
- Auto-retry failed payments
- Smart fraud detection
- Instant refunds
- Subscription billing
- Dynamic payment links

---

# End of Document