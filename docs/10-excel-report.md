# Turf AI Booking — Excel Reporting

**Document:** 10-excel-report.md  
**Version:** 1.0  
**Status:** Approved Architecture  
**Last Updated:** 2026-07-24

---

# 1. Purpose

This document defines the Excel reporting system for Turf AI Booking.

Since the MVP does not include a web dashboard, Excel serves as the primary reporting interface for turf owners.

Reports should provide:

- Daily bookings
- Revenue
- Customer information
- Payment details
- Slot utilization
- Business insights

Reports are automatically generated and delivered through WhatsApp.

---

# 2. Objectives

The reporting system should:

- Generate reports automatically
- Allow manual report generation
- Export data in Excel format
- Support business analytics
- Help owners manage operations
- Minimize manual work

---

# 3. Report Types

The MVP supports:

- Daily Booking Report
- Weekly Report
- Monthly Report
- Revenue Report
- Customer Report
- Payment Report
- Slot Utilization Report

Future reports:

- Cancellation Report
- No Show Report
- Peak Hour Analysis
- Business Performance Report

---

# 4. Report Delivery

Reports may be generated:

Automatically

↓

WhatsApp Document

or

Owner Request

↓

Generate Excel

↓

WhatsApp Document

---

# 5. Workbook Structure

Each report is a single Excel workbook.

Workbook

├── Summary

├── Bookings

├── Payments

├── Customers

├── Revenue

├── Slot Utilization

---

# 6. Summary Sheet

Contains business overview.

Columns

Business Name

Report Date

Total Bookings

Confirmed Bookings

Cancelled Bookings

Revenue

Refund Amount

Average Booking Value

Peak Booking Hour

Most Popular Turf

Generated At

---

# 7. Bookings Sheet

Every booking appears as one row.

Columns

Booking ID

Booking Number

Customer Name

Phone

Turf

Date

Start Time

End Time

Amount

Status

Booking Source

Payment Status

Created At

---

# 8. Payments Sheet

Columns

Payment ID

Booking ID

Gateway

Transaction ID

Amount

Currency

Status

Payment Time

Refund Status

Gateway Reference

---

# 9. Customers Sheet

Columns

Customer Name

WhatsApp Number

First Booking

Last Booking

Total Bookings

Total Spend

Cancelled Bookings

No Shows

Preferred Turf

---

# 10. Revenue Sheet

Columns

Date

Bookings

Revenue

Refunds

Net Revenue

Average Booking Value

Highest Booking

Lowest Booking

---

# 11. Slot Utilization Sheet

Columns

Date

Turf

Available Slots

Booked Slots

Blocked Slots

Utilization %

Peak Hour

---

# 12. Daily Report

Generated:

Every Night

Example

Today's Bookings

↓

Revenue

↓

Utilization

↓

Excel

↓

WhatsApp Owner

---

# 13. Weekly Report

Generated:

Monday Morning

Includes

Bookings

Revenue

Top Customers

Peak Hours

Business Growth

---

# 14. Monthly Report

Generated:

1st Day of Month

Includes

Revenue

Growth

Top Customers

Top Turf

Cancellation Rate

Utilization

---

# 15. Revenue Report

Revenue grouped by

Day

Week

Month

Year

---

# 16. Customer Report

Shows

Frequent Customers

High Spending Customers

Recent Customers

Inactive Customers

---

# 17. Payment Report

Shows

Successful Payments

Failed Payments

Refunds

Pending Payments

Gateway Summary

---

# 18. Slot Utilization

Formula

Booked Slots

÷

Available Slots

×

100

Example

20 Booked

24 Available

=

83.33%

---

# 19. Business KPIs

Report should include:

Total Revenue

Booking Count

Average Revenue Per Booking

Cancellation Rate

Refund Rate

Peak Booking Hour

Repeat Customers

Average Occupancy

---

# 20. AI Generated Reports

Owner

Today's report

↓

AI

↓

Generate Excel

↓

Send Document

---

Owner

Monthly revenue

↓

Generate Report

↓

Send Excel

---

# 21. Report Naming Convention

Examples

Daily_Report_2026-07-24.xlsx

Weekly_Report_2026_W30.xlsx

Monthly_Report_2026_07.xlsx

Revenue_Report_2026.xlsx

---

# 22. Report Storage

Reports stored in:

Database Metadata

↓

Cloud Storage

↓

Temporary Download URL

Old reports remain accessible.

---

# 23. Report Retention

Daily Reports

90 Days

Weekly Reports

1 Year

Monthly Reports

5 Years

Retention policy configurable.

---

# 24. Excel Formatting

Workbook should include:

Bold Headers

Auto Filter

Auto Width

Currency Formatting

Date Formatting

Frozen Header Row

Alternating Row Colors

Conditional Formatting

Professional appearance.

---

# 25. Conditional Formatting

Examples

Confirmed Booking

Green

Cancelled

Red

Pending Payment

Orange

Refunded

Blue

High Revenue

Highlight

---

# 26. Charts (Future)

Future versions may include

Revenue Trend

Booking Trend

Peak Hours

Monthly Growth

Top Customers

Cancellation Analysis

---

# 27. Report Generation Workflow

Owner

↓

Generate Report

↓

Backend

↓

Database Query

↓

Excel Generator

↓

Upload File

↓

WhatsApp Document

---

# 28. Report Security

Owner can access:

Only own business reports.

Customer cannot access reports.

Managers may receive limited reports.

---

# 29. Export Rules

Excel exports should:

Use UTF-8 encoding

Handle Marathi names

Handle Hindi names

Support large datasets

Prevent formula injection

---

# 30. Formula Injection Prevention

If cell values begin with:

=

+

-

@

Prefix with apostrophe (')

Example

Input

=SUM(A1:A5)

Stored

'=SUM(A1:A5)

Prevents Excel formula execution.

---

# 31. Empty Report Handling

If no bookings exist

Generate workbook

Summary

"No Bookings Found"

Still deliver report.

---

# 32. Scheduled Reports

Automatic

Daily

23:59

Weekly

Monday 08:00

Monthly

1st Day

08:00

---

# 33. Manual Reports

Owner commands

Today's Report

Weekly Report

Revenue Report

Customer Report

Payment Report

---

# 34. Performance

Excel generation target:

< 5 Seconds

For

10,000 bookings

---

# 35. Error Handling

Generation Failed

↓

Retry

↓

Notify Owner

↓

Log Error

---

# 36. Audit Logging

Every report stores

Report ID

Business ID

Generated By

Generated Time

Report Type

Download Count

---

# 37. Future Enhancements

Future reports:

PDF Reports

Interactive Dashboard

Power BI Integration

Google Sheets Sync

Email Delivery

CSV Export

---

# 38. Technology

Recommended

Java

↓

Apache POI

↓

Excel Workbook

↓

Upload Storage

↓

WhatsApp Document

---

# 39. MVP Report Summary

The MVP supports:

✓ Daily Reports

✓ Weekly Reports

✓ Monthly Reports

✓ Revenue Reports

✓ Customer Reports

✓ Payment Reports

✓ Slot Utilization

✓ Automatic Delivery

✓ WhatsApp Distribution

✓ Excel Download

---

# 40. Next Document

The next document is:

docs/11-error-handling.md

This document defines:

- Global exception handling
- AI failures
- Payment failures
- WhatsApp failures
- Booking conflicts
- Retry strategies
- Dead-letter queues
- Monitoring
- Logging
- Recovery procedures