# Finventory Specification

## 1. Project Overview
Finventory is a local-first, desktop-class Inventory & GST Accounting Application built with modern web technologies.
It is designed for Indian SMEs who need robust inventory tracking, GST-compliant invoicing, and financial reporting.

## 2. Tech Stack
- **Backend:** Java 21, Spring Boot 3.2+
- **Database:** PostgreSQL 15+ (Local Docker container or native install)
- **Frontend:** Next.js 14 (React), Tailwind CSS, Shadcn UI
- **Desktop Wrapper:** Electron (for native OS integration)
- **Build Tools:** Maven (Backend), npm (Frontend)

## 3. Core Modules (Slices)

### Slice A: Masters & Sales
- **Masters:**
  - Party (Customer/Vendor): Name, GSTIN, Address, Contact.
  - Item (Product): Name, Code/SKU, HSN, Tax Rate, Unit, Price.
  - TaxSlab: GST Rates (0%, 5%, 12%, 18%, 28%).
  - Warehouse: Location management.
- **Sales Invoice:**
  - Invoice Header: Date, Customer, Invoice No.
  - Invoice Lines: Item, Qty, Rate, Tax Amount, Total.
  - GST Logic: Intra-state (CGST+SGST) vs Inter-state (IGST).
  - Stock Impact: Reduce inventory on sale.
  - GL Impact: Post accounting entries.

### Slice B: Purchase & Inventory
- **Purchase Invoice:**
  - Vendor bills.
  - Stock Impact: Increase inventory on purchase.
  - GL Impact: Accounts Payable.

### Slice C: Returns & Credit Notes
- Sales Return (Credit Note).
- Purchase Return (Debit Note).

### Slice D: Reports
- Stock Summary (Current stock levels).
- Party Outstanding (Receivables/Payables).
- GSTR-1 / GSTR-3B data.

### Slice E: Desktop Packaging
- Electron integration.
- Offline-first capabilities (future scope).

## 4. Database Schema (High Level)

### Users & Auth
- `users`: id, username, password_hash, role (ADMIN, USER).

### Masters
- `parties`: id, name, type (CUSTOMER/VENDOR), gstin, address, phone, email.
- `items`: id, name, code, hsn_code, tax_rate, unit_price, uom.
- `warehouses`: id, name, location.

### Transactions
- `sales_invoices`: id, invoice_number, date, party_id, total_amount, tax_amount.
- `sales_invoice_lines`: id, invoice_id, item_id, qty, rate, amount, tax_amount.
- `stock_ledger`: id, date, item_id, warehouse_id, qty_in, qty_out, ref_type, ref_id.
- `gl_entries`: id, date, account_head, debit, credit, ref_type, ref_id.

## 5. Development Plan

### Phase 1: Backend Foundation (Completed)
- Project Setup.
- Security (JWT).
- Quality Gates.

### Phase 2: Masters Module (Current)
- CRUD APIs for Party, Item, Warehouse.
- Validation.

### Phase 3: Sales Module
- Invoice creation.
- GST Calculation.
- Stock & GL Posting.

### Phase 4: Purchase Module
- Purchase entry.
- Stock updates.

### Phase 5: Frontend & Desktop
- UI implementation.
- Electron wrapping.
