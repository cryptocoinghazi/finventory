# Finventory Project Tracker

## 📌 Development Workflow
This project follows a strict **Sequential Development Workflow**:
1.  **Backend & Database First**:
    -   Implement Entity, Repository, Service, Controller.
    -   Write and execute comprehensive Unit & Integration Tests.
    -   **Verify** all tests pass.
    -   Mark Backend task as **Completed**.
2.  **Frontend Implementation**:
    -   **Only** starts after Backend task is marked **Completed**.
    -   Implement UI components (Pages, Forms, Tables).
    -   Integrate with Backend APIs.
    -   Write and execute E2E/Component Tests.
    -   Mark Frontend task as **Completed**.

---

## 🚀 Feature Status

### 1. Environment & Scaffolding
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Project Setup** | ✅ Completed | N/A | ✅ Completed | N/A | Java 21, Spring Boot 3, Next.js 14 |
| **CI/CD** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | GitHub Actions configured |
| **Security (JWT)** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Auth Controller, JWT Filter |

### 2. Master Data Management (Slice A)
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Party API** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Customer/Vendor Management |
| **Item API** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Product/Service Management |
| **Warehouse API** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Multi-location Inventory |
| **Tax Slab API** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | GST Rates (0, 5, 12, 18, 28%) |
| **Validations** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | JSR-380, Global Exception Handler |

### 3. Sales Module (Slice A)
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Sales Invoice Domain** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Header & Lines, Calculations |
| **GST Tax Logic** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Intra-state (CGST/SGST) vs Inter-state (IGST) |
| **Stock Posting (OUT)** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Reduces Inventory |
| **GL Posting (Sales)** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Dr AR, Cr Sales, Cr Output Tax |
| **Invoice Numbering** | ⬜ Pending | ⬜ Pending | ⏳ Pending | ⏳ Pending | Custom Series (FY/Branch) |

### 4. Purchase Module (Slice B)
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Purchase Invoice Domain**| ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Vendor Bills |
| **Stock Posting (IN)** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Increases Inventory |
| **GL Posting (Purchase)** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Dr Purchase, Dr Input Tax, Cr AP |

### 5. Returns & Credit Notes (Slice C)
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Sales Return** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Credit Note |
| **Purchase Return** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Debit Note |

### 6. Reports & Dashboards (Slice D)
| Feature | Backend Status | Backend Tests | Frontend Status | Frontend Tests | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Stock Summary** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | In/Out/Balance |
| **Party Outstanding** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | Receivables/Payables |
| **GST Registers** | ✅ Completed | ✅ Verified | ⏳ Pending | ⏳ Pending | GSTR-1, GSTR-3B |

---

## 📉 Detailed History (Backend)
- [x] **Scaffolding**: Spring Boot 3, PostgreSQL, Flyway, Docker.
- [x] **Security**: JWT Authentication, Role-based Access (ADMIN/USER).
- [x] **Quality**: Checkstyle, Spotless, JUnit 5, Testcontainers.
- [x] **Masters**: CRUD for Party, Item, Warehouse, TaxSlab.
- [x] **Sales**: Invoice Creation, Tax Calculation, Stock Update, GL Posting.
- [x] **Purchase**: Invoice Creation, Stock IN, GL Debit (Purchase/Input Tax).
- [x] **GST Logic**: State-based tax splitting (Intra/Inter).
- [x] **Verification**: Full Integration Test Suite Passed (12/12 Tests) across all modules (Sales, Purchase, Returns, Reports).
- [x] **CI/CD**: Addressed GitHub Checkstyle build errors (WhitespaceAround).

## 📝 Next Steps
1.  **Implement Invoice Numbering** (Custom Series for Sales/Purchase) - *Pending Backend Task*.
2.  **Start Frontend Implementation** (Next.js 14).
    -   Setup Project Structure.
    -   Implement Authentication (Login/Register).
    -   Implement Dashboard.
