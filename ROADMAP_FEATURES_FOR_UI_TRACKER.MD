Finventory — Product Roadmap & UI Delivery Plan
Professional roadmap + per-feature UI trackers and Trae prompts (Desktop + Web)

Owner	Ghazi Syed
Product Name	Finventory
Scope	India GST ERP (MARG-like) — Desktop + Web (Electron + Next.js)
Assumption	Backend & APIs are complete and tested; document focuses on Frontend + Release planning
Doc Version	1.0
Date	March 09, 2026

How to use this document in Trae: Add this file to your Trae workspace root (e.g., /docs). Start each UI feature by copying the feature’s Trae prompt block into Trae, implement the screens, run tests, and then update the feature tracker section accordingly.

1. Executive Summary

Finventory is a GST-compliant ERP for Indian trade workflows (Sales, Purchase, Inventory, Accounting) delivered as a single Web UI (Next.js) and a Desktop wrapper (Electron). The backend is already implemented and verified by unit + integration tests. This roadmap formalizes the UI build plan, release phases, and per-feature trackers with ready-to-run Trae prompts.
1.1 Release Goals (MVP)
Ship a complete Masters → Sales → Purchase flow with printing and role-based access.
Deliver core operational reports: Stock Summary, Party Outstanding, GST Registers.
Maintain consistent UI theme and component patterns across Web + Desktop.
Automated tests: component tests for critical forms + E2E smoke tests for every release candidate.
2. Product Roadmap (Frontend)

2.1 Milestones
Milestone	Focus	Key Outputs	Exit Criteria
M0 — UI Foundation	Design system + app shell	Theme tokens, layout, navigation, auth screens, API client, error handling	Login works; protected routes; CI runs UI tests
M1 — Masters UI	CRUD screens for Party/Item/Warehouse/TaxSlab	Lists, create/edit forms, validations, import/export stubs	All Masters screens done + component tests
M2 — Sales UI	Sales invoice, returns, print	Invoice list, create/edit, view, print templates, return flow	E2E: create invoice end-to-end; print preview verified
M3 — Purchase UI	Purchase invoice, returns	Purchase list, create/edit, view, return flow	E2E: create purchase end-to-end
M4 — Reports & Dashboards	Operational + GST reports	Stock summary, outstanding, sales/purchase register, export	Report perf + export verified
M5 — Desktop Packaging	Electron packaging + printing	Installer build, auto-update placeholder, native print integration	Desktop smoke test passed (Win)

2.2 Release Phasing (Suggested)
Recommended release approach: ship incremental internal builds after each milestone. MVP external pilot can start after M2 (Sales) if Masters are stable, then expand with Purchase and Reports.

3. UI System & Consistency Standards

3.1 Tech & UI Toolkit (assumed)
Frontend: Next.js 14 (App Router), TypeScript, Tailwind CSS, shadcn/ui components, React Hook Form + Zod for validation, TanStack Query for API state, Playwright for E2E.
3.2 Global UI Rules (must follow for consistency)
Single layout shell: Top bar + left navigation; consistent page header with title + primary action.
All lists use the same table component: server paging, column configs, filters, empty state.
All forms use: React Hook Form + Zod schema; inline field errors; submit disabled while saving.
Consistent feedback: toast for success; inline callout for API errors; 401 triggers logout.
All currency values: ₹ formatting; BigDecimal-style rounding (2 decimals) displayed consistently.
Role-based UI gating mirrors backend roles (ADMIN/USER plus module permissions).
3.3 Screen Patterns (standard templates)
List Page: page header + filters row + data table + row actions (View/Edit/Delete) + pagination.
Create/Edit Page: breadcrumb + form sections (cards) + sticky footer actions (Save/Cancel).
View Page: summary header + tabs (Lines, Taxes, Ledger/Posting, Audit) + print action.
Report Page: parameters panel + run report + table + export (CSV/PDF) + print.

4. Screen Inventory (Web + Desktop)

4.1 Navigation Map (MVP)
Area	Screens (routes)	Count
Auth	/login, /forgot-password (optional)	1–2
Dashboard	/dashboard	1
Masters	/masters/parties, /masters/items, /masters/warehouses, /masters/taxes	4
Sales	/sales/invoices (list), /sales/invoices/new, /sales/invoices/[id], /sales/returns	3–4
Purchase	/purchase/invoices (list), /purchase/invoices/new, /purchase/invoices/[id], /purchase/returns	3–4
Reports	/reports/stock-summary, /reports/outstanding, /reports/gst-registers	3
Admin	/admin/users, /admin/roles (optional for MVP)	1–2
Desktop	Settings modal (API base URL), Print setup	1–2

Estimated MVP screen count: ~16–20 screens (including create/view variants and desktop settings).

5. Feature Trackers (Frontend) + Trae Build Prompts

Each feature below is tracked separately. Start a feature only after the previous feature’s tracker is in 'Completed' state.

F1. UI Foundation (App Shell + Theme + API Client)
Field	Details
Screens	Layout shell, sidebar navigation, page header component, toast system
API Dependencies	Auth endpoints, user profile endpoint (optional)
Tests Required	Unit tests for utilities; component tests for layout; E2E login smoke
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
You are implementing the Finventory frontend foundation in Next.js 14 (App Router) + TypeScript.
Requirements:
- Add Tailwind + shadcn/ui and define theme tokens (colors, typography, spacing). Ensure consistent styling across all pages.
- Create App Shell: sidebar navigation (Masters, Sales, Purchase, Reports, Admin), top bar with user menu, breadcrumbs support.
- Create shared UI components:
  1) PageHeader (title, subtitle, primary action)
  2) DataTable wrapper (server paging, filters slot, empty state)
  3) FormSection card (section title + content)
  4) ConfirmDialog
  5) Toast notifications
- Create API client using fetch wrapper with base URL, auth header injection, and global error handling:
  - On 401/403: redirect to /login and clear token
  - Display error callout for API errors (include correlation id if present)
- Create auth flow:
  - /login page with JWT login
  - store token securely (httpOnly cookie if possible; else localStorage with precautions)
  - protected route guard for /dashboard and all modules
- Add Playwright E2E test: login -> load dashboard.
- Add component tests for PageHeader and DataTable.
After implementation: run lint, unit tests, component tests, and Playwright; fix failures.

F2. Masters — Party Management UI
Field	Details
Screens	Party List, Party Create/Edit, Party View (optional)
API Dependencies	GET/POST/PUT/DELETE /parties; search + paging
Tests Required	Component tests for list + form; E2E: create party
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Party Management UI in Finventory (Next.js 14 + shadcn/ui).
Screens:
- /masters/parties (list with search, filters: type=Customer/Vendor, state)
- /masters/parties/new (create)
- /masters/parties/[id]/edit (edit)
Optional: /masters/parties/[id] (view)
Rules:
- Use shared PageHeader, DataTable, FormSection components.
- Forms use React Hook Form + Zod schema; show inline field errors.
Fields (minimum):
- name, type (Customer/Vendor), gstin, stateCode, address lines, phone/email, creditDays
Behavior:
- GSTIN validation (format) and stateCode required if gstin present.
- List supports server paging and debounced search.
Tests:
- Component test: form validation and submit
- E2E: create party and verify it appears in list
After implementation: run tests and ensure theme consistency.

F3. Masters — Item Management UI
Field	Details
Screens	Item List, Item Create/Edit, Item View (optional)
API Dependencies	GET/POST/PUT/DELETE /items; search + paging
Tests Required	Component tests for list + form; E2E: create item
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Item Management UI.
Screens:
- /masters/items (list with filters: type=Product/Service, taxSlab, hsn)
- /masters/items/new
- /masters/items/[id]/edit
Fields:
- code, name, type, hsn, unit, taxSlabId, barcode(optional), isActive
Rules:
- Reuse DataTable and FormSection.
- Add select lookups for Tax Slab and Unit.
Tests:
- Component test: create/edit item
- E2E: create item -> verify in list.

F4. Masters — Warehouse (Godown) UI
Field	Details
Screens	Warehouse List, Create/Edit
API Dependencies	GET/POST/PUT/DELETE /warehouses
Tests Required	Component test: create; E2E smoke
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Warehouse (Godown) UI.
Screens:
- /masters/warehouses (list)
- /masters/warehouses/new
- /masters/warehouses/[id]/edit
Fields:
- code, name, address(optional), isActive
Tests:
- Component test for form
- E2E: create warehouse.

F5. Masters — Tax Slab UI
Field	Details
Screens	Tax Slab List, Create/Edit
API Dependencies	GET/POST/PUT/DELETE /tax-slabs
Tests Required	Component test; E2E smoke
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Tax Slab UI.
Screens:
- /masters/taxes (list)
- /masters/taxes/new
- /masters/taxes/[id]/edit
Fields:
- name, ratePercent (0/5/12/18/28), cgst, sgst, igst, cess(optional), effectiveFrom
Rules:
- Display derived split (cgst+sgst vs igst) clearly.
Tests:
- Component test for validation
- E2E smoke.

F6. Sales — Invoice List + Create/Edit UI
Field	Details
Screens	Invoice List, Create Invoice, View Invoice, Print Preview
API Dependencies	Sales invoice APIs + lookups (party/item/tax/warehouse)
Tests Required	Component tests for line editor; E2E create invoice
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Sales Invoice UI with consistent UX.
Screens:
- /sales/invoices (list with filters: date range, party, status)
- /sales/invoices/new (create)
- /sales/invoices/[id] (view + print)
Optional: /sales/invoices/[id]/edit (if backend supports edit)
Create Invoice UX:
- Header: invoiceDate, party, placeOfSupply, warehouse
- Lines grid: add item, qty, rate, discount, tax slab; auto-calc totals from backend response
Rules:
- Do NOT duplicate tax logic in frontend; call backend for totals preview.
- Provide 'Calculate' / 'Preview totals' action that hits backend draft/preview endpoint if available; else compute display from API response post-save.
- Print view: A4 invoice layout with GST breakup and company/buyer details.
Tests:
- Component test: line editor add/remove, validation
- E2E: create invoice -> verify it appears in list -> open view -> print preview renders.

F7. Purchase — Invoice UI
Field	Details
Screens	Purchase List, Create, View, Print
API Dependencies	Purchase invoice APIs + lookups
Tests Required	Component tests; E2E create purchase
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Purchase Invoice UI mirroring Sales patterns for consistency.
Screens:
- /purchase/invoices (list)
- /purchase/invoices/new
- /purchase/invoices/[id]
Rules:
- Reuse components; same line editor base component if possible.
Tests:
- Component test for purchase form
- E2E: create purchase invoice.

F8. Returns — Sales Return & Purchase Return UI
Field	Details
Screens	Return List/Create/View for both Sales and Purchase
API Dependencies	Return APIs; reference original invoice lookup
Tests Required	E2E: create return
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Returns UI.
Screens:
- /sales/returns (list + create return referencing sales invoice)
- /purchase/returns (list + create return referencing purchase invoice)
UX:
- Search and select original invoice
- Auto-populate lines; allow adjusting return quantities
- Show reversal totals from backend
Tests:
- E2E: create sales return from an existing invoice.

F9. Reports — Stock Summary UI
Field	Details
Screens	Stock Summary report page with filters and export
API Dependencies	GET /reports/stock-summary
Tests Required	Component test: filters; E2E: run report
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Stock Summary report.
Screen:
- /reports/stock-summary
Filters:
- asOfDate, warehouse (optional), item (optional)
Output:
- Table: item, warehouse, inQty, outQty, balance
Export:
- CSV download
Tests:
- Component test for filters
- E2E: run report and verify table shows results.

F10. Reports — Party Outstanding UI
Field	Details
Screens	Outstanding report page with export
API Dependencies	GET /reports/outstanding
Tests Required	E2E smoke
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Party Outstanding report.
Screen:
- /reports/outstanding
Filters:
- asOfDate, partyType, minOutstanding
Output:
- Table: party, receivable/payable, age buckets (optional)
Export CSV.
Tests:
- E2E smoke.

F11. Reports — GST Registers UI
Field	Details
Screens	Sales/Purchase register, HSN summary, export
API Dependencies	GET /reports/gst-registers (or separate endpoints)
Tests Required	E2E smoke
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build GST Registers UI.
Screen:
- /reports/gst-registers
Filters:
- date range, type (GSTR-1 sales / purchase), state, party
Output:
- Register table + totals
- HSN summary tab
Export CSV/PDF if supported.
Tests:
- E2E smoke to run report.

F12. Desktop Packaging UI (Electron)
Field	Details
Screens	Desktop settings modal + print setup
API Dependencies	Same backend endpoints; desktop local settings storage
Tests Required	Desktop smoke manual + automated launch test if possible
Status	Not Started / In Progress / Blocked / Completed
Notes	

Tracker Checklist
UI pages created and wired into navigation
Reusable components extracted (table/form sections)
Validation + error states implemented
RBAC UI gating implemented (if applicable)
Component tests passing (forms + table)
E2E smoke test added/updated
Accessibility pass: labels, focus states, keyboard navigation
Trae Prompt (copy/paste)
Build Electron wrapper for Finventory.
Requirements:
- Load Next.js build in production; dev uses localhost.
- Add Settings screen/modal:
  - API base URL
  - Printer selection (if supported)
  - Toggle: open print preview in new window
- Add packaging scripts and basic installer build.
- Provide a smoke test script to launch desktop and load /dashboard after login.

6. Governance, QA & Acceptance

6.1 Definition of Done (per UI feature)
All screens for the feature completed and accessible via navigation
API integration complete with loading + error states
Component tests added/updated and passing
Playwright E2E smoke for the feature passes
No visual regressions: consistent spacing, typography, button styles
Accessibility check: labels, keyboard navigation, focus visible
Document tracker updated: status, notes, screenshots (optional)
6.2 Recommended Test Suite (minimum)
Component tests: Party form, Item form, Invoice line editor. E2E: login, create party, create item, create sales invoice, run stock summary report.
Tip: Keep a single Playwright 'smoke.spec.ts' that covers the core happy path. Run it on every PR.
