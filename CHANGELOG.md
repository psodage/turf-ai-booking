# Changelog — Turf AI Booking

All notable changes to the Turf AI Booking project are documented in this file.

---

## [0.1.0] - 2026-07-25

### Added
- **Phase 1 (Setup & Core Standards)**: Spring Boot 3.5 application structure, BaseEntity, Exception Hierarchy, GlobalExceptionHandler, CorrelationIdFilter, logback-spring JSON logging.
- **Phase 2 (Database & Domain Model)**: 17 Flyway SQL migrations (V1–V17), seed migration for demo business Green Pitch, 16 JPA entities, 16 Spring Data JPA repositories with pessimistic write locking queries (`SELECT FOR UPDATE`).
- **Phase 3 (Booking Engine)**: TurfService, OperatingHoursService, PricingService, SlotService (60-min slots, 30-day advance window), BookingService (10-min holds, BK-YYYY-NNNNN sequential IDs, 2-hour cancellation rules), BlockedSlotService, 2-minute auto-expiry hold cleanup scheduler, SlotController, BookingController, BlockedSlotController, and OpenAPI/Swagger documentation.
- **Phase 4 (WhatsApp Integration)**: Meta Cloud API integration, GET webhook verification handshake, POST event receiver, HMAC-SHA256 signature verification (`X-Hub-Signature-256`), 5-minute replay protection, phone_number_id tenant routing, customer auto-registration, wamid deduplication, WhatsAppService (text, interactive buttons, list options, templates, document attachments).
- **Phase 5 (AI Agent & Orchestration)**: AiProvider interface, MockAiProvider, OpenAiProvider REST client, PromptManager, ConversationContextBuilder (10-message sliding window, 30-min session timeout, 2000 token budget), AiToolGateway with 11 structured tools returning standardized ToolResult JSON, AiOrchestratorService.
- **Phase 6 (Payment Gateway Integration)**: Razorpay Payment Links API integration (RazorpayProperties, RazorpayClientWrapper, MockRazorpayClientWrapper), PaymentService, POST /webhook/razorpay callback receiver with HMAC-SHA256 signature verification, duplicate event suppression via PaymentAudit, late payment 60-second grace period booking confirmation hook (ADR-016), automated refund engine, PaymentController.
- **Phase 7 (Excel Reporting & Business Reports)**: Apache POI integration (`poi-ooxml 5.3.0`), 6-sheet Excel report generator (Summary, Bookings, Payments, Customers, Revenue, Slot Utilization), Excel formula injection protection (`'=SUM`), ReportStorageService, ReportSchedulerService (@Scheduled cron jobs for nightly, weekly, monthly reports), ReportController, AI `generateExcelReport` tool with WhatsApp document delivery.
- **Phase 9 (Deployment, Infrastructure & Pilot Launch)**: Production multi-stage Dockerfile (Eclipse Temurin JRE 17, non-root user, healthcheck), docker-compose.yml, docker-compose.dev.yml, Nginx SSL reverse proxy configuration (`docker/nginx/nginx.conf`), Spring Boot `prod` and `dev` environment profiles, Actuator health indicators, automated daily PostgreSQL backup script (`scripts/backup-postgres.sh`), restore script (`scripts/restore-postgres.sh`), Deployment Guide (`docs/13-deployment-guide.md`), Pilot Onboarding Plan (`docs/14-pilot-onboarding.md`).
