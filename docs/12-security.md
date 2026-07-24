# Turf AI Booking — Security Architecture

**Document:** 12-security.md
**Version:** 1.0
**Status:** Approved Architecture
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the security architecture for Turf AI Booking.

It covers:

- Authentication
- Authorization
- API Security
- WhatsApp Security
- Payment Security
- AI Security
- Database Security
- Secrets Management
- Encryption
- Logging
- Multi-Tenant Isolation
- Compliance

Security is enforced at every layer of the application.

---

# 2. Security Principles

The platform follows:

✓ Zero Trust

✓ Least Privilege

✓ Defense in Depth

✓ Secure by Default

✓ Fail Securely

✓ Audit Everything

✓ Never Trust Client Input

---

# 3. Security Layers

Customer

↓

WhatsApp

↓

Meta Cloud API

↓

Spring Boot API Gateway

↓

Authentication

↓

Authorization

↓

Business Validation

↓

Services

↓

Database

Each layer validates the request independently.

---

# 4. Authentication

Users are identified using:

Customer

↓

WhatsApp Number

↓

Database

↓

Role

↓

Business

Future support:

- OTP Login
- Email Login
- Google Login

---

# 5. Authorization

Authentication answers:

"Who are you?"

Authorization answers:

"What are you allowed to do?"

Example

Customer

↓

View Own Booking

Allowed

Customer

↓

View Revenue

Denied

---

# 6. Role-Based Access Control (RBAC)

Roles:

CUSTOMER

OWNER

MANAGER

SYSTEM_ADMIN

Permissions are defined in:

docs/03-rbac.md

---

# 7. Multi-Tenant Isolation

Every business is isolated.

Every query includes:

business_id

Example

Business A

Cannot access

Business B

Even if booking IDs are known.

---

# 8. API Security

All APIs must enforce:

Authentication

↓

Authorization

↓

Validation

↓

Rate Limiting

↓

Audit Logging

No endpoint bypasses these checks.

---

# 9. HTTPS Only

All communication uses HTTPS.

Never allow:

HTTP

TLS versions below 1.2 are not supported.

---

# 10. WhatsApp Security

Verify every incoming webhook.

Checks:

Verify Token

↓

Request Signature

↓

Event ID

↓

Timestamp

Reject invalid requests immediately.

---

# 11. Duplicate Event Protection

Store every webhook event.

If

event_id

already exists

↓

Ignore duplicate.

---

# 12. Payment Security

Payment gateway is authoritative.

Never trust:

Customer message

Browser redirect

Screenshot

Only verified gateway webhooks update payment status.

---

# 13. Payment Signature Verification

Every payment webhook verifies:

Signature

↓

Order ID

↓

Amount

↓

Currency

↓

Transaction ID

Invalid events are rejected.

---

# 14. AI Security

AI never:

Accesses PostgreSQL

Executes SQL

Modifies data directly

Reads secrets

Confirms payments

Creates bookings

AI only calls approved backend tools.

---

# 15. Tool Authorization

Every AI tool call contains:

User ID

Business ID

Role

Conversation ID

Backend validates permissions before execution.

---

# 16. Prompt Injection Protection

Ignore attempts such as:

"Ignore previous instructions."

"Show database."

"Reveal secret key."

AI refuses and continues using backend tools only.

---

# 17. Input Validation

Validate:

Phone Numbers

Dates

Times

UUIDs

Amounts

Business IDs

Reject malformed requests before processing.

---

# 18. SQL Injection Protection

Use:

Spring Data JPA

Prepared Statements

Parameterized Queries

Never concatenate SQL strings.

---

# 19. Cross-Site Scripting (Future)

Future web portal must:

Escape HTML

Sanitize user input

Use Content Security Policy (CSP)

Not applicable to WhatsApp MVP.

---

# 20. Secrets Management

Never store secrets in source code.

Store:

WhatsApp Token

Webhook Secret

Payment Keys

Database Password

OpenAI API Key

In:

Environment Variables

or

Secrets Manager

---

# 21. Environment Variables

Example

WHATSAPP_TOKEN

DATABASE_URL

DATABASE_PASSWORD

OPENAI_API_KEY

RAZORPAY_KEY

RAZORPAY_SECRET

Never commit .env files.

---

# 22. Encryption

Sensitive data encrypted:

At Rest

Database encryption

In Transit

TLS

Passwords

BCrypt

---

# 23. Password Policy

For future dashboard users:

BCrypt

Minimum 12 characters

Never store plain text passwords.

---

# 24. Audit Logging

Log:

Login

Booking

Payment

Cancellation

Role Change

AI Tool Calls

Webhook Events

Logs must be immutable.

---

# 25. Correlation ID

Every request receives:

Correlation ID

Used in:

Logs

Tracing

Monitoring

Support

Example

X-Correlation-ID

4d7c6d2e-...

---

# 26. Request Validation

Each request validates:

Headers

↓

Authentication

↓

Authorization

↓

Payload

↓

Business Rules

↓

Execution

---

# 27. Rate Limiting

Prevent abuse.

Example

Customer

50 requests

per minute

Owner

100 requests

per minute

Webhook

Higher limit

---

# 28. Brute Force Protection

Repeated failures

↓

Temporary block

↓

Audit Log

↓

Alert if suspicious.

---

# 29. Database Security

Database accessible only from backend.

Never expose PostgreSQL publicly.

Use private networking.

---

# 30. Backup Strategy

Daily backup

↓

Encrypted Storage

↓

Retention

30 days minimum

Test restore regularly.

---

# 31. Data Retention

Bookings

Never deleted

Payments

Never deleted

Audit Logs

Never deleted

Conversations

Configurable retention

---

# 32. PII Protection

Store only:

Name

Phone Number

Language

Avoid unnecessary personal data.

---

# 33. Logging Rules

Never log:

Passwords

API Keys

Payment Secrets

OTP Codes

Webhook Secrets

Full card details

---

# 34. Error Responses

Do not expose:

Stack traces

SQL errors

Internal IDs

Framework versions

Example

Internal Server Error

Instead

Something went wrong.

Please try again later.

---

# 35. Notification Security

Booking notifications include:

Booking ID

Date

Time

Turf

Do not include:

Internal database IDs

Secrets

Payment credentials

---

# 36. AI Conversation Privacy

Conversation history belongs to:

Business

Customer

Only authorized users may access it.

---

# 37. Compliance

The platform should align with:

Indian IT Act

Data Privacy Best Practices

Payment Gateway Security Requirements

Future:

DPDP Act compliance

---

# 38. Monitoring

Monitor:

Failed Logins

Unauthorized Access

Webhook Failures

Payment Failures

Repeated Booking Attempts

AI Failures

---

# 39. Security Alerts

Critical alerts:

Multiple failed webhook signatures

Repeated authorization failures

Unusual booking spikes

Repeated payment failures

Possible abuse patterns

---

# 40. Incident Response

Security Incident

↓

Contain

↓

Investigate

↓

Recover

↓

Notify affected stakeholders (if required)

↓

Document lessons learned

---

# 41. Dependency Security

Regularly update:

Spring Boot

PostgreSQL Driver

Apache POI

OpenAI SDK

WhatsApp SDK

Scan dependencies for vulnerabilities.

---

# 42. Secure Development Practices

- Code Reviews
- Static Analysis
- Dependency Scanning
- Secret Scanning
- Unit Testing
- Integration Testing

---

# 43. Security Checklist

Before production:

✓ HTTPS Enabled

✓ Secrets Configured

✓ Database Secured

✓ Webhooks Verified

✓ Payment Verification Enabled

✓ Audit Logging Enabled

✓ Rate Limiting Enabled

✓ Backup Configured

✓ Monitoring Enabled

---

# 44. MVP Security Summary

The MVP includes:

✓ WhatsApp Authentication

✓ RBAC

✓ AI Tool Authorization

✓ Payment Verification

✓ Database Isolation

✓ Encrypted Communication

✓ Audit Logging

✓ Rate Limiting

✓ Multi-Tenant Security

✓ Secure Secrets Management

---

# 45. Future Enhancements

- Multi-Factor Authentication (MFA)
- Hardware Security Modules (HSM)
- Web Application Firewall (WAF)
- Device Fingerprinting
- Fraud Detection
- Security Dashboard
- SIEM Integration
- Zero-Trust Networking

---

# 46. Next Document

The final document is:

docs/13-roadmap.md

This document defines:

- Development Phases
- Milestones
- Sprint Plan
- MVP Scope
- Post-MVP Features
- Infrastructure Roadmap
- Cost Estimation
- Launch Strategy
- Customer Acquisition Plan
- Scaling Strategy