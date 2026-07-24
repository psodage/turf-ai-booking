# Turf AI Booking — Error Handling & Recovery

**Document:** 11-error-handling.md  
**Version:** 1.0  
**Status:** Approved Architecture  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the global error handling strategy for Turf AI Booking.

The objectives are:

- Prevent data corruption
- Prevent duplicate bookings
- Ensure graceful degradation
- Improve observability
- Enable automatic recovery
- Provide user-friendly error messages
- Maintain system reliability

Every system component must follow these rules.

---

# 2. Error Handling Principles

The platform follows these principles:

✓ Never lose data

✓ Never confirm invalid bookings

✓ Never expose internal errors

✓ Log every unexpected error

✓ Retry only safe operations

✓ Fail safely

✓ Keep users informed

---

# 3. Error Categories

The system classifies errors into:

Validation Errors

Business Rule Errors

Authentication Errors

Authorization Errors

Payment Errors

AI Errors

WhatsApp Errors

Database Errors

Network Errors

Infrastructure Errors

Unknown Errors

---

# 4. Global Error Flow

Incoming Request

↓

Validation

↓

Business Rules

↓

Database

↓

External Services

↓

Success

or

↓

Error Handler

↓

Log Error

↓

Generate Friendly Response

↓

Notify User (if required)

---

# 5. Validation Errors

Occurs when user input is invalid.

Examples

- Invalid phone number
- Invalid date
- Invalid time
- Missing booking details
- Invalid payment amount

Example Response

Sorry, I couldn't understand your booking request.

Please provide a valid date and time.

---

# 6. Business Rule Errors

Occurs when business rules prevent the operation.

Examples

- Slot already booked
- Turf closed
- Booking window expired
- Booking in the past
- Cancellation not allowed

Example

Customer

Book yesterday

↓

System

Bookings cannot be created for past dates.

---

# 7. Authentication Errors

Occurs when identity cannot be verified.

Examples

- Unknown owner
- Invalid webhook signature
- Invalid token

Action

Reject request immediately.

Log security event.

---

# 8. Authorization Errors

Occurs when user lacks permission.

Examples

Customer

Today's revenue

↓

Denied

Owner

Customer list

↓

Allowed

Manager

Delete business

↓

Denied

---

# 9. Booking Conflict Errors

Scenario

Customer A

↓

Book 7 PM

↓

Customer B

↓

Book 7 PM

↓

Only one booking succeeds.

Second request returns

Slot is no longer available.

Please choose another time.

---

# 10. Payment Errors

Examples

Payment Failed

Payment Cancelled

Gateway Timeout

Duplicate Payment

Late Payment

Insufficient Funds

Incorrect Amount

Every payment failure must be logged.

---

# 11. Late Payment Handling

Scenario

Hold expires

↓

Customer pays

↓

Booking cannot be confirmed

↓

Refund initiated

↓

Owner notified (optional)

---

# 12. Payment Webhook Errors

If webhook validation fails:

Reject event

↓

Log payload

↓

Do not update booking

---

# 13. Duplicate Webhooks

Gateway sends:

Payment Success

↓

Again

↓

Again

Process only once.

Duplicate events ignored.

---

# 14. WhatsApp Errors

Possible failures:

Message delivery failed

Webhook unavailable

Duplicate events

Rate limits

Template rejected

Media upload failed

---

# 15. Duplicate WhatsApp Messages

WhatsApp may resend the same event.

System checks

Message ID

↓

Already processed?

↓

Ignore

---

# 16. AI Errors

Possible failures

Model unavailable

Timeout

Tool failure

Malformed response

Hallucination detection

---

# 17. AI Timeout

If AI exceeds timeout:

Fallback message

Sorry, I'm taking longer than expected.

Please try again in a few moments.

---

# 18. Tool Execution Errors

Example

AI

↓

checkAvailability()

↓

Backend unavailable

↓

AI

↓

Unable to check availability right now.

Please try again.

---

# 19. Database Errors

Examples

Connection failure

Deadlock

Timeout

Constraint violation

Transaction rollback

---

# 20. Constraint Violations

Examples

Duplicate booking

Duplicate payment

Duplicate webhook

Duplicate phone number

Return

409 Conflict

---

# 21. Transaction Rollback

If any booking step fails:

Rollback entire transaction.

Never leave partial bookings.

---

# 22. Network Errors

Possible failures

Internet interruption

Gateway timeout

Cloud API unavailable

Retry if safe.

---

# 23. Retry Policy

Retry only:

Notification delivery

Webhook acknowledgement

AI requests

Excel generation

Never retry:

Payment confirmation

Booking creation

Refund processing

Without idempotency.

---

# 24. Retry Strategy

Retry schedule

1st Retry

5 Seconds

2nd Retry

30 Seconds

3rd Retry

2 Minutes

Then

Manual review

---

# 25. Circuit Breaker

External service repeatedly failing

↓

Circuit opens

↓

Requests skipped temporarily

↓

Retry later

Applies to:

AI APIs

Payment Gateway

WhatsApp API

---

# 26. Dead Letter Queue (Future)

Failed events may be moved to:

Dead Letter Queue

↓

Manual investigation

Future enhancement.

---

# 27. Logging Strategy

Every error logs

Timestamp

User ID

Business ID

Conversation ID

Request ID

Error Code

Stack Trace

Severity

---

# 28. Error Severity

INFO

Minor events

WARNING

Recoverable issues

ERROR

Operation failed

CRITICAL

System unavailable

---

# 29. Error Codes

Examples

ERR-001

Validation Failed

ERR-002

Booking Conflict

ERR-003

Payment Failed

ERR-004

Unauthorized

ERR-005

AI Timeout

ERR-006

Database Error

ERR-007

Webhook Invalid

ERR-008

Duplicate Event

---

# 30. User-Friendly Messages

Never expose

SQL Exception

NullPointerException

Stack Trace

Internal IDs

Instead

We're experiencing a temporary issue.

Please try again shortly.

---

# 31. Monitoring

Monitor

Booking failures

Payment failures

Webhook failures

AI failures

Database failures

Notification failures

---

# 32. Health Checks

Every service exposes:

/health

Status

UP

DOWN

DEGRADED

---

# 33. Alerting

Critical alerts

Database unavailable

Payment gateway failure

WhatsApp webhook failure

High booking conflict rate

Repeated AI failures

---

# 34. Recovery

After recovery

Resume pending jobs

Retry notifications

Regenerate reports

Do not automatically recreate bookings.

---

# 35. Incident Logging

Every production incident records

Time

Cause

Impact

Resolution

Preventive Action

---

# 36. Excel Generation Errors

If Excel generation fails

Retry

↓

Still fails

↓

Notify owner

↓

Log error

---

# 37. Notification Failures

Booking confirmed

↓

WhatsApp failed

↓

Retry

↓

Retry

↓

Retry

↓

Mark failed

Booking remains confirmed.

---

# 38. AI Fallback

If AI unavailable

Allow deterministic operations

Examples

Booking status

Availability

Payment status

Handled directly by backend where possible.

---

# 39. Security Errors

Invalid Signature

↓

Reject

↓

Audit Log

↓

Alert if repeated

---

# 40. Performance Targets

Booking API

<500 ms

Availability

<300 ms

Payment Verification

<2 seconds

AI Response

<5 seconds

Excel Generation

<5 seconds

---

# 41. Disaster Recovery

Critical data

Bookings

Payments

Audit Logs

Backed up daily.

Recovery procedures tested regularly.

---

# 42. Testing Strategy

Test

Validation failures

Payment failures

Duplicate webhooks

Duplicate bookings

AI failures

Database outages

Network interruptions

---

# 43. MVP Error Handling Summary

The MVP supports:

✓ Validation handling

✓ Booking conflict detection

✓ Transaction rollback

✓ Payment verification

✓ Duplicate event protection

✓ Friendly error messages

✓ Retry logic

✓ Centralized logging

✓ Monitoring

✓ Recovery procedures

---

# 44. Next Document

The next document is:

docs/12-security.md

This document defines:

- Authentication
- Authorization
- Role-Based Access Control
- API security
- WhatsApp security
- Payment security
- Data encryption
- Audit logging
- Secrets management
- Rate limiting
- Multi-tenant isolation
- Compliance considerations