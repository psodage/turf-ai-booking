# Turf AI Booking

AI-powered WhatsApp booking system for football turf businesses.

## 🎯 Project Goal

Turf AI Booking helps football turf owners manage their bookings through WhatsApp.

Customers can:
- Discover turf information
- Check availability
- View pricing
- Book a turf
- Make online payments
- Receive booking confirmations
- Cancel bookings according to the turf's policy

Turf owners can use an AI-powered WhatsApp assistant to:
- View today's bookings
- View upcoming bookings
- Check revenue
- Block/unblock slots
- Manage pricing
- Generate booking reports

## 📍 Initial Market

Kolhapur, Maharashtra, India.

## 🏗️ Architecture

```text
Customer
    ↓
WhatsApp
    ↓
WhatsApp Business Platform
    ↓
Spring Boot Backend
    ↓
AI Agent
    ↓
Booking Engine
    ↓
Payment Gateway (Razorpay)
    ↓
PostgreSQL
```

## 🚀 Local Development

### Prerequisites
- **Java 21** SDK
- **Maven 3.9+**
- **Docker & Docker Compose**

### Quick Start
1. **Clone the repository:**
   ```bash
   git clone <repo-url>
   cd turf-ai-booking
   ```
2. **Setup Environment Variables:**
   ```bash
   cp .env.example .env
   ```
3. **Start Database:**
   ```bash
   docker-compose up -d
   ```
4. **Run Spring Boot Application:**
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
5. **Verify Health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## 📂 Project Structure

- `backend/` - Spring Boot 3.5 application
- `docs/` - Product and technical documentation
- `frontend/` - React frontend for internal/admin tools (future)

## 🚧 Project Status

Implementation in progress. Phase 1 Foundation & Architecture active.
See `PROJECT_STATUS.md` for current sprint status.

## 🛠️ Technology Stack

### Backend
- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- PostgreSQL 16
- Flyway (migrations)

### AI
- OpenAI / Gemini
- Function / Tool calling

### Communication
- WhatsApp Business Platform (Cloud API)

### Payments
- Razorpay Payment Links

### Reporting
- Apache POI (Excel)

### Scheduling
- Spring `@Scheduled`

### Deployment
- Docker & Docker Compose

## 📋 Key Documentation

| Document | Purpose |
|----------|---------|
| ARCHITECTURE.md | System architecture |
| DECISIONS.md | Architecture Decision Records (19 ADRs) |
| TODO.md | Development tasks |
| PROJECT_STATUS.md | Current sprint |
| docs/ | Business and technical specifications |

## 🔐 Security

Secrets and credentials must be stored in environment variables.
Never commit `.env` files or API keys to GitHub.