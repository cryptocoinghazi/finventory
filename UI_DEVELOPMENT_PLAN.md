Finventory UI Development Plan & Prompt Sheet (Pro UI Edition)
🎛 Global UI Quality Bar (must apply to every step)

🛑 Operational Protocol: Command Logging & Verification (Flexible)
1. Logging command output to files is recommended for long or noisy runs.
2. Analyze output (console or log) to verify success/failure before proceeding.
3. Log files should be ignored by git when used (`*.log` in .gitignore).
4. Prioritize speed during routine tasks; use logs when helpful.

The UI must look and feel like a premium SaaS product:

Clean grid, consistent spacing scale, strong typography hierarchy

Soft shadows, 2xl rounded corners, subtle borders, refined hover/focus states

High-quality empty states, skeleton loaders, and error states

Keyboard-first usability (Tab order, ESC closes, Cmd/Ctrl+K search)

Responsive for laptop + desktop, and usable on tablets

Design consistency across all modules (tables/forms/actions)

Tech standards (use consistently)

Next.js 14 App Router + TypeScript

Tailwind + shadcn/ui (Radix)

React Hook Form + Zod

TanStack Query for API data

TanStack Table for data tables

Sonner (or shadcn toast) for notifications

Playwright for E2E

Component tests (Testing Library)

Backend API Reality Map (must follow; do not assume endpoints)

Auth
- POST /api/v1/auth/login
- POST /api/v1/auth/register

Masters
- Parties: POST/GET /api/v1/parties, GET/PUT/DELETE /api/v1/parties/{id}
- Items: POST/GET /api/v1/items, GET/PUT/DELETE /api/v1/items/{id}
- Warehouses: POST/GET /api/v1/warehouses, GET/DELETE /api/v1/warehouses/{id} (no update endpoint)
- Tax Slabs: POST/GET /api/v1/tax-slabs, DELETE /api/v1/tax-slabs/{id} (no get-by-id, no update endpoint)

Sales
- Sales Invoices: POST/GET /api/v1/sales-invoices, GET /api/v1/sales-invoices/{id} (no update, no preview endpoint)
- Sales Returns: POST /api/sales-returns (no list/view endpoints)

Purchase
- Purchase Invoices: POST /api/purchase-invoices (no list/view endpoints)
- Purchase Returns: POST /api/purchase-returns (no list/view endpoints)

Reports
- GET /api/reports/stock-summary (no filters)
- GET /api/reports/party-outstanding (no filters)
- GET /api/reports/gstr-1
- GET /api/reports/gstr-2
- GET /api/reports/gstr-3b

UI rule: If a screen needs an API that does not exist, the UI must show it as Coming Soon or hide it, and log the missing API in UI_TRACKER.md Issue Log.

Backend API TODO (for UI parity / unlock Coming Soon UI)

Masters
- Warehouses: Add PUT /api/v1/warehouses/{id}
- Tax Slabs: Add GET /api/v1/tax-slabs/{id} and PUT /api/v1/tax-slabs/{id}

Sales
- Sales Invoices:
  - Add PUT /api/v1/sales-invoices/{id} (if editing is required)
  - Add preview endpoint (recommended): POST /api/v1/sales-invoices/preview
    - Purpose: return computed totals/taxes without persisting, so UI never re-implements GST logic
- Sales Returns:
  - Add GET /api/sales-returns and GET /api/sales-returns/{id} for list/view screens
  - Add optional query support: ?fromDate=&toDate=&partyId=&warehouseId=&status=

Purchase
- Purchase Invoices:
  - Add GET /api/purchase-invoices and GET /api/purchase-invoices/{id} for list/view screens
  - Add optional query support: ?fromDate=&toDate=&partyId=&warehouseId=&status=
- Purchase Returns:
  - Add GET /api/purchase-returns and GET /api/purchase-returns/{id} for list/view screens
  - Add optional query support: ?fromDate=&toDate=&partyId=&warehouseId=&status=

Reports (filters + export readiness)
- Stock Summary:
  - Add query params: GET /api/reports/stock-summary?asOfDate=&warehouseId=&itemId=
- Party Outstanding:
  - Add query params: GET /api/reports/party-outstanding?asOfDate=&partyType=&minOutstanding=
- GST Registers:
  - Add query params (recommended): date-range and optional party/state filters for /gstr-1 and /gstr-2

Admin (only if we want the Admin screens in navigation)
- Users:
  - Add GET /api/v1/users, GET /api/v1/users/{id}, PUT /api/v1/users/{id}
  - Add role/permission assignment endpoints if needed

UI building blocks (must exist early and reused everywhere)

/components/ui-kit/

AppShell (sidebar + topbar + content)

PageHeader (title, subtitle, primary CTA, secondary actions)

DataTablePro (filters row, column config, server paging, skeleton, empty state)

FormLayout + FormSectionCard (sticky footer actions)

SmartSelect (async search select for party/item)

ConfirmDialog

Toast system

EmptyState (illustration optional)

InlineErrorCallout

CommandPalette (Cmd/Ctrl+K global search/navigation)

Kbd component

MoneyText (₹ formatting)

StatusPill (Draft/Posted/Cancelled)

PrintLayout wrapper

Visual system (single theme across app)

Define tokens in tailwind.config:

--radius = 16px (2xl)

Neutral background layers (surface, elevated, subtle)

Brand primary (blue) but muted + classy

Typography:

Page titles 24–28px, section headers 16–18px, body 14–16px

Layout grid:

Content max-width ~1280px, 24px padding desktop

🛑 “Fix-Then-Proceed” Protocol (keep exactly)

If any bug/regression/blocker occurs:

Pause current feature

Log in UI_TRACKER.md under Issue Log

Diagnose (Backend/Frontend)

Fix immediately

Verify with a test

Resume feature

📌 Step-by-Step Implementation Prompts (Upgraded for Stunning UI)
Step 1: F1 — UI Foundation (Setup)

Goal: Initialize project + design system + app shell + auth + testing base.

✅ Prompt (replace your Step 1 prompt with this)

“Initialize the Finventory Frontend (Premium UI Standard).
Create a new Next.js 14 App Router project with TypeScript and Tailwind in /frontend.
Install and configure shadcn/ui and Radix. Establish a polished design system:

Define theme tokens (colors, spacing, radius=16px, typography scale, shadows, borders)

Implement a global AppShell layout: premium sidebar navigation + topbar

Sidebar: Masters, Sales, Purchase, Reports, Admin

Topbar: breadcrumb area, global search (Cmd/Ctrl+K), user menu

Create /components/ui-kit/ primitives:
PageHeader, DataTablePro (TanStack Table), FormSectionCard, ConfirmDialog, EmptyState, InlineErrorCallout, MoneyText, StatusPill, Toast, CommandPalette, Kbd.

Implement API client wrapper:

Base URL config

Auth token injection

Global error handler (401 -> redirect /login, clear token)

Standard error shape handling in UI

Implement /login page using premium form UX:

field icons, clear validation messages, loading state, disabled button while submitting

Route protection: redirect unauthenticated users to /login

Create a real /dashboard page (no fake data):
- Show quick links to Masters/Sales/Purchase/Reports
- Show a small status panel sourced from what is actually available (or show Coming Soon)

Add loading skeleton components for list pages and form pages

Add Playwright E2E: login -> dashboard visible -> logout works

Add component tests for PageHeader, DataTablePro empty+loading state
After: run lint, unit tests, component tests, Playwright. Fix everything before proceeding.”

Step 2: F2 — Masters: Party (Premium CRUD)
✅ Prompt

“Implement Feature F2: Party Management (Premium UI).
Create Party types from backend API.
Build screens:

/masters/parties list using DataTablePro with:

search (debounced), filters (Customer/Vendor, State)

column sorting, server pagination

row actions: View/Edit/Delete via kebab menu

empty state CTA ‘Add Party’

/masters/parties/new and /masters/parties/[id]/edit using FormLayout + FormSectionCard
Fields: Name, Type, GSTIN, State Code, Address, Phone, Email, Credit Days
Validation:

GSTIN regex

If GSTIN present -> State Code required
UX requirements:

Sticky footer actions (Save, Save & New, Cancel)

Success toast with “View Party” action

Inline error callout for API errors
Testing:

Component tests: form validation + submit loading state

Playwright E2E: create party -> appears in list -> edit party -> saved
Ensure consistent theme, spacing, and keyboard accessibility.”

Step 3: F3 — Masters: Item
✅ Prompt

“Implement Feature F3: Item Management (Premium UI).
Screens:

/masters/items list with filters (Type, Tax Slab, HSN) + bulk actions (optional)

/masters/items/new and /masters/items/[id]/edit
Fields: Code, Name, Type, HSN, Unit, Tax Slab (async select), IsActive
UX:

Show computed label: “GST: 18%” near Tax slab

Skeleton while loading lookups
Testing:

Component: create/edit item + dropdown fetch

Playwright: create item -> verify list and search works
Reuse DataTablePro & SmartSelect components.”

Step 4: F4 — Masters: Warehouse
✅ Prompt

“Implement Feature F4: Warehouse Management (Premium UI).
Screens: list + create + view/delete
Note: Backend has no update endpoint for warehouses. Do not implement edit until backend adds PUT /api/v1/warehouses/{id}. Mark edit as Coming Soon.
UX: consistent with Party/Item pages, empty state CTA
Testing: Playwright create warehouse.”

Step 5: F5 — Masters: Tax Slab
✅ Prompt

“Implement Feature F5: Tax Slab Management (Premium UI).
Screens: list + create + delete
Note: Backend has no update endpoint and no get-by-id for tax slabs. Do not implement edit until backend adds GET/PUT endpoints. Mark edit as Coming Soon.
UX requirement:

Provide a beautiful ‘Tax Split’ preview card:

Intra: CGST + SGST

Inter: IGST
Testing: Playwright smoke + component validation test.”

Step 6: F6 — Sales Invoice (Showpiece Screen)

This is the screen that sells the product. Make it stunning.

✅ Prompt

“Implement Feature F6: Sales Invoice (Premium ERP-grade UX).
Screens:

/sales/invoices list: date range + party + status filters, quick actions (Create, Export)

/sales/invoices/new create form:
Header: Date, Party (async search), Warehouse, Place of Supply
Lines grid (table editor):

Item async select (searchable), auto-fill HSN, auto tax slab from Item

Qty, Rate, Discount, Tax displayed

Inline row validation + keyboard-friendly row add/remove

Row subtotal visible
Footer summary panel (sticky):

Taxable, CGST, SGST, IGST, Round-off, Grand Total
Rules:

Do NOT duplicate GST logic in frontend. Treat backend as source of truth.

Implement a “Preview Totals” action if backend supports draft preview; else show totals after save.
Invoice view:

/sales/invoices/[id] with:

Summary header + status pill

Tabs: Items, Taxes, Ledger Posting, Audit (if endpoints exist)

Print button
Print:

Build a premium print template (A4) with clear GST breakup and QR placeholder
Testing:

Component: line editor (add/remove rows, validate qty, async item select)

Playwright: create invoice -> appears in list -> open view -> print preview renders
Ensure this screen uses the best spacing, typography, and subtle micro-interactions.”

Step 7: F7 — Purchase Invoice
✅ Prompt

“Implement Feature F7: Purchase Invoice (Mirror Sales UX).
Same patterns, same components, consistent feel.
Add status flow if backend expects APPROVED.
Note: Backend currently exposes only POST /api/purchase-invoices. Build Create-first UI; list/view pages must be Coming Soon until backend adds GET endpoints.
Playwright: create purchase invoice.”

Step 8: F8 — Returns
✅ Prompt

“Implement Feature F8: Returns (Premium UX).
Sales return: search original invoice with smart lookup, prefill lines, allow qty adjustments, show reversal totals.
Purchase return similar.
Note: Backend currently exposes only POST endpoints for returns. Build Create-first UI; list/view pages must be Coming Soon until backend adds GET endpoints.
Playwright: create return from existing invoice.”

Step 9: F9 — Reports: Stock Summary
✅ Prompt

“Implement Feature F9: Stock Summary Report (Premium analytics table).
Backend currently provides no query filters; keep filters disabled or mark as Coming Soon until backend adds parameters.
Run report, table + export, skeleton loading.
Playwright: run report -> verify results.”

Step 10: F10 — Party Outstanding
✅ Prompt

“Implement Feature F10: Party Outstanding (Premium).
Add quick filter chips (Receivable/Payable), export, and empty state.
Backend currently provides no query filters; keep filters disabled or mark as Coming Soon until backend adds parameters.
Playwright smoke.”

Step 11: F11 — GST Registers
✅ Prompt

“Implement Feature F11: GST Registers (Premium).
Tabs: Sales Register / Purchase Register / HSN Summary
Export CSV/PDF if supported.
Backend provides /api/reports/gstr-1, /gstr-2, /gstr-3b. Implement UI against these endpoints first; add date-range filters only when backend supports parameters.
Playwright smoke.”

Step 12: F12 — Desktop Packaging (Electron)
✅ Prompt

“Implement Feature F12: Electron Desktop App (Professional packaging).
Electron wrapper loads Next.js build.
Add a polished Settings modal:

API base URL

Printer preferences
Add packaging scripts + basic installer build.
Desktop smoke test script.”

✅ Extra: Add This To Every Prompt Automatically (UI Input Prompt Change)

Append this at the bottom of every feature prompt in Trae:

“UI Quality Clause:

Use the shared UI-kit components only; do not create one-off styles.

Use consistent spacing scale, typography, radius, shadows.

Always include: skeleton loading, empty state CTA, error callout, keyboard accessibility.

Ensure every page has: PageHeader + Primary action + consistent table/form layout.

Keep interactions crisp: hover states, focus rings, subtle transitions.”
