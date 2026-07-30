# AGENTS.md — Coding Standards & Guidelines

## Tech Stack
- **Language:** Java 17+ (Java 21 compatible)
- **Framework:** Spring Boot 3.5
- **Database:** PostgreSQL 16
- **ORM:** Spring Data JPA
- **Build Tool:** Maven

## Package Structure
```text
com.turfai.booking
  ├── config/          # Spring Security, Web, & System configuration
  ├── controller/      # REST API Controllers & Webhook Handlers
  ├── dto/             # Data Transfer Objects (Request/Response records)
  ├── entity/          # JPA Entities extending BaseEntity
  ├── exception/       # Custom Exception Hierarchy & GlobalExceptionHandler
  ├── repository/      # Spring Data JPA Repositories
  ├── scheduler/       # Scheduled tasks (@Scheduled)
  ├── service/         # Domain Business Logic Services
  ├── ai/              # AI Agents, Tools, Prompts, & Orchestration
  └── util/            # Helpers & Constants
```

## Architectural Guidelines
- **Constructor Injection Only:** Never use `@Autowired` field injection.
- **Layering Isolation:** Controller → DTO → Service → Repository → Entity.
- **No SQL in Controllers:** All persistence & query logic must reside in Repositories/Services.
- **Input Validation:** Use `@Valid` and Bean Validation annotations on all incoming DTOs.
- **Entities Encapsulated:** Never expose domain entities in Controller APIs; map to DTOs.
- **No TODO Comments:** All committed code must be production-ready and fully implemented.

## Naming Conventions
- **Services:** `BookingService`, `PaymentService` (direct class implementations, no unnecessary `ServiceImpl` suffix).
- **Repositories:** `BookingRepository` (extending `JpaRepository`).
- **DTOs:** `CreateBookingRequest`, `BookingResponse` (Java `record` preferred for immutable DTOs).
- **Entities:** `Booking`, `User`, `Turf` (singular nouns).
- **Exceptions:** `BookingNotFoundException`, `SlotUnavailableException` (extending `BaseException`).

## Git & Development Workflow
- **Branch Naming:** `feature/M01-project-boots`, `fix/issue-description`.
- **Commit Style:** Use format `ai: <short summary>` (e.g. `ai: fix login validation`).

### Git Commit Policy
After completing ANY code modification:
1. Verify the changes are complete.
2. Stage all modified, new, and deleted files (`git add -A`).
3. Create a Git commit immediately.
4. Use a clear, concise commit message following the format: `ai: <short summary>`.

**Rules:**
- Never leave the working tree with uncommitted changes after completing a task.
- Only skip committing if explicitly instructed ("don't commit", "no commit", "leave changes uncommitted").
- If a commit fails (merge conflict, hooks, permissions, etc.), stop and explain the reason instead of silently continuing.
- At the end of every task, include the commit hash and commit message in your final response.