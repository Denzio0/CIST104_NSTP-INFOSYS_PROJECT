-- ============================================================
-- NSTP INTEGRATED INFORMATION SYSTEM — COMPLETE DATABASE SCRIPT
-- Pamantasan ng Lungsod ng Maynila
-- College of Information Systems and Technology Management
-- Group Fifth Unit | Prof. Darwin C. Co | May 2026
-- ============================================================
-- Compatible with: XAMPP MySQL 8.0+ | phpMyAdmin import
-- Encoding: UTF-8 (utf8mb4)
-- ============================================================

-- Step 1: Create and select database
DROP DATABASE IF EXISTS nstp_infosystem;
CREATE DATABASE nstp_infosystem
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE nstp_infosystem;

-- ============================================================
-- TABLE 1: students
-- Central reference table for all NSTP student profiles.
-- All other student-data tables reference this via student_id (FK).
-- ============================================================
CREATE TABLE students (
    student_id      INT           NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(150)  NOT NULL,
    program         ENUM('CWTS','ROTC') NOT NULL,
    year_level      VARCHAR(20)   NOT NULL,
    section         VARCHAR(20)   NOT NULL,
    contact_no      VARCHAR(20)   DEFAULT NULL,
    email           VARCHAR(100)  DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_students PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Central student profile table. All other data tables reference this.';

-- Index: search by name and program
CREATE INDEX idx_students_program  ON students (program);
CREATE INDEX idx_students_name     ON students (full_name);

-- ============================================================
-- TABLE 2: users
-- System user accounts. Each user is linked to one student profile.
-- Relationship to students: one-to-one (one user per student).
-- ============================================================
CREATE TABLE users (
    user_id         INT           NOT NULL AUTO_INCREMENT,
    username        VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    role            ENUM('Admin','CWTS_Student','ROTC_Student','ROTC_Senior') NOT NULL,
    student_id      INT           DEFAULT NULL,
    is_active       TINYINT(1)    NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      DATETIME      DEFAULT NULL,
    CONSTRAINT pk_users            PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username   UNIQUE KEY (username),
    CONSTRAINT fk_users_student    FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='System login accounts. Admin accounts have student_id = NULL.';

CREATE INDEX idx_users_role     ON users (role);
CREATE INDEX idx_users_active   ON users (is_active);

-- ============================================================
-- TABLE 3: attendance
-- Stores one attendance record per student per session.
-- Relationship to students: many-to-one (many records per student).
-- ============================================================
CREATE TABLE attendance (
    record_id       INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    session_name    VARCHAR(100)  NOT NULL,
    session_date    DATE          NOT NULL,
    status          ENUM('Present','Absent','Late','Excused') NOT NULL DEFAULT 'Absent',
    remarks         TEXT          DEFAULT NULL,
    logged_by       INT           DEFAULT NULL,
    logged_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_attendance        PRIMARY KEY (record_id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_attendance_admin  FOREIGN KEY (logged_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Attendance records per student per session. Supports Present/Absent/Late/Excused.';

-- Composite index: prevents duplicate session entries for same student
CREATE INDEX idx_attendance_student    ON attendance (student_id);
CREATE INDEX idx_attendance_session    ON attendance (session_name, session_date);
CREATE UNIQUE INDEX uq_attendance_entry ON attendance (student_id, session_name, session_date);

-- ============================================================
-- TABLE 4: engagement
-- CWTS community service activity records.
-- Relationship to students: many-to-one.
-- ============================================================
CREATE TABLE engagement (
    activity_id     INT             NOT NULL AUTO_INCREMENT,
    student_id      INT             NOT NULL,
    activity_name   VARCHAR(150)    NOT NULL,
    activity_date   DATE            NOT NULL,
    location        VARCHAR(150)    DEFAULT NULL,
    category        VARCHAR(80)     DEFAULT NULL,
    hours_rendered  DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    verified        TINYINT(1)      NOT NULL DEFAULT 0,
    notes           TEXT            DEFAULT NULL,
    logged_by       INT             DEFAULT NULL,
    logged_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_engagement        PRIMARY KEY (activity_id),
    CONSTRAINT fk_engagement_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_engagement_admin  FOREIGN KEY (logged_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_engagement_hours CHECK (hours_rendered >= 0 AND hours_rendered <= 24)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='CWTS community service activity logs. hours_rendered tracks progress toward required minimum.';

CREATE INDEX idx_engagement_student  ON engagement (student_id);
CREATE INDEX idx_engagement_verified ON engagement (verified);

-- ============================================================
-- TABLE 5: performance
-- ROTC drill and performance rating entries.
-- Relationship to students: many-to-one.
-- ============================================================
CREATE TABLE performance (
    entry_id        INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    drill_name      VARCHAR(100)  NOT NULL,
    drill_date      DATE          NOT NULL,
    rating          ENUM('Excellent','Proficient','Satisfactory',
                         'Needs_Improvement','Unsatisfactory') NOT NULL,
    remarks         TEXT          DEFAULT NULL,
    officer_name    VARCHAR(100)  DEFAULT NULL,
    logged_by       INT           DEFAULT NULL,
    logged_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_performance        PRIMARY KEY (entry_id),
    CONSTRAINT fk_performance_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_performance_officer FOREIGN KEY (logged_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='ROTC drill performance ratings. Used for at-risk detection and average score computation.';

CREATE INDEX idx_performance_student ON performance (student_id);
CREATE INDEX idx_performance_date    ON performance (drill_date);

-- ============================================================
-- TABLE 6: points
-- Points transaction ledger for both CWTS and ROTC students.
-- Relationship to students: many-to-one.
-- ============================================================
CREATE TABLE points (
    transaction_id  INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    amount          INT           NOT NULL,
    type            ENUM('award','deduct') NOT NULL,
    reason          TEXT          NOT NULL,
    transaction_date DATE         NOT NULL,
    admin_id        INT           DEFAULT NULL,
    logged_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_points           PRIMARY KEY (transaction_id),
    CONSTRAINT fk_points_student   FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_points_admin     FOREIGN KEY (admin_id)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_points_amount   CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Points transaction ledger. Use SUM with CASE WHEN to compute net balance per student.';

CREATE INDEX idx_points_student ON points (student_id);
CREATE INDEX idx_points_type    ON points (type);

-- ============================================================
-- TABLE 7: demerits
-- Demerit entries for ROTC students only.
-- Relationship to students: many-to-one.
-- ============================================================
CREATE TABLE demerits (
    demerit_id      INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    reason          VARCHAR(200)  NOT NULL,
    severity        ENUM('Minor','Moderate','Major') NOT NULL DEFAULT 'Minor',
    notes           TEXT          DEFAULT NULL,
    officer_name    VARCHAR(100)  DEFAULT NULL,
    demerit_date    DATE          NOT NULL,
    status          ENUM('Active','Appealed','Dismissed') NOT NULL DEFAULT 'Active',
    logged_by       INT           DEFAULT NULL,
    logged_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_demerits         PRIMARY KEY (demerit_id),
    CONSTRAINT fk_demerits_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_demerits_officer FOREIGN KEY (logged_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='ROTC demerit records. Severity weights: Minor=1, Moderate=2, Major=3 for weighted scoring.';

CREATE INDEX idx_demerits_student ON demerits (student_id);
CREATE INDEX idx_demerits_status  ON demerits (status);

-- ============================================================
-- TABLE 8: announcements
-- System announcements posted by administrators.
-- Audience filtering restricts visibility to CWTS, ROTC, or All.
-- ============================================================
CREATE TABLE announcements (
    announcement_id INT           NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)  NOT NULL,
    body            TEXT          NOT NULL,
    audience        ENUM('All','CWTS','ROTC') NOT NULL DEFAULT 'All',
    priority        ENUM('Normal','Important','Urgent') NOT NULL DEFAULT 'Normal',
    posted_date     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posted_by       INT           DEFAULT NULL,
    is_pinned       TINYINT(1)    NOT NULL DEFAULT 0,
    CONSTRAINT pk_announcements      PRIMARY KEY (announcement_id),
    CONSTRAINT fk_announcements_user FOREIGN KEY (posted_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Admin-posted announcements with audience filtering. Pinned announcements appear first.';

CREATE INDEX idx_announcements_audience ON announcements (audience);
CREATE INDEX idx_announcements_pinned   ON announcements (is_pinned, posted_date DESC);

-- ============================================================
-- TABLE 9: read_announcements
-- Junction table tracking which students have read which announcements.
-- Relationship: many-to-many between students and announcements.
-- ============================================================
CREATE TABLE read_announcements (
    id              INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    announcement_id INT           NOT NULL,
    read_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_read_announcements   PRIMARY KEY (id),
    CONSTRAINT uq_read_announcement    UNIQUE KEY (student_id, announcement_id),
    CONSTRAINT fk_read_ann_student     FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_read_ann_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcements (announcement_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Tracks which students have read each announcement. Used for NEW badge logic.';

-- ============================================================
-- TABLE 10: notifications
-- Auto-generated system alerts for individual students.
-- Relationship to students: many-to-one.
-- ============================================================
CREATE TABLE notifications (
    notif_id        INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    type            VARCHAR(80)   NOT NULL,
    message         TEXT          NOT NULL,
    created_date    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read         TINYINT(1)    NOT NULL DEFAULT 0,
    CONSTRAINT pk_notifications      PRIMARY KEY (notif_id),
    CONSTRAINT fk_notifications_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Auto-generated alerts: attendance risk, demerits, calendar events, engagement warnings.';

CREATE INDEX idx_notifications_student ON notifications (student_id);
CREATE INDEX idx_notifications_read    ON notifications (is_read);

-- ============================================================
-- TABLE 11: disputes
-- Student-submitted requests to correct attendance records.
-- Relationship to students: many-to-one.
-- Relationship to attendance: many-to-one (references original record).
-- ============================================================
CREATE TABLE disputes (
    dispute_id          INT           NOT NULL AUTO_INCREMENT,
    student_id          INT           NOT NULL,
    record_id           INT           NOT NULL,
    reason              TEXT          NOT NULL,
    requested_status    ENUM('Present','Excused','Late') NOT NULL,
    submitted_date      DATE          NOT NULL,
    status              ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
    admin_response      TEXT          DEFAULT NULL,
    resolved_by         INT           DEFAULT NULL,
    resolved_at         DATETIME      DEFAULT NULL,
    CONSTRAINT pk_disputes            PRIMARY KEY (dispute_id),
    CONSTRAINT fk_disputes_student    FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_disputes_attendance FOREIGN KEY (record_id)
        REFERENCES attendance (record_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_disputes_admin      FOREIGN KEY (resolved_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Attendance correction requests. Admin resolves by updating status and admin_response.';

CREATE INDEX idx_disputes_student ON disputes (student_id);
CREATE INDEX idx_disputes_status  ON disputes (status);

-- ============================================================
-- TABLE 12: calendar_events
-- Scheduled events viewable by all users with program filtering.
-- ============================================================
CREATE TABLE calendar_events (
    event_id        INT           NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)  NOT NULL,
    event_date      DATE          NOT NULL,
    program_type    ENUM('All','CWTS','ROTC') NOT NULL DEFAULT 'All',
    location        VARCHAR(150)  DEFAULT NULL,
    description     TEXT          DEFAULT NULL,
    reminder_flag   TINYINT(1)    NOT NULL DEFAULT 0,
    posted_by       INT           DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_calendar_events     PRIMARY KEY (event_id),
    CONSTRAINT fk_calendar_events_user FOREIGN KEY (posted_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Scheduled events for calendar display. Filtered by program_type per user role.';

CREATE INDEX idx_calendar_date    ON calendar_events (event_date);
CREATE INDEX idx_calendar_program ON calendar_events (program_type);

-- ============================================================
-- TABLE 13: feedback
-- Student-submitted feedback and concerns to administrators.
-- ============================================================
CREATE TABLE feedback (
    feedback_id     INT           NOT NULL AUTO_INCREMENT,
    student_id      INT           NOT NULL,
    submitted_date  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type            VARCHAR(80)   NOT NULL,
    subject         VARCHAR(200)  NOT NULL,
    body            TEXT          NOT NULL,
    status          ENUM('Pending','Resolved') NOT NULL DEFAULT 'Pending',
    admin_response  TEXT          DEFAULT NULL,
    resolved_by     INT           DEFAULT NULL,
    resolved_at     DATETIME      DEFAULT NULL,
    CONSTRAINT pk_feedback          PRIMARY KEY (feedback_id),
    CONSTRAINT fk_feedback_student  FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_feedback_admin    FOREIGN KEY (resolved_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Student feedback and concern submissions with admin resolution tracking.';

CREATE INDEX idx_feedback_student ON feedback (student_id);
CREATE INDEX idx_feedback_status  ON feedback (status);

-- ============================================================
-- USEFUL VIEWS FOR COMMON QUERIES
-- ============================================================

-- View: student points balance (net = awards - deductions)
CREATE OR REPLACE VIEW vw_points_balance AS
SELECT
    s.student_id,
    s.full_name,
    s.program,
    COALESCE(SUM(CASE WHEN p.type = 'award'  THEN p.amount ELSE 0 END), 0)
        - COALESCE(SUM(CASE WHEN p.type = 'deduct' THEN p.amount ELSE 0 END), 0)
        AS net_balance
FROM students s
LEFT JOIN points p ON s.student_id = p.student_id
GROUP BY s.student_id, s.full_name, s.program;

-- View: student attendance summary (rate per student)
CREATE OR REPLACE VIEW vw_attendance_summary AS
SELECT
    s.student_id,
    s.full_name,
    COUNT(a.record_id)                                               AS total_sessions,
    SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END)           AS present_count,
    SUM(CASE WHEN a.status = 'Absent'  THEN 1 ELSE 0 END)           AS absent_count,
    SUM(CASE WHEN a.status = 'Late'    THEN 1 ELSE 0 END)           AS late_count,
    SUM(CASE WHEN a.status = 'Excused' THEN 1 ELSE 0 END)           AS excused_count,
    ROUND(
        COALESCE(SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END), 0)
        / NULLIF(COUNT(a.record_id), 0) * 100, 2
    ) AS attendance_rate_pct
FROM students s
LEFT JOIN attendance a ON s.student_id = a.student_id
GROUP BY s.student_id, s.full_name;

-- View: CWTS engagement totals per student
CREATE OR REPLACE VIEW vw_engagement_totals AS
SELECT
    s.student_id,
    s.full_name,
    COALESCE(SUM(e.hours_rendered), 0)                              AS total_hours,
    COALESCE(SUM(CASE WHEN e.verified = 1 THEN e.hours_rendered ELSE 0 END), 0) AS verified_hours
FROM students s
LEFT JOIN engagement e ON s.student_id = e.student_id
WHERE s.program = 'CWTS'
GROUP BY s.student_id, s.full_name;

-- View: ROTC performance averages
CREATE OR REPLACE VIEW vw_performance_avg AS
SELECT
    s.student_id,
    s.full_name,
    COUNT(p.entry_id) AS total_entries,
    ROUND(AVG(
        CASE p.rating
            WHEN 'Excellent'         THEN 5
            WHEN 'Proficient'        THEN 4
            WHEN 'Satisfactory'      THEN 3
            WHEN 'Needs_Improvement' THEN 2
            WHEN 'Unsatisfactory'    THEN 1
            ELSE 0
        END
    ), 2) AS avg_score
FROM students s
LEFT JOIN performance p ON s.student_id = p.student_id
WHERE s.program = 'ROTC'
GROUP BY s.student_id, s.full_name;

-- View: active demerit counts per ROTC student
CREATE OR REPLACE VIEW vw_demerit_summary AS
SELECT
    s.student_id,
    s.full_name,
    COUNT(d.demerit_id) AS total_demerits,
    SUM(CASE WHEN d.status = 'Active' THEN 1 ELSE 0 END) AS active_demerits,
    SUM(CASE d.severity
            WHEN 'Minor'    THEN 1
            WHEN 'Moderate' THEN 2
            WHEN 'Major'    THEN 3
            ELSE 0
        END) AS weighted_score
FROM students s
LEFT JOIN demerits d ON s.student_id = d.student_id
WHERE s.program = 'ROTC'
GROUP BY s.student_id, s.full_name;

-- ============================================================
-- SAMPLE DATA — DEMONSTRATION DATABASE
-- Realistic Filipino names, PLM-appropriate sections
-- ============================================================

-- Students (3 CWTS, 3 ROTC)
INSERT INTO students (full_name, program, year_level, section, contact_no, email) VALUES
('Maria Santos Reyes',        'CWTS', '2nd Year', 'BSIT-2A', '09171234567', 'maria.reyes@plm.edu.ph'),
('Jose Dela Cruz Mendoza',    'CWTS', '2nd Year', 'BSIT-2A', '09182345678', 'jose.mendoza@plm.edu.ph'),
('Ana Beatriz Villanueva',    'CWTS', '2nd Year', 'BSIT-2B', '09193456789', 'ana.villanueva@plm.edu.ph'),
('Carlo Miguel Bautista',     'ROTC', '2nd Year', 'BSIT-2A', '09204567890', 'carlo.bautista@plm.edu.ph'),
('Jessa Marie Fernandez',     'ROTC', '2nd Year', 'BSIT-2B', '09215678901', 'jessa.fernandez@plm.edu.ph'),
('Ryan Paolo Aquino',         'ROTC', '2nd Year', 'BSIT-2C', '09226789012', 'ryan.aquino@plm.edu.ph');

-- Users (1 admin, 1 ROTC senior, then one per student)
-- NOTE: passwords are SHA-256 hashes of 'Admin@123', 'Senior@123', 'Student@123'
INSERT INTO users (username, password_hash, role, student_id, is_active) VALUES
('admin',           'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'Admin',        NULL, 1),
('rotc_senior',     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ROTC_Senior',  4,    1),
('maria_reyes',     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'CWTS_Student', 1,    1),
('jose_mendoza',    '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'CWTS_Student', 2,    1),
('ana_villanueva',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'CWTS_Student', 3,    1),
('carlo_bautista',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ROTC_Student', 4,    1),
('jessa_fernandez', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ROTC_Student', 5,    1),
('ryan_aquino',     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ROTC_Student', 6,    1);

-- Attendance (3 sessions, mixed statuses)
INSERT INTO attendance (student_id, session_name, session_date, status, logged_by) VALUES
(1, 'Week 1 - Orientation',    '2026-01-10', 'Present', 1),
(2, 'Week 1 - Orientation',    '2026-01-10', 'Present', 1),
(3, 'Week 1 - Orientation',    '2026-01-10', 'Absent',  1),
(4, 'Week 1 - Drill Practice', '2026-01-10', 'Present', 1),
(5, 'Week 1 - Drill Practice', '2026-01-10', 'Present', 1),
(6, 'Week 1 - Drill Practice', '2026-01-10', 'Late',    1),
(1, 'Week 2 - Community Prep', '2026-01-17', 'Present', 1),
(2, 'Week 2 - Community Prep', '2026-01-17', 'Late',    1),
(3, 'Week 2 - Community Prep', '2026-01-17', 'Present', 1),
(4, 'Week 2 - Tactical Drill', '2026-01-17', 'Present', 1),
(5, 'Week 2 - Tactical Drill', '2026-01-17', 'Absent',  1),
(6, 'Week 2 - Tactical Drill', '2026-01-17', 'Present', 1),
(1, 'Week 3 - Activity Day',   '2026-01-24', 'Present', 1),
(2, 'Week 3 - Activity Day',   '2026-01-24', 'Present', 1),
(3, 'Week 3 - Activity Day',   '2026-01-24', 'Excused', 1),
(4, 'Week 3 - Field Exercise', '2026-01-24', 'Absent',  1),
(5, 'Week 3 - Field Exercise', '2026-01-24', 'Present', 1),
(6, 'Week 3 - Field Exercise', '2026-01-24', 'Present', 1);

-- Engagement (CWTS students — student 1 near completion)
INSERT INTO engagement (student_id, activity_name, activity_date, location, category, hours_rendered, verified, logged_by) VALUES
(1, 'Coastal Cleanup Drive',       '2026-01-15', 'Manila Bay',        'Environmental',    8.00, 1, 1),
(1, 'Tree Planting Activity',      '2026-01-22', 'Luneta Park',       'Environmental',    6.00, 1, 1),
(1, 'Senior Citizen Assistance',   '2026-02-05', 'Barangay 673',      'Social Services',  4.00, 1, 1),
(1, 'Blood Drive Volunteer',       '2026-02-12', 'PLM Main Building', 'Health Services',  4.00, 0, 1),
(2, 'Coastal Cleanup Drive',       '2026-01-15', 'Manila Bay',        'Environmental',    8.00, 1, 1),
(2, 'Literacy Outreach Program',   '2026-01-29', 'Barangay 671',      'Education',        5.00, 1, 1),
(3, 'Medical Mission Volunteer',   '2026-02-01', 'Sta. Ana Hospital', 'Health Services',  6.00, 1, 1);

-- Performance (ROTC — student 5 is at-risk with 3 consecutive low ratings)
INSERT INTO performance (student_id, drill_name, drill_date, rating, remarks, officer_name, logged_by) VALUES
(4, 'Basic Drill Commands',  '2026-01-12', 'Proficient',        'Good posture and timing.',      'Capt. Reyes',  2),
(4, 'Physical Fitness Test', '2026-01-19', 'Excellent',         'Outstanding performance.',      'Capt. Reyes',  2),
(4, 'Tactical Exercise A',   '2026-01-26', 'Satisfactory',      'Needs work on formation.',      'Capt. Reyes',  2),
(5, 'Basic Drill Commands',  '2026-01-12', 'Satisfactory',      'Average performance.',          'Capt. Reyes',  2),
(5, 'Physical Fitness Test', '2026-01-19', 'Needs_Improvement', 'Did not complete all events.',  'Capt. Reyes',  2),
(5, 'Tactical Exercise A',   '2026-01-26', 'Needs_Improvement', 'Poor formation alignment.',     'Capt. Reyes',  2),
(5, 'Rifle Handling',        '2026-02-02', 'Unsatisfactory',    'Failed safety procedures.',     'Capt. Reyes',  2),
(6, 'Basic Drill Commands',  '2026-01-12', 'Excellent',         'Exceptional execution.',        'Capt. Reyes',  2),
(6, 'Physical Fitness Test', '2026-01-19', 'Proficient',        'Strong endurance.',             'Capt. Reyes',  2);

-- Points transactions
INSERT INTO points (student_id, amount, type, reason, transaction_date, admin_id) VALUES
(1, 50,  'award',  'Perfect attendance Week 1',              '2026-01-10', 1),
(1, 30,  'award',  'Coastal Cleanup participation',          '2026-01-15', 1),
(2, 50,  'award',  'Perfect attendance Week 1',              '2026-01-10', 1),
(2, 20,  'deduct', 'Late submission of activity form',       '2026-01-20', 1),
(3, 25,  'award',  'Volunteer work recognition',             '2026-02-01', 1),
(4, 50,  'award',  'Perfect attendance Week 1',              '2026-01-10', 1),
(4, 40,  'award',  'Excellent drill performance',            '2026-01-19', 1),
(5, 30,  'award',  'Attendance compliance Week 1',           '2026-01-10', 1),
(5, 30,  'deduct', 'Incomplete physical fitness test',       '2026-01-19', 1),
(6, 50,  'award',  'Perfect attendance Week 1',              '2026-01-10', 1),
(6, 50,  'award',  'Excellent drill performance',            '2026-01-12', 1);

-- Demerits (ROTC students)
INSERT INTO demerits (student_id, reason, severity, notes, officer_name, demerit_date, status, logged_by) VALUES
(5, 'Incomplete uniform during inspection', 'Minor',    'Missing cap.',              'Capt. Reyes', '2026-01-12', 'Active',    2),
(5, 'Failed safety procedures',             'Major',    'Refer to performance log.', 'Capt. Reyes', '2026-02-02', 'Active',    2),
(6, 'Late reporting to formation',          'Minor',    'First offense.',            'Capt. Reyes', '2026-01-24', 'Dismissed', 2);

-- Announcements
INSERT INTO announcements (title, body, audience, priority, posted_by, is_pinned) VALUES
('Welcome to NSTP AY 2025-2026',
 'All NSTP students are reminded to review the attendance policy. Attendance below 75% will result in a failing grade.',
 'All', 'Important', 1, 1),
('CWTS Community Service Schedule',
 'CWTS students must complete a minimum of 20 hours of verified community service by the end of the semester. Check the calendar for scheduled activities.',
 'CWTS', 'Urgent', 1, 1),
('ROTC Uniform Inspection — Week 4',
 'A full uniform inspection will be conducted during Week 4 training. Ensure all uniform components are complete and serviceable.',
 'ROTC', 'Important', 1, 0);

-- Calendar Events
INSERT INTO calendar_events (title, event_date, program_type, location, description, reminder_flag, posted_by) VALUES
('CWTS Coastal Cleanup',      '2026-03-01', 'CWTS', 'Manila Bay',        'Community service activity. Bring gloves.', 1, 1),
('ROTC Field Exercise',       '2026-03-05', 'ROTC', 'Camp Aguinaldo',    'Half-day field exercise.',                  1, 1),
('NSTP General Assembly',     '2026-03-10', 'All',  'PLM Gymnasium',     'Mandatory for all NSTP students.',          1, 1),
('CWTS Literacy Outreach',    '2026-03-15', 'CWTS', 'Barangay 671',      'Bring school supplies.',                    0, 1),
('ROTC Physical Fitness Test','2026-03-20', 'ROTC', 'PLM Track Field',   'Formal PFT. All cadets must participate.',  1, 1);

-- Disputes (one unresolved)
INSERT INTO disputes (student_id, record_id, reason, requested_status, submitted_date, status) VALUES
(3, 3, 'I was absent because I was admitted to the hospital. Attached medical certificate as proof.', 'Excused', '2026-01-18', 'Pending');

-- Notifications
INSERT INTO notifications (student_id, type, message, is_read) VALUES
(5, 'Performance Alert',  'Your performance has been flagged as At-Risk. Three consecutive low ratings recorded.', 0),
(5, 'Demerit Notice',     'A Major demerit has been assigned to your record. Please report to your officer.', 0),
(3, 'Attendance Notice',  'You have an unresolved attendance dispute. Check the Attendance module for status.', 0),
(1, 'Reminder',           'CWTS Coastal Cleanup is scheduled in 3 days (March 1). Check the Calendar module.', 0);

-- ============================================================
-- SETUP INSTRUCTIONS
-- ============================================================
-- HOW TO IMPORT IN phpMyAdmin:
--   1. Open browser → http://localhost/phpmyadmin
--   2. Click "Import" in the top navigation bar
--   3. Click "Choose File" and select this .sql file
--   4. Ensure "Format: SQL" is selected
--   5. Click "Go" — the database and all tables will be created
--
-- HOW TO CONNECT FROM JAVA (JDBC):
--   URL:      jdbc:mysql://localhost:3306/nstp_infosystem
--   User:     root
--   Password: (blank for default XAMPP)
--   Driver:   com.mysql.cj.jdbc.Driver (MySQL Connector/J 8.x)
--
-- VERIFY SETUP:
--   SELECT * FROM students;             -- Should return 6 rows
--   SELECT * FROM vw_points_balance;    -- Should show net balances
--   SELECT * FROM vw_attendance_summary; -- Should show rates per student
-- ============================================================

SELECT 'nstp_infosystem database created successfully.' AS status;
SELECT CONCAT('Tables created: ', COUNT(*)) AS table_count
FROM information_schema.tables
WHERE table_schema = 'nstp_infosystem' AND table_type = 'BASE TABLE';
