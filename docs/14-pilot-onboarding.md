# Turf AI Booking — Pilot Onboarding & Go-Live Plan

**Document:** 14-pilot-onboarding.md  
**Version:** 1.0  
**Status:** Approved  
**Last Updated:** 2026-07-25  

---

## 1. Pilot Customer Details

- **Business Name:** Green Pitch Kolhapur
- **Owner Name:** Vikram Patil
- **Phone:** +91 98765 43210
- **Turf Name:** Pitch A (7v7 Synthetic Grass)
- **Operating Hours:** 06:00 AM - 11:00 PM (Mon-Sun)
- **Pricing:**
  - Base Rate: ₹800 / hour
  - Weekend Peak Rate: ₹1200 / hour

---

## 2. Onboarding Checklist

- [x] Business registered in database (`R__seed_demo_data.sql` / Admin API).
- [x] Turf, operating hours, and peak pricing rules populated.
- [x] Meta WhatsApp Business App connected to dedicated phone number.
- [x] Webhook callback URL configured (`https://webhook.turfai.in/webhook/whatsapp`).
- [x] Razorpay account connected and live API keys configured.
- [x] Razorpay Webhook URL registered (`https://webhook.turfai.in/webhook/razorpay`).

---

## 3. End-to-End Validation Protocol

1. **Test Availability Check:**
   - Send WhatsApp text: *"Is slot available tomorrow at 6 PM?"*
   - Expect AI response with available slots and pricing.
2. **Test Hold & Payment Link Creation:**
   - Send WhatsApp text: *"Book tomorrow 6 PM to 7 PM"*
   - Expect AI to place a 10-minute hold and send Razorpay Payment Link.
3. **Test Payment & Confirmation:**
   - Complete ₹1 payment using Razorpay sandbox/live link.
   - Verify Razorpay webhook fires to `POST /webhook/razorpay`.
   - Verify booking transitions to `CONFIRMED`.
   - Verify customer receives WhatsApp confirmation text and owner receives instant alert.
4. **Test Excel Report Generation:**
   - Owner sends: *"Send me today's report"*.
   - Verify owner receives 6-sheet `.xlsx` document directly in WhatsApp chat.

---

## 4. 30-Day Pilot Support Plan

- **Daily Health Checks:** Automated monitoring of `/actuator/health` every 15 minutes.
- **Daily Backup Audits:** Verify daily `turfai_db` dump created at 02:00 AM.
- **Incident Response Time:** < 15 minutes for payment/booking failures.
- **Weekly Review:** Analyze AI conversation history to improve system prompts.
