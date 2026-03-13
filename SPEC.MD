Design and implement the following Finventory product improvements as a structured, production-ready enhancement set.

IMPORTANT:
- Build this inside the existing Finventory architecture
- Keep code modular and production-grade
- Do not break current working modules
- Reuse existing UI patterns, backend architecture, auth, routing, and design system
- Apply changes step by step, but design them as a coherent product enhancement plan
- Add database migrations where needed
- Add backend + frontend validation
- Add tests where practical
- Keep role-based access in mind
- Prefer clear modules over hacks

==================================================
1. DELETE SALES / PURCHASE INVOICE
==================================================

Add safe delete functionality for:
- Sales Invoices
- Purchase Invoices

Requirements:
- Soft delete preferred if accounting/stock implications exist
- If hard delete is unsafe, implement cancel/archive/reverse logic instead
- Prevent deleting posted/finalized invoices without proper safeguards
- If delete affects stock/accounting, reverse stock and journal impact correctly
- Add confirmation dialog with warning text
- Show delete action only to authorized users
- Audit log all delete operations

UI:
- Delete action in invoice list and invoice detail page
- Show deleted/cancelled status clearly

==================================================
2. PARTY OUTSTANDING PAGE IMPROVEMENTS
==================================================

Improve Party Outstanding page and make it more useful.

Add features:
- Filter by customer/vendor
- Filter by date range
- Search by party name, phone, GSTIN
- Show ageing buckets:
  - 0–30
  - 31–60
  - 61–90
  - 90+
- Show total receivable / payable summary cards
- Export CSV / PDF
- Click party to open detailed ledger/outstanding breakdown
- Add “mark reminder sent” / note field if useful
- Better sorting and cleaner table layout
- Improve empty states and loading states

STATUS: DONE (2026-03-12)

==================================================
3. DATABASE BACKUP FUNCTIONALITY
==================================================

Create a backup module for Finventory database.

Requirements:
- Allow admin to trigger manual backup from UI
- Show backup history
- Show backup timestamp, size, status
- Support downloadable SQL/backup file if feasible
- Support local filesystem storage for backups initially
- Add retention rule configuration if feasible
- Add restore placeholder or restore design hooks, but do not implement destructive restore unless safe
- Secure admin-only access
- Add logging for backup actions

UI:
- New “Database Backup” page under Settings/Admin/System
- “Create Backup” button
- Backup list/history table
- Status badges

STATUS: DONE (2026-03-12)

==================================================
4. NOTIFICATIONS / ALERTS MODULE
==================================================

Design and implement useful notifications for Finventory.

First define practical, useful notifications.
Implement a notification center / alerts system.

Suggested useful notifications:
- Low stock alert
- Out of stock alert
- Pending invoice overdue
- Payment pending reminder
- Backup completed / backup failed
- Backup overdue
- New expense added
- Large discount/coupon applied
- Invoice deleted / reversed
- Migration completed / failed (if migration module remains)
- Daily sales summary
- Monthly sales target / milestone alert
- System warning / configuration issue

Requirements:
- Notifications bell in header/topbar
- Unread count badge
- Notification drawer/dropdown
- Mark as read / mark all as read
- Deep link to related page/entity
- Store notifications in DB
- Add notification types, severity, timestamps
- Admin settings for which alerts are enabled

Also propose which notifications should be:
- real-time / immediate
- daily summary
- admin-only
- cashier-visible

==================================================
5. SIDEBAR SCROLLABLE FIX
==================================================

Fix sidebar usability.

Current issue:
- last settings option is not visible unless browser zooms out

Requirements:
- Make sidebar vertically scrollable
- Keep header/top brand area fixed if appropriate
- Keep active item visible
- Ensure sidebar works well on laptop screens
- Ensure bottom options remain reachable
- Preserve responsive/mobile behavior

STATUS: DONE (2026-03-12)

==================================================
6. QUICK POS: ADD NEW ITEM FROM BILLING SCREEN
==================================================

In Quick POS, if an item is not available while billing, allow creating a new item directly from POS.

Requirements:
- Add “Add New Item” button / action in POS
- Open modal/dialog from POS screen
- Minimal fast-create form:
  - item name
  - barcode
  - price
  - category/vendor optional
  - stock optional
  - unit optional
- Save item without leaving POS
- Newly created item should become searchable/selectable immediately in POS
- Validation for required fields
- Role-based permission for who can add items from POS
- Audit log this action

UX:
- Must be fast and cashier-friendly
- Do not disrupt billing flow
- After create, focus should return to POS item search

STATUS: DONE (2026-03-12)

==================================================
7. OFFERS / COUPONS MODULE
==================================================

Create an Offers / Coupons module and integrate it with Quick POS.

Requirements:
- Create module for offers/coupons
- Add navigation placement appropriately in app
- Support:
  - flat discount
  - percentage discount
  - item-specific offer
  - cart-level coupon
  - date validity
  - active/inactive status
  - usage limits if feasible
  - minimum bill amount if needed
- Allow offers on:
  - individual items
  - full bill/cart
- Coupon code support
- Apply coupon in Quick POS during billing
- Show discount impact clearly in cart summary
- Prevent invalid/expired coupons
- Keep audit trail of applied offers/coupons

UI:
- Offers/Coupons list page
- Create/edit form
- POS coupon apply modal/input
- Display applied discount line in POS summary

STATUS: DONE (2026-03-12)
==================================================
8. ACCOUNTING MODULE WITH EXPENSES
==================================================

Create an Accounting module, starting with Expenses.

Requirements:
- Add “Accounting” module in sidebar/navigation
- Initial submodules:
  - Expenses
  - basic ledgers if needed
  - future-ready structure for income/expense/account reports

Expenses requirements:
- Add expense categories
- Create expense entries with:
  - title
  - category
  - amount
  - date
  - note
  - payment method optional
  - attachment placeholder optional
- Expense list page
- Create/edit/delete expense
- Search and filter by date/category/amount
- Expense totals summary
- Link expenses into accounting reports

==================================================
9. REPORTS MODULE EXPANSION
==================================================

Enhance Reports with:
- Sales report
- Stock report
- Income and Loss report
- Annual report
- Custom date range report

Detailed requirements:

A) Sales Report
- sales by day/week/month/custom range
- filter by item, category, customer, invoice status
- totals, discounts, paid/pending split
- export CSV/PDF

B) Stock Report
- stock summary
- low stock / out of stock
- stock movement if available
- item-wise quantity and valuation if feasible

C) Income and Loss Report
- revenue
- discounts
- expenses
- profit/loss view
- date range filter

D) Annual Report
- yearly summary
- sales trends
- expenses trends
- profit trend
- top items / top customers if feasible

E) Custom Date Report
- generic date range selection
- applies across reports
- reusable date filter component

STATUS: DONE (2026-03-12)
==================================================
10. INFORMATION ARCHITECTURE / PLACEMENT
==================================================

Place modules appropriately in the app.

Suggested sidebar structure:
- Dashboard
- Masters
- Sales
- Purchase
- POS / Quick POS
- Accounting
- Reports
- Offers / Coupons
- Notifications
- Settings / Admin / System

Refine placement so the app feels clean and logical.

==================================================
11. UX / UI EXPECTATIONS
==================================================

All new modules must:
- follow existing Finventory design system
- be clean, modern, and usable
- work on laptop and tablet
- have proper empty states, loading states, error states
- use confirmation dialogs for dangerous actions
- maintain consistency with current app styling

==================================================
12. BACKEND / DATA / SAFETY
==================================================

Requirements:
- add proper Flyway migrations
- add entities/repositories/services/controllers as needed
- keep APIs structured and RESTful
- add validation and error handling
- add audit logging where important
- do not break existing auth/security
- add permissions where relevant

==================================================
13. IMPLEMENTATION APPROACH
==================================================

Implement in manageable slices, but prepare the complete design.

Suggested execution order:
1. Sidebar scroll fix
2. Party outstanding improvements
3. Quick POS add item modal
4. Offers / Coupons module + POS integration
5. Accounting module with Expenses
6. Reports expansion
7. Notifications center
8. Backup module
9. Safe invoice delete/cancel/reverse

But first provide a clear implementation plan before coding.

==================================================
14. OUTPUT REQUIRED
==================================================

When done, provide:
1. implementation plan/module breakdown
2. DB changes / migrations added
3. backend modules added/changed
4. frontend pages/components added/changed
5. navigation/sidebar changes
6. notification types implemented
7. accounting/report structures implemented
8. limitations / future enhancements
9. testing done
