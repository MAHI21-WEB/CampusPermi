# System Architecture — Campus Permission Management System (CPMS)

CPMS follows a strict four-layer architecture separating JavaFX presentation, core Java business logic, JDBC persistence, and MySQL storage.

## Layers

1. **Presentation Layer (`com.campus.permission.controller`, `resources/fxml`, `resources/css`)**
   - `LoginController` (`login.fxml`)
   - `DashboardController` (`representative-dashboard.fxml`)
   - `RequestController` (`request-form.fxml`)
   - `ApprovalController` (`faculty-dashboard.fxml`, `dosa-dashboard.fxml`, `dean-dashboard.fxml`)
   - `RequestDetailsController` (`request-details.fxml`)
   - `SceneNavigator` centralizes FXML loading, CSS attachment, and role-based routing.
   - Controllers never execute SQL queries or duplicate approval transition rules.

2. **Service Layer (`com.campus.permission.service`)**
   - `AuthService`: Email/password validation, BCrypt verification (`PasswordUtil`), and `SessionContext` lifecycle.
   - `RequestService`: Request creation/validation, draft submission, active category lookup, and society-scoped read authorization.
   - `ApprovalService`: Centralized sequential workflow engine (`FACULTY` → `DOSA` → `ASSOCIATE_DEAN`), faculty-society ownership enforcement, and atomic JDBC transaction management (`commit` / `rollback`).
   - `DashboardService`: Status count aggregation (`HashMap`), recent request sorting (`Comparator`), and chronological activity timeline generation (`LinkedList` + `Iterator`).
   - `AttachmentService`: Buffered byte-stream file storage (`BufferedInputStream` / `BufferedOutputStream`) and metadata persistence.
   - `BackupService`: Binary object serialization/deserialization (`ObjectOutputStream` / `ObjectInputStream`) and character-stream summary export (`BufferedWriter`).

3. **Data Access Layer (`com.campus.permission.dao`, `com.campus.permission.dao.impl`)**
   - Generic `BaseDAO<T>` interface extended by `UserDAO`, `SocietyDAO`, `PermissionTypeDAO`, `RequestDAO`, `ApprovalDAO`, and `AttachmentDAO`.
   - JDBC implementations use `PreparedStatement`, `ResultSet` row mappers, `try-with-resources`, and `java.util.logging.Logger`, wrapping `SQLException` into `DatabaseException`.

4. **Database Layer (`database/schema.sql`)**
   - MySQL 8.0 (`campus_permission_db`) with 6 tables (`Users`, `Societies`, `Permission_Types`, `Requests`, `Approvals`, `Attachments`), foreign keys, check constraints, and indexes.
