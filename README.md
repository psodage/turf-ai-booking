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

Customer
    ↓
WhatsApp
    ↓
WhatsApp Business Platform
    ↓
AI Agent
    ↓
Spring Boot Backend
    ↓
PostgreSQL
    ↓
Booking Engine
    ↓
Payment Gateway

## 📂 Project Structure

- `backend/` - Spring Boot backend
- `frontend/` - React frontend for internal/admin tools
- `n8n/` - Automation workflows
- `docs/` - Product and technical documentation

## 🚧 Project Status

Currently in planning and development.

## 🛠️ Planned Technology Stack

### Backend

- Java
- Spring Boot
- Spring Security
- PostgreSQL

### AI

- LLM API
- Tool calling / function calling

### Communication

- WhatsApp Business Platform

### Payments

- Payment Gateway

### Automation

- n8n

### Reporting

- Excel

## 🔐 Security

Secrets and credentials must be stored in environment variables.

Never commit `.env` files or API keys to GitHub.