    - [x] Invoice List (`/purchase/invoices`).
    - [x] View Invoice (`/purchase/invoices/[id]`).
- [x] **Logic**
    - [x] Vendor Selection.
    - [x] Vendor Invoice Number.
    - [x] Line Item Editor.
- [ ] **Testing**
    - [ ] E2E Test: Create Purchase Invoice.

### F8. Returns Management
- **Status**: [x] Completed
- **Priority**: High
- **Description**: Handle Sales Returns (Credit Note) and Purchase Returns (Debit Note).
- **Sub-tasks**:
  - [x] Sales Return List (Frontend)
  - [x] Purchase Return List (Frontend)
  - [x] Sales Return Creation Form (Frontend + Logic)
  - [x] Purchase Return Creation Form (Frontend + Logic)
  - [x] Sales Return Detail View (Frontend + Logic)
  - [x] Purchase Return Detail View (Frontend + Logic)
  - [x] Backend Integration (API Client)

### F9. Reports: Stock Summary
- **Status**: [x] Completed
- **Screens**
    - [x] Stock Summary Page (`/reports/stock-summary`).
- **Logic**
    - [x] Filters (Client-side implemented).
    - [ ] Export to CSV (Future).
- **Testing**
    - [ ] E2E Test: Generate Report.

### F10. Reports: Party Outstanding
- **Status**: [x] Completed
- **Screens**
    - [x] Outstanding Page (`/reports/outstanding`).
- **Logic**
    - [x] Filters (Client-side implemented).
- **Testing**
    - [ ] E2E Test: Generate Report.

### F11. Reports: GST Registers
- **Status**: [x] Completed
- **Screens**
    - [x] GST Registers Page (`/reports/gst-registers`).
- **Logic**
    - [x] GSTR-1 / GSTR-3B Toggle.
    - [x] Date range filters (Client-side search available).
- **Testing**
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
| UI-001 | Browser CORS blocked /api/v1/auth/login from http://localhost:3000 | Fixed (CORS enabled + proxy route) | |
| UI-002 | Checkstyle MagicNumber failure on CORS maxAge | Fixed (constant introduced) | |
| UI-003 | Server-side filters/pagination missing for Parties endpoints | Tracked (client-side filters used) | |
| UI-004 | Authorization missing for /api/v1/users/me (non-admin) | Fixed (SecurityConfig updated) | |
| UI-005 | Missing UI components (Label, Toast) in Settings | Fixed (Components created) | |
| UI-006 | SmartSelect filtering not working for Tax Slabs | Fixed (Added server-side search to TaxSlabController) | |
| UI-007 | WarehouseForm manual validation inconsistent | Fixed (Refactored to Zod + React Hook Form) | |
