# Database Design — Campus Permission Management System (CPMS)

## Overview

CPMS uses a relational MySQL database named `campus_permission_db` (`InnoDB`, `utf8mb4`). The schema models users, registered college societies, database-driven permission categories, permission requests, an immutable multi-level approval history, and supporting attachment metadata.

## Tables and Relationships

| Table | Primary Key | Foreign Keys | Purpose |
|---|---|---|---|
| `Users` | `user_id` | `society_id` → `Societies(society_id)` | Stores application users across all 4 roles (`CLUB_REP`, `FACULTY_ADVISOR`, `DOSA_REP`, `ASSOCIATE_DEAN`) with BCrypt password hashes. |
| `Societies` | `society_id` | `faculty_advisor_id` → `Users(user_id)` | Stores registered clubs/societies and their assigned faculty advisor (1 advisor can manage multiple societies). |
| `Permission_Types` | `type_id` | — | Database-managed permission categories (9 seeded categories, extensible via `is_active`). |
| `Requests` | `request_id` | `society_id` → `Societies(society_id)`, `submitted_by` → `Users(user_id)`, `type_id` → `Permission_Types(type_id)` | Stores permission requests, optional event date/time, and current workflow `status`. |
| `Approvals` | `approval_id` | `request_id` → `Requests(request_id)`, `approver_id` → `Users(user_id)` | Append-only audit log of every `FACULTY`, `DOSA`, and `ASSOCIATE_DEAN` decision with remarks and timestamp. |
| `Attachments` | `attachment_id` | `request_id` → `Requests(request_id)` | Stores metadata and local filesystem paths for supporting documents. |

## Integrity & Concurrency Rules

- **Check Constraints:** Enforce valid `role`, `status`, `approval_level`, and `decision` values at the database level in addition to service-layer validation.
- **Indexes:** Added on `Users(role, society_id)`, `Societies(faculty_advisor_id)`, `Requests(society_id, status)`, `Requests(status)`, `Requests(submitted_by)`, `Approvals(request_id, approver_id)`, and `Attachments(request_id)`.
- **Transactions:** Approval actions insert into `Approvals` and update `Requests.status` inside a single JDBC transaction (`commit` / `rollback`).
