-- ============================================================================
-- Campus Permission Management System (CPMS)
-- Database Schema (MySQL 8.0+)
-- Course: UCS553 - Enterprise Web Application
-- ============================================================================

CREATE DATABASE IF NOT EXISTS campus_permission_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE campus_permission_db;

-- Drop tables in reverse dependency order for clean re-initialization
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Attachments;
DROP TABLE IF EXISTS Approvals;
DROP TABLE IF EXISTS Requests;
DROP TABLE IF EXISTS Permission_Types;
DROP TABLE IF EXISTS Societies;
DROP TABLE IF EXISTS Users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Users
-- Roles: CLUB_REP, FACULTY_ADVISOR, DOSA_REP, ASSOCIATE_DEAN
-- ----------------------------------------------------------------------------
CREATE TABLE Users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(40)  NOT NULL,
    society_id    INT          NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CLUB_REP', 'FACULTY_ADVISOR', 'DOSA_REP', 'ASSOCIATE_DEAN'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. Societies
-- One faculty advisor can be assigned to multiple societies.
-- ----------------------------------------------------------------------------
CREATE TABLE Societies (
    society_id         INT          NOT NULL AUTO_INCREMENT,
    society_name       VARCHAR(150) NOT NULL,
    description        TEXT         NULL,
    faculty_advisor_id INT          NOT NULL,
    CONSTRAINT pk_societies PRIMARY KEY (society_id),
    CONSTRAINT uq_societies_name UNIQUE (society_name),
    CONSTRAINT fk_societies_advisor FOREIGN KEY (faculty_advisor_id)
        REFERENCES Users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Add foreign key from Users.society_id -> Societies.society_id
ALTER TABLE Users
    ADD CONSTRAINT fk_users_society FOREIGN KEY (society_id)
        REFERENCES Societies (society_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 3. Permission_Types
-- Database-driven permission categories.
-- ----------------------------------------------------------------------------
CREATE TABLE Permission_Types (
    type_id     INT          NOT NULL AUTO_INCREMENT,
    type_name   VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_permission_types PRIMARY KEY (type_id),
    CONSTRAINT uq_permission_types_name UNIQUE (type_name)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. Requests
-- Statuses: DRAFT, PENDING_FACULTY, PENDING_DOSA, PENDING_ASSOCIATE_DEAN, APPROVED, REJECTED
-- ----------------------------------------------------------------------------
CREATE TABLE Requests (
    request_id   INT          NOT NULL AUTO_INCREMENT,
    society_id   INT          NOT NULL,
    submitted_by INT          NOT NULL,
    type_id      INT          NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    event_date   DATE         NULL,
    start_time   TIME         NULL,
    end_time     TIME         NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_requests_society FOREIGN KEY (society_id)
        REFERENCES Societies (society_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_requests_submitter FOREIGN KEY (submitted_by)
        REFERENCES Users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_requests_type FOREIGN KEY (type_id)
        REFERENCES Permission_Types (type_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_requests_status CHECK (
        status IN ('DRAFT', 'PENDING_FACULTY', 'PENDING_DOSA', 'PENDING_ASSOCIATE_DEAN', 'APPROVED', 'REJECTED')
    )
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 5. Approvals
-- Immutable audit trail of approval/rejection decisions.
-- ----------------------------------------------------------------------------
CREATE TABLE Approvals (
    approval_id    INT         NOT NULL AUTO_INCREMENT,
    request_id     INT         NOT NULL,
    approver_id    INT         NOT NULL,
    approval_level VARCHAR(40) NOT NULL,
    decision       VARCHAR(20) NOT NULL,
    remarks        TEXT        NULL,
    decision_date  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_approvals PRIMARY KEY (approval_id),
    CONSTRAINT fk_approvals_request FOREIGN KEY (request_id)
        REFERENCES Requests (request_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_approvals_approver FOREIGN KEY (approver_id)
        REFERENCES Users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_approvals_level CHECK (approval_level IN ('FACULTY', 'DOSA', 'ASSOCIATE_DEAN')),
    CONSTRAINT chk_approvals_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. Attachments
-- Metadata and filesystem paths for supporting documents.
-- ----------------------------------------------------------------------------
CREATE TABLE Attachments (
    attachment_id INT          NOT NULL AUTO_INCREMENT,
    request_id    INT          NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_attachments PRIMARY KEY (attachment_id),
    CONSTRAINT fk_attachments_request FOREIGN KEY (request_id)
        REFERENCES Requests (request_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Indexes for frequently queried columns
-- ----------------------------------------------------------------------------
CREATE INDEX idx_users_role ON Users (role);
CREATE INDEX idx_users_society ON Users (society_id);
CREATE INDEX idx_societies_advisor ON Societies (faculty_advisor_id);
CREATE INDEX idx_requests_society_status ON Requests (society_id, status);
CREATE INDEX idx_requests_status ON Requests (status);
CREATE INDEX idx_requests_submitted_by ON Requests (submitted_by);
CREATE INDEX idx_approvals_request ON Approvals (request_id);
CREATE INDEX idx_approvals_approver ON Approvals (approver_id);
CREATE INDEX idx_attachments_request ON Attachments (request_id);
