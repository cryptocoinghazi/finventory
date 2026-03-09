# Finventory UI Development Tracker

## 📌 Development Protocol
1.  **Strict Sequential Order**: Do not start F(N+1) until F(N) is fully marked as `[x]`.
2.  **Fix-Then-Proceed**: If a bug is found in a previous feature during development, STOP current work, FIX the bug, VERIFY it, then resume.
3.  **Component Tests**: Every UI component (Form, Table) must have a passing test.
4.  **E2E Smoke Tests**: Every feature must have a corresponding E2E test scenario.

---

## 🚀 Feature Implementation Status

### F1. UI Foundation (App Shell + Auth)
- [ ] **Project Setup**
    - [ ] Initialize Next.js 14 (App Router) with TypeScript.
    - [ ] Setup Tailwind CSS & shadcn/ui.
    - [ ] Define Theme Tokens (Radius=16px, Typography, Shadows).
- [ ] **Core Components**
    - [ ] `AppShell` (Sidebar + Topbar + Content).
    - [ ] `PageHeader` (Title, Subtitle, Actions).
    - [ ] `DataTablePro` (Server paging, skeleton, empty state).
    - [ ] `FormLayout` + `FormSectionCard` (Sticky footer actions).
    - [ ] `SmartSelect` (Async search).
    - [ ] `ConfirmDialog`, `Toast`, `InlineErrorCallout`, `EmptyState`.
    - [ ] `MoneyText`, `StatusPill`, `CommandPalette`, `Kbd`, `PrintLayout`.
- [ ] **API Client**
    - [ ] `fetch` wrapper with Base URL & Auth Header injection.
    - [ ] Global Error Handling (401 -> Redirect to Login).
- [ ] **Authentication**
    - [ ] Login Page (`/login`) with JWT handling.
    - [ ] Route Guards (Middleware for protected routes).
    - [ ] Sidebar Navigation & App Shell Layout.
    - [ ] Dashboard (`/dashboard`) shows real data or Coming Soon.
- [ ] **Testing**
    - [ ] Component Test: `PageHeader`, `DataTablePro`.
    - [ ] E2E Test: Login Flow -> Dashboard Load.

### F2. Masters: Party Management
- [ ] **Screens**
    - [ ] Party List (`/masters/parties`) with Search/Filter.
    - [ ] Create Party Form (`/masters/parties/new`).
    - [ ] Edit Party Form (`/masters/parties/[id]/edit`).
- [ ] **Logic**
    - [ ] GSTIN Validation Regex.
    - [ ] State Code Lookup.
- [ ] **Testing**
    - [ ] Component Test: Form Validation.
    - [ ] E2E Test: Create Party -> Verify in List.

### F3. Masters: Item Management
- [ ] **Screens**
    - [ ] Item List (`/masters/items`).
    - [ ] Create Item Form (`/masters/items/new`).
    - [ ] Edit Item Form (`/masters/items/[id]/edit`).
- [ ] **Logic**
    - [ ] Tax Slab Selection Lookup.
    - [ ] Unit Selection Lookup.
- [ ] **Testing**
    - [ ] Component Test: Form Validation.
    - [ ] E2E Test: Create Item -> Verify in List.

### F4. Masters: Warehouse Management
- [ ] **Screens**
    - [ ] Warehouse List (`/masters/warehouses`).
    - [ ] Create Warehouse Form.
    - [ ] Edit Warehouse (Coming Soon: requires PUT /api/v1/warehouses/{id}).
- [ ] **Testing**
    - [ ] E2E Test: Create Warehouse.

### F5. Masters: Tax Slab Management
- [ ] **Screens**
    - [ ] Tax Slab List (`/masters/taxes`).
    - [ ] Create Tax Slab Form.
    - [ ] Edit Tax Slab (Coming Soon: requires GET/PUT /api/v1/tax-slabs/{id}).
- [ ] **Logic**
    - [ ] Rate Percent Selection (0, 5, 12, 18, 28).
- [ ] **Testing**
    - [ ] E2E Test: Create Tax Slab.

### F6. Sales: Invoice Management
- [ ] **Screens**
    - [ ] Invoice List (`/sales/invoices`).
    - [ ] Create Invoice Form (`/sales/invoices/new`).
    - [ ] View Invoice (`/sales/invoices/[id]`).
- [ ] **Logic**
    - [ ] Line Item Editor (Add/Remove Items).
    - [ ] Auto-calculate Totals (via Backend Preview if avail, or local approx).
    - [ ] Print Preview (PDF Generation/Layout).
- [ ] **Testing**
    - [ ] Component Test: Line Item Editor.
    - [ ] E2E Test: Create Invoice -> View -> Print.

### F7. Purchase: Invoice Management
- [ ] **Screens**
    - [ ] Create Invoice Form (`/purchase/invoices/new`).
    - [ ] Invoice List/View (Coming Soon: backend exposes only POST /api/purchase-invoices).
- [ ] **Testing**
    - [ ] E2E Test: Create Purchase Invoice.

### F8. Returns Management
- [ ] **Screens**
    - [ ] Sales Return Create.
    - [ ] Purchase Return Create.
    - [ ] Return Lists/Views (Coming Soon: backend exposes only POST endpoints).
- [ ] **Logic**
    - [ ] Original Invoice Lookup & Selection.
- [ ] **Testing**
    - [ ] E2E Test: Create Sales Return.

### F9. Reports: Stock Summary
- [ ] **Screens**
    - [ ] Stock Summary Page (`/reports/stock-summary`).
- [ ] **Logic**
    - [ ] Filters (Coming Soon: backend has no query params yet).
    - [ ] Export to CSV.
- [ ] **Testing**
    - [ ] E2E Test: Generate Report.

### F10. Reports: Party Outstanding
- [ ] **Screens**
    - [ ] Outstanding Page (`/reports/outstanding`).
- [ ] **Logic**
    - [ ] Filters (Coming Soon: backend has no query params yet).
- [ ] **Testing**
    - [ ] E2E Test: Generate Report.

### F11. Reports: GST Registers
- [ ] **Screens**
    - [ ] GST Registers Page (`/reports/gst-registers`).
- [ ] **Logic**
    - [ ] GSTR-1 / GSTR-3B Toggle.
    - [ ] Date range filters (Coming Soon: add after backend supports params).
- [ ] **Testing**
    - [ ] E2E Test: Generate Report.

### F12. Desktop Packaging (Electron)
- [ ] **Setup**
    - [ ] Electron Main Process Setup.
    - [ ] Load Next.js build.
- [ ] **Features**
    - [ ] Settings Modal (API URL, Printer).
- [ ] **Testing**
    - [ ] Manual Launch Test.

---

## 🐛 Issue Log
| ID | Description | Status | Fix Commit |
| :--- | :--- | :--- | :--- |
| | | | |
