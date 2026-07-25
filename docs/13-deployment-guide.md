# Turf AI Booking — Deployment & Operations Guide

**Document:** 13-deployment-guide.md  
**Version:** 1.0  
**Status:** Approved  
**Last Updated:** 2026-07-25  

---

## 1. System Requirements

- **Server Architecture:** 64-bit Linux (Ubuntu 22.04 LTS recommended)
- **CPU:** 2 vCPU cores minimum (4 vCPU recommended)
- **RAM:** 4 GB minimum (8 GB recommended)
- **Disk:** 50 GB SSD storage minimum
- **Software Dependencies:** Docker v24+, Docker Compose v2.20+, Git

---

## 2. Environment Setup

Copy `.env.example` to `.env` on the production server and populate all secrets:

```bash
# Database Secrets
POSTGRES_DB=turfai_db
POSTGRES_USER=turfai_app
POSTGRES_PASSWORD=SECURE_DATABASE_PASSWORD_HERE

# WhatsApp Cloud API Production Secrets
WHATSAPP_PHONE_NUMBER_ID=109876543210
WHATSAPP_ACCESS_TOKEN=EAAG...PROD_ACCESS_TOKEN
WHATSAPP_VERIFY_TOKEN=turfai_prod_verify_token_99
WHATSAPP_APP_SECRET=a1b2c3d4e5f6...PROD_APP_SECRET

# Razorpay Production Live Keys
RAZORPAY_MODE=live
RAZORPAY_KEY_ID=rzp_live_PROD_KEY_ID
RAZORPAY_KEY_SECRET=PROD_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET=PROD_WEBHOOK_SECRET

# OpenAI Production Credentials
AI_PROVIDER=openai
AI_API_KEY=sk-proj-PROD_OPENAI_KEY
```

---

## 3. Production Deployment Commands

1. **Clone Codebase & Submodules:**
   ```bash
   git clone https://github.com/psodage/turf-ai-booking.git
   cd turf-ai-booking
   ```

2. **Launch Docker Services:**
   ```bash
   docker compose up -d --build
   ```

3. **Verify Container Health:**
   ```bash
   docker compose ps
   ```

4. **Verify Application Health Endpoint:**
   ```bash
   curl -i http://localhost:8080/actuator/health
   ```

---

## 4. HTTPS & SSL Configuration

1. Place SSL certificate and private key in `./docker/nginx/ssl/`:
   - `fullchain.pem`
   - `privkey.pem`
2. Ensure DNS records point `api.turfai.in` and `webhook.turfai.in` to server IPv4.
3. Verify TLS 1.2 / TLS 1.3 handshake:
   ```bash
   curl -I https://webhook.turfai.in/webhook/whatsapp
   ```

---

## 5. Backup & Restore Runbook

### Automated Nightly Backup (Cron)
Add cron entry (`crontab -e`):
```cron
0 2 * * * /bin/bash /opt/turf-ai-booking/scripts/backup-postgres.sh >> /var/log/turfai-backup.log 2>&1
```

### Manual Backup
```bash
./scripts/backup-postgres.sh
```

### Database Restoration
```bash
./scripts/restore-postgres.sh /var/backups/turfai_db/turfai_db_20260725_020000.sql.gz
```

---

## 6. Rollback Procedure

In case of critical production issues:
1. Revert Git tag / commit:
   ```bash
   git checkout tags/v0.1.0-stable
   ```
2. Rebuild and restart containers:
   ```bash
   docker compose up -d --build --force-recreate
   ```
3. Restore database snapshot if schema migration failed:
   ```bash
   ./scripts/restore-postgres.sh /var/backups/turfai_db/last_known_good.sql.gz
   ```
