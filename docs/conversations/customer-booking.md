# Turf AI Booking — Customer Booking Conversation

**Document:** conversations/customer-booking.md  
**Version:** 2.0  
**Status:** Approved  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the complete customer booking journey through the WhatsApp AI Agent.

It specifies:

- Customer conversation flow
- AI responses
- Backend tool calls
- Business validations
- Error scenarios
- Payment flow
- Booking confirmation
- Recovery paths

This document serves as the source of truth for AI conversation design.

---

# 2. Objectives

The booking experience should be:

- Fast
- Natural
- Conversational
- Error-resistant
- Payment-first
- Fully automated

Target booking completion time:

< 2 Minutes

---

# 3. Happy Path Overview

Customer

↓

Greeting

↓

Book Turf

↓

Select Date

↓

Select Time

↓

Check Availability

↓

Confirm Price

↓

Payment

↓

Payment Success

↓

Booking Confirmation

↓

Reminder

↓

Visit Turf

↓

Booking Completed

---

# 4. Conversation Principles

The AI should:

✓ Be polite

✓ Be concise

✓ Ask one question at a time

✓ Never overwhelm the customer

✓ Never assume missing information

✓ Confirm before payment

✓ Never confirm booking before payment

---

# 5. Conversation Start

### Customer

Hi

---

### AI

👋 Welcome to *{{Business Name}}*

I can help you:

⚽ Book a Turf

📅 View Your Bookings

❌ Cancel a Booking

Please type what you'd like to do.

---

# 6. Intent Detection

Possible intents:

- Book Turf
- Check Availability
- View Booking
- Cancel Booking
- Pricing
- Location
- Help

If intent is unclear:

AI asks for clarification.

---

# 7. Booking Flow

Customer

Book Turf

↓

AI

Sure!

Which date would you like to play?

---

# 8. Date Selection

Customer examples:

Tomorrow

25 July

Next Sunday

This Friday

Backend converts the natural language into a valid date.

Validation:

- Date must not be in the past.
- Date must be within advance booking window.

Tool Call:

```
checkBookingDate()
```

---

# 9. Invalid Date

Customer

Yesterday

AI

Sorry, bookings can only be made for today or future dates.

Please choose another date.

---

# 10. Time Selection

AI

What time would you like to play?

Example:

6 PM

7 PM

8 PM

---

Customer

7 PM

Validation:

Opening Hours

↓

Slot Exists

↓

Within Business Hours

Tool Call

```
validateSlot()
```

---

# 11. Turf Selection

If multiple turfs exist:

AI

Please choose a turf.

1️⃣ Turf 1 (5v5)

2️⃣ Turf 2 (7v7)

Tool Call

```
getAvailableTurfs()
```

---

# 12. Availability Check

Tool

```
checkAvailability()

Input

date

time

turf_id
```

Possible responses:

Available

Unavailable

Blocked

Already Booked

---

# 13. Slot Available

AI

Great!

The slot is available.

Price:

₹800

Would you like to continue?

Buttons:

✅ Continue

❌ Cancel

---

# 14. Slot Not Available

AI

Sorry, this slot is already booked.

Here are some alternatives:

6 PM

8 PM

9 PM

Tool Call

```
suggestAlternativeSlots()
```

---

# 15. Booking Summary

AI

Booking Summary

Turf:

Turf 1

Date:

25 July

Time:

7 PM – 8 PM

Amount:

₹800

Proceed to payment?

Buttons

Pay Now

Cancel

---

# 16. Booking Hold

When customer clicks

Pay Now

Backend

↓

Create Booking Hold

↓

10-minute expiry

Tool Call

```
createBookingHold()
```

No other customer may book this slot while hold is active.

---

# 17. Payment Link

Backend

↓

Generate Payment Link

↓

AI

Please complete payment using the secure link below.

Payment Link

↓

10-minute expiry

---

# 18. Payment Success

Payment Webhook

↓

Verify Signature

↓

Update Payment

↓

Confirm Booking

↓

AI

🎉 Booking Confirmed!

Booking ID:

BK-2026-00123

Date:

25 July

Time:

7 PM

See you on the field! ⚽

---

# 19. Payment Failed

AI

Your payment could not be completed.

Would you like to try again?

Buttons

Retry Payment

Cancel Booking

---

# 20. Payment Timeout

Booking Hold

↓

Expired

↓

AI

The booking session has expired.

Please start again.

---

# 21. Booking Confirmation

The booking is confirmed only when:

✓ Payment Verified

✓ Booking Updated

✓ Audit Logged

✓ Notification Sent

Never before.

---

# 22. Reminder Flow

2 Hours Before Match

↓

AI

⏰ Reminder

Your booking starts at

7 PM today.

See you soon!

---

# 23. Customer Arrives

No AI interaction required.

Future:

QR Check-In

---

# 24. Booking Completion

After end time:

Backend

↓

Status

↓

COMPLETED

---

# 25. Customer Requests Pricing

Customer

How much?

AI

Weekday

₹800

Weekend

₹1000

Peak Hour

₹1200

Tool

```
getPricing()
```

---

# 26. Customer Requests Location

Customer

Location

AI

📍 Here is our location:

Google Maps Link

Tool

```
getLocation()
```

---

# 27. Customer Requests Help

AI

I can help you:

⚽ Book Turf

📅 View Bookings

❌ Cancel Booking

📍 Get Location

💰 Check Pricing

---

# 28. Error Handling

Examples

Backend unavailable

↓

AI

Sorry, I couldn't complete your request.

Please try again in a few moments.

---

# 29. Business Rules

The AI must never:

- Confirm unpaid bookings
- Guess availability
- Guess pricing
- Guess booking IDs

All information must come from backend tools.

---

# 30. Tool Calls Summary

| Tool | Purpose |
|------|---------|
| checkAvailability | Check slot availability |
| getAvailableTurfs | List available turfs |
| createBookingHold | Reserve slot temporarily |
| createPaymentLink | Generate payment link |
| confirmBooking | Confirm booking after payment |
| getPricing | Fetch pricing |
| getLocation | Fetch Google Maps location |

---

# 31. Edge Cases

- Customer sends invalid date
- Customer sends invalid time
- Customer selects unavailable slot
- Payment fails
- Payment times out
- Duplicate payment webhook
- Duplicate WhatsApp message
- Customer becomes inactive
- Turf closes unexpectedly
- Slot blocked by owner

Each scenario must be handled gracefully.

---

# 32. Success Criteria

A booking journey is successful when:

✓ Slot validated

✓ Booking hold created

✓ Payment verified

✓ Booking confirmed

✓ Customer notified

✓ Owner notified

✓ Audit log created

---

# 33. Future Enhancements

- Voice booking
- Multi-language conversations
- Smart slot recommendations
- Group bookings
- Recurring bookings
- Promo code support
- Calendar integration

---

# End of Document