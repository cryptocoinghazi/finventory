MARG-like ERP (India GST)
Desktop + Web

TRAE Workspace Build Document
A step-by-step specification + copy/paste prompt pack for Trae IDE

Version: 1.0  |  Target: MVP (Billing + Inventory + Accounting)
Region: India (GST)  |  Clients: Web + Desktop (Electron)

How to use this document in Trae
Put this file in the root of your Trae workspace (repo) under /docs. In Trae, open this document and start a new chat with the project context enabled.

Recommended flow per module:
1) Paste the “Always-on Trae Instruction” once.
2) Execute prompts in order (Scaffolding → Masters → Sales → Purchase → Reports → Desktop).
3) After each prompt: run tests, fix failures, and commit.

Always-on Trae Instruction (paste once per session)
Follow repo rules strictly:
- Never mutate ledger history; use reversal entries only.
- Never change DB schema without a Flyway migration.
- Use DTOs; do not expose JPA entities from controllers.
- Money fields use BigDecimal with explicit rounding rules.
- All posting logic must be tested (unit + integration).
- Keep controllers thin; move business rules into services/domain.
1. Product scope (MVP)
Goal: Deliver a usable MARG-like ERP for India GST with Desktop + Web clients, built on a shared backend API.
1.1 MVP modules
    • Authentication + Role-based access
    • Masters: Parties, Items, Taxes, Units, Warehouses (Godowns), Price lists
    • Sales: Quotation → Invoice → Credit Note/Return
    • Purchase: Purchase Invoice → Debit Note/Return
    • Inventory: Stock ledger, stock summary, valuation (Average for MVP)
    • Accounting: Ledger, Vouchers (Receipt/Payment/Journal), Trial Balance
    • Reports: Sales/Purchase register, Stock summary, Party outstanding, GST summaries
    • Printing: A4 invoice + optional thermal format
    • Desktop wrapper: Electron packaging of the web UI
1.2 Phase-2 (optional / post-MVP)
    • GST e-Invoice JSON generation (INV-1) and IRP integration (when credentials available)
    • e-Way Bill integration/exports
    • Barcode/POS, batch/expiry, multi-branch, bank reconciliation, Tally import/export
2. Architecture
2.1 Desktop + Web strategy
Use a single React/Next.js UI codebase. Web runs in browser; Desktop uses Electron to host the same UI. Both talk to the same Spring Boot backend via REST APIs.
2.2 Backend stack (recommended)
    • Java 21 + Spring Boot 3
    • PostgreSQL + Flyway migrations
    • Spring Security (JWT) + RBAC
    • OpenAPI/Swagger for API docs
    • Testcontainers for integration tests
    • Docker compose for local dev; CI pipeline for build/test/lint
2.3 Non-negotiable ERP rules
    1. StockLedgerEntry and GLTransaction/GLLine are append-only (never edit history).
    2. All financial postings must balance: sum(debits) == sum(credits) per transaction.
    3. Document numbering is FY + Branch + Series + running number (concurrency-safe).
    4. Every document stores createdBy/createdAt and a traceable document reference for every ledger entry.
3. Repository structure (mono-repo)
Create the following top-level folders:
Path	Purpose
backend/	Spring Boot REST API + DB migrations + tests
web/	Next.js UI (shared by web and desktop)
desktop/	Electron wrapper + packaging
docs/	This spec + domain rules + API contracts + test strategy
4. Domain model (MVP)
4.1 Masters
    • Party (Customer/Vendor): name, GSTIN, PAN (optional), state code, addresses, credit terms
    • Item: code, name, HSN, unit, tax category/slab, rate, barcode (optional)
    • TaxSlab: CGST/SGST/IGST %, cess (optional)
    • Warehouse (Godown)
    • Branch + FinancialYear (for numbering and reporting)
4.2 Documents
    • SalesInvoice (header + lines)
    • PurchaseInvoice (header + lines)
    • CreditNote / DebitNote / Returns (reference original document)
4.3 Append-only ledgers
    • StockLedgerEntry: itemId, warehouseId, qtyIn, qtyOut, rate, docType, docId, postedAt
    • GLTransaction: voucherNo, docType/docId, postedAt
    • GLLine: accountId, debit, credit, narration, docRef
5. Build plan (vertical slices)
Build end-to-end slices to reach a working product fast. Each slice includes API + UI + tests.
Slice	Deliverables	Target
Slice A	Masters + Sales Invoice posting (stock OUT + GL) + print view	Weeks 1–2
Slice B	Purchase Invoice posting (stock IN + GL)	Week 3
Slice C	Returns / Credit Notes (reversals)	Week 4
Slice D	Reports + GST summaries (registers, HSN, stock summary, outstanding)	Week 5
Slice E	Desktop (Electron) packaging + printing integration	Week 6
6. Trae Prompt Pack (execute in order)
Important: Run tests after every prompt. If Trae generates code, immediately ask it to: “Run tests, fix failures, apply formatting, update OpenAPI, and summarize changes.”
6.1 Backend scaffolding
PROMPT A1 — Spring Boot skeleton
Create a Spring Boot 3 (Java 21) project in /backend with: Web, Validation, Security, JPA, Flyway, Actuator, OpenAPI.
Use PostgreSQL. Add Dockerfile and docker-compose for local postgres.
Create package structure: controller/service/repository/domain/dto/config.
Add /health endpoint.

PROMPT A2 — Security baseline
Implement JWT auth with roles. Endpoints: /auth/login, /auth/refresh (optional).
Seed default admin via Flyway for local dev.
Add method security annotations.

PROMPT A3 — Quality gates
Add Spotless (or Checkstyle) + unit tests + Testcontainers integration test base.
Configure GitHub Actions: build, test, lint.

6.2 Masters module
PROMPT B1 — Masters APIs
Implement master data APIs: Party, Item, TaxSlab, Warehouse.
Include entity, DTOs, repository, service, controller, validations, paging/sorting, search by name/GSTIN/HSN.
Create Flyway migrations. Add integration tests.

PROMPT B2 — Constraints & indexes
Add DB constraints and indexes:
- unique GSTIN per tenant/branch as applicable
- unique item code per tenant
- indexes on (tenantId, name), (gstin), (hsn)

6.3 Sales Invoice (core slice)
PROMPT C1 — SalesInvoice domain
Implement SalesInvoice header+lines.
Fields: invoiceNo, invoiceDate, partyId, placeOfSupplyStateCode, warehouseId,
lines: itemId, qty, rate, discount, taxSlabId. Compute totals: taxable, cgst, sgst, igst, grand.
Use BigDecimal. Validations: qty>0, rate>=0.

PROMPT C2 — Posting engine (Stock + GL)
On invoice creation, post:
- StockLedgerEntry OUT per line
- GLTransaction with GLLine entries:
  Dr AccountsReceivable (party)
  Cr SalesRevenue
  Cr OutputCGST, OutputSGST, OutputIGST (as applicable)
Ensure debits==credits.
Ledgers are append-only. Do not update balances; derive via queries or materialized summary.

PROMPT C3 — GST tax logic
If place of supply state == seller state → CGST+SGST
Else → IGST
Add unit tests for both paths.

PROMPT C4 — Invoice numbering
Implement FY + branch + series + running number.
Make generation concurrency-safe in PostgreSQL (sequence or locked counter table).

6.4 Purchase Invoice
PROMPT D1 — PurchaseInvoice + posting
Implement PurchaseInvoice similar to SalesInvoice.
Post:
- StockLedgerEntry IN per line
- GL: Dr Purchases/Inventory, Dr InputCGST/SGST/IGST, Cr AccountsPayable (vendor)
Add tests ensuring GL balances and stock impact.

6.5 Returns / credit notes
PROMPT E1 — Sales return (reversal)
Implement SalesReturn referencing original SalesInvoice.
Reverse stock (IN) and reverse GL postings.
Keep audit trail links to original doc.
Add tests.

6.6 Reports
PROMPT F1 — Stock summary
Create /reports/stock-summary?warehouseId=&asOfDate=
Compute from StockLedgerEntry: sum(qtyIn-qtyOut) grouped by item and warehouse.

PROMPT F2 — Party outstanding
Compute outstanding by party from GL as of date.

PROMPT F3 — GST registers
Create Sales register + Purchase register + HSN summary. Provide JSON + CSV export.

6.7 Web UI (Next.js)
PROMPT W1 — Web UI skeleton
Create Next.js app in /web with JWT auth, route guards, API client.
Pages: Parties, Items, Sales Invoice create/edit, Invoice list.
Add form validation and error handling.

PROMPT W2 — Invoice printing
Add invoice print view (A4) with tax breakdown and QR placeholder.

6.8 Desktop (Electron)
PROMPT X1 — Electron wrapper
Create Electron app in /desktop that loads the /web build or localhost dev server.
Add settings for API base URL, native printing integration, and packaging scripts.

7. Definition of Done (for every feature)
    • API implemented + OpenAPI updated
    • Flyway migrations included (no manual DB changes)
    • Unit tests + integration tests added and passing
    • Permissions enforced (RBAC)
    • Validation + error responses included
    • Audit fields populated and visible in DB
    • Docs updated (this file + API contracts if needed)