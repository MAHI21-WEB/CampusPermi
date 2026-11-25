-- ============================================================================
-- Campus Permission Management System (CPMS)
-- Sample Seed Data
-- Default test password for all sample accounts: Password@123
-- (Hashed with BCrypt cost 10)
-- ============================================================================

USE campus_permission_db;

-- Clear existing data in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE Attachments;
TRUNCATE TABLE Approvals;
TRUNCATE TABLE Requests;
TRUNCATE TABLE Permission_Types;
UPDATE Users SET society_id = NULL;
TRUNCATE TABLE Societies;
TRUNCATE TABLE Users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Permission_Types (9 categories)
-- ----------------------------------------------------------------------------
INSERT INTO Permission_Types (type_name, description, is_active) VALUES
('Event/Activity Permission', 'General permission for organizing workshops, hackathons, seminars, or cultural events.', TRUE),
('Venue/Infrastructure Booking', 'Booking of auditoriums, lecture halls, seminar rooms, or open-air grounds.', TRUE),
('Financial/Budget Approval', 'Approval for society budget allocation, reimbursements, or event sponsorship.', TRUE),
('Equipment/Technical Support', 'Request for audio-visual equipment, projectors, microphones, or IT support.', TRUE),
('Publicity and Marketing Permission', 'Approval for posters, banners, standees, social media campaigns, and campus publicity.', TRUE),
('External Speaker/Guest Permission', 'Permission to invite external speakers, industry experts, or dignitaries to campus.', TRUE),
('External Participation/Collaboration', 'Approval for inter-college participation, external collaborations, or off-campus travel.', TRUE),
('Food and Refreshment Permission', 'Permission for catering, refreshments, or food stalls during campus events.', TRUE),
('Other', 'Miscellaneous administrative or operational permissions not covered above.', TRUE);

-- ----------------------------------------------------------------------------
-- 2. Users (Faculty Advisors, DOSA Rep, Associate Dean first)
-- BCrypt hash corresponds to: Password@123
-- ----------------------------------------------------------------------------
INSERT INTO Users (name, email, password_hash, role, society_id) VALUES
('Dr. Rajesh Sharma', 'rajesh.sharma@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'FACULTY_ADVISOR', NULL),
('Dr. Meera Verma', 'meera.verma@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'FACULTY_ADVISOR', NULL),
('Anil Kapoor (DOSA)', 'dosa.office@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'DOSA_REP', NULL),
('Prof. Sunita Malhotra', 'assoc.dean@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'ASSOCIATE_DEAN', NULL);

-- ----------------------------------------------------------------------------
-- 3. Societies
-- Note: Dr. Rajesh Sharma (user_id = 1) advises two societies (1:N relationship).
-- ----------------------------------------------------------------------------
INSERT INTO Societies (society_name, description, faculty_advisor_id) VALUES
('Creative Computing Society', 'Technical society focused on software engineering, hackathons, and coding culture.', 1),
('ACM Student Chapter', 'Student chapter promoting computing research, competitive programming, and tech talks.', 1),
('Literary & Debating Society', 'Cultural society organizing debates, MUNs, and creative writing events.', 2);

-- ----------------------------------------------------------------------------
-- 4. Club/Society Representatives
-- ----------------------------------------------------------------------------
INSERT INTO Users (name, email, password_hash, role, society_id) VALUES
('Aarav Singh', 'aarav.ccs@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'CLUB_REP', 1),
('Priya Nair', 'priya.acm@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'CLUB_REP', 2),
('Kabir Mehta', 'kabir.lit@thapar.edu', '$2a$10$N/vLwfPMYwpdn56OSbIRs.4d1ZBT.CInx46wprCHS7M7rK76pFlpm', 'CLUB_REP', 3);

-- ----------------------------------------------------------------------------
-- 5. Sample Requests across workflow stages
-- ----------------------------------------------------------------------------
INSERT INTO Requests (society_id, submitted_by, type_id, title, description, event_date, start_time, end_time, status) VALUES
(1, 5, 1, 'HackTU 2026 Annual Hackathon', '24-hour inter-college hackathon with 300 expected participants.', '2026-12-10', '09:00:00', '18:00:00', 'PENDING_FACULTY'),
(1, 5, 2, 'Main Auditorium Booking for Tech Orientation', 'Booking Main Auditorium for first-year student orientation session.', '2026-12-03', '14:00:00', '17:00:00', 'PENDING_DOSA'),
(2, 6, 6, 'Guest Lecture on Distributed Systems', 'Inviting Principal Engineer from Google India for a 2-hour technical talk.', '2026-12-08', '11:00:00', '13:00:00', 'PENDING_ASSOCIATE_DEAN'),
(3, 7, 3, 'Thapar MUN Budget Approval', 'Estimated budget of INR 45,000 for logistics, certificates, and stationery.', '2026-12-15', '09:30:00', '17:30:00', 'APPROVED'),
(2, 6, 4, 'PA System Request for Coding Contest', 'Requesting 2 wireless microphones and projector setup in LP-101.', '2026-12-05', '16:00:00', '19:00:00', 'REJECTED');

-- ----------------------------------------------------------------------------
-- 6. Sample Approval History (consistent with Requests.status)
-- ----------------------------------------------------------------------------
-- Request 2 (PENDING_DOSA): Approved by Faculty Advisor (user 1)
INSERT INTO Approvals (request_id, approver_id, approval_level, decision, remarks) VALUES
(2, 1, 'FACULTY', 'APPROVED', 'Recommended. Orientation schedule aligns with academic calendar.');

-- Request 3 (PENDING_ASSOCIATE_DEAN): Approved by Faculty Advisor (user 1) and DOSA (user 3)
INSERT INTO Approvals (request_id, approver_id, approval_level, decision, remarks) VALUES
(3, 1, 'FACULTY', 'APPROVED', 'Guest profile verified and relevant to CSE curriculum.'),
(3, 3, 'DOSA', 'APPROVED', 'Venue availability and guest protocol verified.');

-- Request 4 (APPROVED): Approved at all three levels
INSERT INTO Approvals (request_id, approver_id, approval_level, decision, remarks) VALUES
(4, 2, 'FACULTY', 'APPROVED', 'Budget breakdown is reasonable and well-documented.'),
(4, 3, 'DOSA', 'APPROVED', 'Forwarded to Associate Dean for final financial sanction.'),
(4, 4, 'ASSOCIATE_DEAN', 'APPROVED', 'Final approval granted. Adhere to institute finance guidelines.');

-- Request 5 (REJECTED): Rejected by Faculty Advisor (user 1)
INSERT INTO Approvals (request_id, approver_id, approval_level, decision, remarks) VALUES
(5, 1, 'FACULTY', 'REJECTED', 'Clashes with mid-semester lab evaluations in LP block.');
