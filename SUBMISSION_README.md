# CPMS Handoff & Pre-Submission Checklist

This guide lists everything that is **already completed** in the repository and **what remains to be done** before final UCS553 mini-project submission.

---

## 1. Current Status (Already Completed)

- **Application Code (`src/main/java`):** Complete 4-layer JavaFX + JDBC implementation (models, enums, custom exceptions, DAO interfaces & JDBC implementations, services, controllers, and utilities).
- **UI Views (`src/main/resources`):** All 7 FXML screens (`login.fxml`, `representative-dashboard.fxml`, `faculty-dashboard.fxml`, `dosa-dashboard.fxml`, `dean-dashboard.fxml`, `request-form.fxml`, `request-details.fxml`) and `css/style.css`.
- **Database Scripts (`database/`):** `schema.sql` (6 tables, FKs, constraints, indexes) and `sample_data.sql` (9 permission categories, 3 societies, 7 sample users across all 4 roles, sample requests & approval history).
- **Unit Tests (`src/test/java`):** 19 JUnit 5 tests (`AuthServiceTest`, `RequestServiceTest`, `ApprovalServiceTest`, `BackupAndAttachmentTest`) — all passing (`mvn test`) using in-memory stubs/proxies.
- **Documentation & Diagram Sources (`docs/`):** `architecture.md`, `database-design.md`, `report-notes.md`, and 5 Mermaid diagram files in `docs/diagrams/`.

---

## 2. What Is Left Before Submission

### Step 1: Set Up Local MySQL & Configuration
*(Not yet run on this machine because the MySQL service was stopped during development.)*

1. Start the **MySQL80** service (Windows Services or elevated PowerShell: `net start MySQL80`).
2. Load the database schema and sample data:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database\schema.sql
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database\sample_data.sql
   ```
3. Copy `src/main/resources/config/application.properties.example` to `src/main/resources/config/application.properties` and fill in your local MySQL username and password.

---

### Step 2: Live End-to-End Testing & Screenshot Capture
Launch the application:
```powershell
mvn javafx:run
```

Test each scenario below with a live MySQL database and **capture screenshots** for Chapter 4 of the report:

- **Login & Error Handling:**
  - Log in with invalid password → capture error banner screenshot.
  - Log in as each of the 4 roles (password for all seeded accounts: `Password@123`):
    - Club Rep: `aarav.ccs@thapar.edu`
    - Faculty Advisor: `rajesh.sharma@thapar.edu`
    - DOSA Rep: `dosa.office@thapar.edu`
    - Associate Dean: `assoc.dean@thapar.edu`
- **Request Creation & Attachment:**
  - As Club Rep, click `+ New Request`, select a category, attach a sample PDF/TXT file, and submit → capture form & dashboard screenshots.
  - Click `Export Backup` on the Representative Dashboard to verify `.ser` export.
- **Multi-Level Approval Workflow:**
  - Approve the submitted request sequentially as **Faculty Advisor** → **DOSA** → **Associate Dean** (capture each dashboard + the **Request Details** screen showing the Approval History table, Activity Timeline, and Decision panel).
  - Test **Reject** (with remarks) on another request and verify the Representative sees the rejection reason and that the request cannot be approved further.

---

### Step 3: Export Diagrams for the Report
Render the 5 Mermaid diagram files in `docs/diagrams/` to PNG/SVG (using Mermaid Live Editor at https://mermaid.live or a VS Code Mermaid extension):
1. `docs/diagrams/architecture-diagram.md` → Chapter 2.1 (System Architecture)
2. `docs/diagrams/use-case-diagram.md` → Chapter 2.2 (Use Case Diagram)
3. `docs/diagrams/class-diagram.md` → Chapter 2.3 (Class Diagram)
4. `docs/diagrams/activity-diagram.md` → Chapter 2.4 (Activity Diagram)
5. `docs/diagrams/er-diagram.md` → Chapter 2.5 (ER Diagram)

---

### Step 4: Compile the Final UCS553 Mini-Project Report
Assemble the Word/PDF report following the exact UCS553 format:
1. **Cover Page:** Add student names, roll numbers, department, faculty guide name, and submission date.
2. **Abstract & Table of Contents:** Summarize the problem, solution, and tech stack.
3. **Chapter 1 (Introduction):** Problem statement, objectives, and scope (4 roles + 9 categories).
4. **Chapter 2 (System Design):** Paste content from `docs/architecture.md` and `docs/database-design.md`, and insert the 5 exported diagrams.
5. **Chapter 3 (Implementation):** Use `docs/report-notes.md` (especially the Chapter 3 Code Snippet Reference Index) to include snippets for authentication, JDBC connectivity, request insertion, approval transactions, Collections (`ArrayList`, `LinkedList`, `HashSet`, `HashMap`, `Iterator`, `Comparator`), custom exceptions, and file I/O/serialization.
6. **Chapter 4 (Results & Screenshots):** Insert the screenshots captured in Step 2.
7. **References:** Cite Oracle Java 17, OpenJFX 17, MySQL 8.0, JDBC, and Maven documentation.
