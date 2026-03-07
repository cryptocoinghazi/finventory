# Finventory Development Tracker

This file tracks the development progress of the Finventory application.
Based on `docs/SPEC.MD`.

## Environment Setup
- [x] Create project directories (backend, web, desktop, docs)
- [x] Move SPEC.MD to docs/
- [x] **Local Environment Setup**
    - [x] Create `tools/` and setup scripts
    - [x] Document setup in `docs/LOCAL_SETUP.md`
    - [x] Run `scripts/setup_tools.ps1` (Installed: Java 21, Maven 3.9, Node 20, PostgreSQL 15)
    - [x] Verify environment activation
    - [x] Run `scripts/start_db.ps1` (Start Database)
    - [x] Create `scripts/start_all.ps1` (One-click startup for DB + Backend)

## 6.1 Backend Scaffolding
- [x] **PROMPT A1: Spring Boot Skeleton**
    - [x] Create Spring Boot 3 project
    - [x] Configure PostgreSQL, JPA, Flyway
    - [x] Add Docker support
    - [x] Create package structure
    - [x] Add /health endpoint
- [x] **PROMPT A2: Security Baseline**
    - [x] Implement JWT Auth
    - [x] Add Roles
    - [x] Seed default admin
- [x] **PROMPT A3: Quality Gates**
    - [x] Add Spotless/Checkstyle
    - [x] Add Unit Tests base
    - [x] Add Testcontainers base
    - [x] Configure CI (GitHub Actions)

## 6.2 Masters Module (Slice A)
- [x] **PROMPT B1: Masters APIs**
    - [x] Party API (Entity, DTO, Repo, Service, Controller)
    - [x] Item API
    - [x] TaxSlab API
    - [x] Warehouse API
    - [x] Flyway migrations
    - [x] Integration tests
- [ ] **PROMPT B2: Constraints & Indexes**
    - [ ] Add DB constraints (unique GSTIN, Item Code)
    - [ ] Add Indexes

## 6.3 Sales Invoice (Slice A)
- [ ] **PROMPT C1: SalesInvoice Domain**
    - [ ] SalesInvoice Header & Lines
    - [ ] Calculations (Taxable, Taxes, Grand Total)
- [ ] **PROMPT C2: Posting Engine**
    - [ ] StockLedgerEntry (OUT)
    - [ ] GLTransaction & GLLine (Dr AR, Cr Sales, Cr Output Tax)
- [ ] **PROMPT C3: GST Tax Logic**
    - [ ] Inter-state vs Intra-state logic
    - [ ] Unit tests
- [ ] **PROMPT C4: Invoice Numbering**
    - [ ] Concurrency-safe numbering (FY + Branch + Series)

## 6.4 Purchase Invoice (Slice B)
- [ ] **PROMPT D1: PurchaseInvoice + Posting**
    - [ ] PurchaseInvoice Domain
    - [ ] StockLedgerEntry (IN)
    - [ ] GL Posting (Dr Purchase/Inventory, Dr Input Tax, Cr AP)

## 6.5 Returns / Credit Notes (Slice C)
- [ ] **PROMPT E1: Sales Return**
    - [ ] SalesReturn Domain
    - [ ] Reversal Postings
    - [ ] Audit trail

## 6.6 Reports (Slice D)
- [ ] **PROMPT F1: Stock Summary**
    - [ ] API endpoint
- [ ] **PROMPT F2: Party Outstanding**
    - [ ] API endpoint
- [ ] **PROMPT F3: GST Registers**
    - [ ] Sales Register
    - [ ] Purchase Register
    - [ ] HSN Summary

## 6.7 Web UI (Slice A/B/C/D/E)
- [ ] **PROMPT W1: Web UI Skeleton**
    - [ ] Next.js setup
    - [ ] Auth & Route Guards
    - [ ] Basic Pages (Parties, Items, Invoice List)
- [ ] **PROMPT W2: Invoice Printing**
    - [ ] Print View (A4)

## 6.8 Desktop (Slice E)
- [ ] **PROMPT X1: Electron Wrapper**
    - [ ] Electron setup
    - [ ] Packaging

## Definition of Done (Checklist for each feature)
- [ ] API implemented + OpenAPI updated
- [ ] Flyway migrations included
- [ ] Unit tests + integration tests passing
- [ ] Permissions enforced
- [ ] Validation + error responses
- [ ] Audit fields populated
