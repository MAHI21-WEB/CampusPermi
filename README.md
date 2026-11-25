# Campus Permission Management System (CPMS)

**Course:** UCS553 – Enterprise Web Application  
**Institution:** Thapar Institute of Engineering & Technology  
**Tech Stack:** Java 17 · JavaFX 17 · FXML · CSS · MySQL 8 · JDBC · Maven · JUnit 5

CPMS is a layered JavaFX desktop application that digitises club and society permission requests through a strictly sequential, four-role approval workflow:

1. **Club/Society Representative** (`CLUB_REP`) — Creates, saves as draft, submits, and tracks permission requests, uploads supporting documents, and exports serialized backups.
2. **Faculty Advisor** (`FACULTY_ADVISOR`) — Reviews requests for assigned societies (1st approval level).
3. **DOSA Office Representative** (`DOSA_REP`) — Reviews faculty-approved requests (2nd approval level).
4. **Associate Dean** (`ASSOCIATE_DEAN`) — Provides final approval or rejection (3rd approval level).

---

## Prerequisites

- **JDK:** 17+ (`Temurin-17.0.16` verified)
- **Maven:** 3.9+ (`Apache Maven 3.9.11` verified)
- **MySQL:** 8.0+

---

## Database Setup

1. Ensure your MySQL server is running (e.g., `net start MySQL80` on Windows).
2. Create the schema and seed sample data:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database\schema.sql
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database\sample_data.sql
   ```
3. Copy `src/main/resources/config/application.properties.example` to `src/main/resources/config/application.properties` and enter your local MySQL username and password:
   ```properties
   db.url=jdbc:mysql://localhost:3306/campus_permission_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.username=root
   db.password=your_mysql_password
   app.uploads.dir=uploads
   app.backups.dir=backups
   ```
   *(Do not commit `application.properties` to version control; it is listed in `.gitignore`.)*

---

## Sample Test Accounts

All seeded accounts in `database/sample_data.sql` use the test password **`Password@123`** (stored as BCrypt hashes):

| Role | Name | Email |
|---|---|---|
| Club/Society Representative (CCS) | Aarav Singh | `aarav.ccs@thapar.edu` |
| Club/Society Representative (ACM) | Priya Nair | `priya.acm@thapar.edu` |
| Club/Society Representative (LitSoc) | Kabir Mehta | `kabir.lit@thapar.edu` |
| Faculty Advisor (CCS & ACM) | Dr. Rajesh Sharma | `rajesh.sharma@thapar.edu` |
| Faculty Advisor (LitSoc) | Dr. Meera Verma | `meera.verma@thapar.edu` |
| DOSA Office Representative | Anil Kapoor (DOSA) | `dosa.office@thapar.edu` |
| Associate Dean | Prof. Sunita Malhotra | `assoc.dean@thapar.edu` |

---

## Build, Test & Run

```powershell
# Compile and run unit tests
mvn test

# Launch the JavaFX desktop application
mvn javafx:run
```

---

## Project Documentation

- [Architecture Overview](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [UCS553 Report Notes](docs/report-notes.md)
- [Diagrams](docs/diagrams/) (Architecture, Use Case, Class, Activity, and ER diagrams)
