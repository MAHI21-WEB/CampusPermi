# CPMS Report Notes (UCS553)

This file tracks implemented modules, classes, curriculum concepts, and evidence for the final UCS553 mini-project report.

## Phase 1: Environment and Project Setup

- **Module Description:** Maven project initialization, JavaFX runtime configuration, and base application entry point.
- **Classes Involved:** `com.campus.permission.Main`
- **Important Methods:** `Main.start(Stage)`, `Main.main(String[])`
- **UCS553 Concepts Demonstrated:**
  - Unit 1: Java packages (`com.campus.permission`), Maven build & dependency management (`pom.xml`), inheritance (`Main extends Application`).
  - Unit 3: Logging (`java.util.logging.Logger`), properties configuration template (`application.properties.example`).
  - Unit 5: JavaFX application lifecycle (`Application`, `Stage`, `Scene`, `FXMLLoader`, `BorderPane`, `VBox`, `Label`, CSS styling).
- **Screenshots to Capture:** Initial CPMS launch window.

## Phase 2: Database Design

- **Module Description:** Relational MySQL schema (`campus_permission_db`) with 6 tables, foreign keys, check constraints, indexes, and sample seed data for all 4 roles and 9 permission categories.
- **Files Involved:** `database/schema.sql`, `database/sample_data.sql`, `docs/database-design.md`
- **UCS553 Concepts Demonstrated:**
  - Unit 4: Relational database design, primary/foreign keys, constraints, indexes, and MySQL persistence preparation for JDBC/DAO.

## Phase 3: Models, Enums, and DAO Interfaces

- **Module Description:** Domain model classes, workflow enums, and typed DAO interfaces defining the persistence contract.
- **Classes & Interfaces Involved:**
  - Enums: `UserRole`, `RequestStatus`, `ApprovalLevel`, `Decision`
  - Models: `User`, `Society`, `PermissionType`, `PermissionRequest`, `Approval`, `Attachment`
  - DAO Interfaces: `BaseDAO<T>`, `UserDAO`, `SocietyDAO`, `PermissionTypeDAO`, `RequestDAO`, `ApprovalDAO`, `AttachmentDAO`
- **UCS553 Concepts Demonstrated:**
  - Unit 1: Encapsulation (private fields with getters/setters), interfaces (`BaseDAO`, sub-DAOs), packages (`enums`, `model`, `dao`), `Serializable` marker interface.
  - Unit 2: Generics (`BaseDAO<T>`, `Optional<T>`, `List<T>`), `Objects.equals`/`Objects.hash`, `LocalDate`, `LocalTime`, `LocalDateTime`.

## Phase 4: Exceptions, Configuration, and JDBC DAO Layer

- **Module Description:** Custom exception hierarchy, externalized properties configuration (`AppConfig`), JDBC connection factory (`DBConnection`), and concrete JDBC DAO implementations.
- **Classes Involved:**
  - Exceptions: `DatabaseException`, `ValidationException`, `UnauthorizedActionException`, `ResourceNotFoundException`
  - Config & Utility: `AppConfig`, `DBConnection`
  - DAO Implementations: `UserDAOImpl`, `SocietyDAOImpl`, `PermissionTypeDAOImpl`, `RequestDAOImpl`, `ApprovalDAOImpl`, `AttachmentDAOImpl`
- **UCS553 Concepts Demonstrated:**
  - Unit 2: Custom exception hierarchy (`RuntimeException` subclasses), `Optional<T>`, `ArrayList<T>`.
  - Unit 3: Properties file loading (`java.util.Properties`), `java.util.logging.Logger`, `try-with-resources`.
  - Unit 4: JDBC (`DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`, `Statement.RETURN_GENERATED_KEYS`), SQL injection prevention, and DAO pattern.

## Phase 5: Authentication & Role-Based Navigation

- **Module Description:** BCrypt password verification, input validation, session management, login UI, and role-based dashboard routing.
- **Classes & Views Involved:**
  - Utilities: `PasswordUtil`, `ValidationUtil`, `SessionContext`, `SceneNavigator`
  - Service: `AuthService`
  - Controllers: `LoginController`, `DashboardController`
  - FXML & CSS: `login.fxml`, `representative-dashboard.fxml`, `faculty-dashboard.fxml`, `dosa-dashboard.fxml`, `dean-dashboard.fxml`, `style.css`
  - Tests: `AuthServiceTest`
- **UCS553 Concepts Demonstrated:**
  - Unit 1: Service/Controller separation, static utility helpers.
  - Unit 2: `ValidationException`, `UnauthorizedActionException`, `Optional` chaining (`orElseThrow`).
  - Unit 5: JavaFX `TextField`, `PasswordField`, `Button`, `Label`, `FXMLLoader`, event handling (`onAction`).
- **Screenshots to Capture:** Login screen, invalid login error banner, and role-specific dashboard headers.

## Phase 6: Request Management & Representative Dashboard

- **Module Description:** Request creation, draft saving, submission to faculty advisor, society-scoped access control, and representative dashboard with KPI cards and request table.
- **Classes & Views Involved:**
  - Services: `RequestService`, `DashboardService`
  - Controllers: `DashboardController`, `RequestController`
  - Views: `representative-dashboard.fxml`, `request-form.fxml`
  - Tests: `RequestServiceTest`
- **UCS553 Concepts Demonstrated:**
  - Unit 2: `HashMap<RequestStatus, Long>` for status counts, `ArrayList<PermissionRequest>`, `Comparator` sorting by creation date, `ValidationException`, `UnauthorizedActionException`, `ResourceNotFoundException`.
  - Unit 5: JavaFX `TableView`, `TableColumn`, `ComboBox<PermissionType>`, `DatePicker`, `TextArea`, `Alert`, selection listeners.
- **Screenshots to Capture:** Representative dashboard with KPIs & request table, new permission request form.

## Phase 7: Approval Workflow & Authority Dashboards

- **Module Description:** Centralized multi-level approval workflow (`FACULTY` → `DOSA` → `ASSOCIATE_DEAN`), atomic JDBC transaction management, authority dashboards, and reusable request details & approval timeline screen.
- **Classes & Views Involved:**
  - Service: `ApprovalService`
  - Controllers: `ApprovalController`, `RequestDetailsController`, updated `DashboardController`
  - Views: `faculty-dashboard.fxml`, `dosa-dashboard.fxml`, `dean-dashboard.fxml`, `request-details.fxml`, updated `representative-dashboard.fxml`
  - Utility: `SceneNavigator`
  - Tests: `ApprovalServiceTest`
- **UCS553 Concepts Demonstrated:**
  - Unit 2: `HashSet<Integer>` for deduplicating reviewed request IDs, `Comparator` sorting, `UnauthorizedActionException`, `ValidationException`.
  - Unit 4: JDBC transaction management (`setAutoCommit(false)`, `commit()`, `rollback()`).
  - Unit 5: JavaFX `TabPane`, `TableView`, `TableRow` double-click handlers, `GridPane`, `ScrollPane`, `Alert.AlertType.CONFIRMATION`.
- **Screenshots to Capture:** Faculty/DOSA/Associate Dean dashboards, Request Details screen with approval history and decision panel.

## Phase 8: Advanced UCS553 Curriculum Features

- **Module Description:** Supporting document file I/O (`AttachmentService`), serialized request backup/restore & character-stream text export (`BackupService`), and `LinkedList` + `Iterator` activity timeline (`DashboardService`).
- **Classes Involved:**
  - Services: `AttachmentService`, `BackupService`, updated `DashboardService`
  - Controllers & Views: updated `RequestController`, `RequestDetailsController`, `DashboardController`, `request-form.fxml`, `request-details.fxml`, `representative-dashboard.fxml`
  - Tests: `BackupAndAttachmentTest`
- **UCS553 Concepts Demonstrated:**
  - Unit 2: `LinkedList<String>`, `Iterator<String>`.
  - Unit 3: Buffered byte streams (`BufferedInputStream`, `BufferedOutputStream`), character streams (`BufferedWriter`), object serialization/deserialization (`ObjectOutputStream`, `ObjectInputStream`).
  - Unit 5: JavaFX `FileChooser`, `ListView<Attachment>`.
- **Screenshots to Capture:** Request form with attached file, Request details with attachments list and activity timeline, Export Backup dialog.

## Phases 9 & 10: Documentation, Diagrams & Final Verification

- **Files Added:**
  - `README.md` (setup, MySQL initialization, sample test accounts, build/run instructions)
  - `docs/architecture.md`
  - `docs/diagrams/architecture-diagram.md`
  - `docs/diagrams/use-case-diagram.md`
  - `docs/diagrams/class-diagram.md`
  - `docs/diagrams/activity-diagram.md`
  - `docs/diagrams/er-diagram.md`

### Chapter 3 Code Snippet Reference Index

- **Authentication (Section 3.8):** `AuthService.login(...)` & `PasswordUtil.verifyPassword(...)`
- **Database Connectivity:** `DBConnection.getConnection()` & `AppConfig`
- **Request Insertion:** `RequestDAOImpl.save(...)` (`PreparedStatement` + `RETURN_GENERATED_KEYS`)
- **Approval Processing & Transactions:** `ApprovalService.processDecision(...)` & `ApprovalService.executeInTransaction(...)`
- **Collection Framework:** `DashboardService` (`HashMap`, `ArrayList`, `LinkedList`, `Iterator`, `Comparator`), `ApprovalService` (`HashSet`)
- **Exception Handling:** `DatabaseException`, `ValidationException`, `UnauthorizedActionException`, `ResourceNotFoundException`
- **File I/O & Serialization:** `AttachmentService.copyWithByteStreams(...)`, `BackupService.exportRequestsBackup(...)` / `restoreRequestsBackup(...)`
