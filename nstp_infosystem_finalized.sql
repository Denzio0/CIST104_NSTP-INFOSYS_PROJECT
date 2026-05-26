-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 03, 2026 at 10:31 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE `announcements` (
  `announcement_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `body` text NOT NULL,
  `audience` enum('All','CWTS','ROTC') NOT NULL DEFAULT 'All',
  `priority` enum('Normal','Important','Urgent') NOT NULL DEFAULT 'Normal',
  `posted_date` datetime NOT NULL DEFAULT current_timestamp(),
  `posted_by` int(11) DEFAULT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


INSERT INTO `announcements` (`announcement_id`, `title`, `body`, `audience`, `priority`, `posted_date`, `posted_by`, `is_pinned`) VALUES
(1, 'Depende', 'Urgent!!!!!', 'ROTC', 'Normal', '2026-05-02 09:22:48', NULL, 0);


CREATE TABLE `attendance` (
  `record_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `session_name` varchar(100) NOT NULL,
  `session_date` date NOT NULL,
  `status` enum('Present','Absent','Late','Excused') NOT NULL,
  `remarks` text DEFAULT NULL,
  `logged_by` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `attendance` (`record_id`, `student_id`, `session_name`, `session_date`, `status`, `remarks`, `logged_by`, `created_at`, `is_deleted`) VALUES
(1, 13, '1st Training Day', '2026-05-02', 'Present', '', 1, '2026-05-02 10:18:07', 0),
(2, 6, '1st Training Day', '2026-05-02', 'Late', '', 1, '2026-05-02 10:18:07', 0),
(4, 7, '1st Training Day', '2026-05-02', 'Absent', '', 1, '2026-05-02 10:18:07', 0),
(5, 10, '1st Training Day', '2026-05-02', 'Present', '', 1, '2026-05-02 10:18:07', 0),
(16, 11, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(17, 13, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(18, 5, 'ddsds', '2026-05-02', 'Absent', '', 1, '2026-05-02 10:20:01', 0),
(19, 6, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(21, 2, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(22, 7, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(23, 1, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(24, 3, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(25, 12, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(26, 4, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(27, 9, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(28, 10, 'ddsds', '2026-05-02', 'Present', '', 1, '2026-05-02 10:20:01', 0),
(29, 11, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(30, 13, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(31, 5, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(32, 6, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(34, 2, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(35, 7, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(36, 1, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(37, 3, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(38, 12, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(39, 4, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(40, 9, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0),
(41, 10, 'Chacha', '2026-05-02', 'Present', '', 1, '2026-05-02 10:53:35', 0);

DELIMITER $$
CREATE TRIGGER `trg_attendance_date` BEFORE INSERT ON `attendance` FOR EACH ROW BEGIN
    IF NEW.session_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid future date';
    END IF;
END
$$
DELIMITER ;

CREATE TABLE `calendar_events` (
  `event_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `event_date` date NOT NULL,
  `program_type` enum('All','CWTS','ROTC') NOT NULL DEFAULT 'All',
  `location` varchar(150) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `reminder_flag` tinyint(1) NOT NULL DEFAULT 0,
  `created_by` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cocc_ranks` (
  `rank_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `rank_name` varchar(100) NOT NULL DEFAULT 'Cadet',
  `platoon` varchar(50) DEFAULT NULL,
  `battalion` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `demerits` (
  `demerit_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `reason` varchar(200) NOT NULL,
  `severity` enum('Minor','Moderate','Major') NOT NULL,
  `notes` text DEFAULT NULL,
  `officer_name` varchar(100) DEFAULT NULL,
  `demerit_date` date NOT NULL,
  `status` enum('Active','Appealed','Dismissed') NOT NULL DEFAULT 'Active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `demerits` (`demerit_id`, `student_id`, `reason`, `severity`, `notes`, `officer_name`, `demerit_date`, `status`) VALUES
(1, 7, 'trip lang po ni admin', 'Minor', NULL, NULL, '2026-05-02', 'Active'),
(2, 7, 'try again', 'Minor', NULL, NULL, '2026-05-02', 'Active');

CREATE TABLE `disputes` (
  `dispute_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `record_id` int(11) NOT NULL,
  `reason` text NOT NULL,
  `requested_status` enum('Present','Excused','Late') NOT NULL,
  `submitted_date` date NOT NULL,
  `status` enum('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  `admin_response` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `engagement` (
  `activity_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `activity_name` varchar(150) NOT NULL,
  `activity_date` date NOT NULL,
  `location` varchar(150) DEFAULT NULL,
  `category` varchar(80) DEFAULT NULL,
  `hours_rendered` decimal(5,2) NOT NULL CHECK (`hours_rendered` > 0 and `hours_rendered` <= 24),
  `verified` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `engagement` (`activity_id`, `student_id`, `activity_name`, `activity_date`, `location`, `category`, `hours_rendered`, `verified`, `created_at`) VALUES
(1, 5, 'sasas', '2026-05-02', NULL, 'Community Service', 2.00, 0, '2026-05-02 10:21:00');

CREATE TABLE `feedback` (
  `feedback_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `submitted_date` datetime NOT NULL DEFAULT current_timestamp(),
  `type` varchar(80) NOT NULL,
  `subject` varchar(200) NOT NULL,
  `body` text NOT NULL,
  `status` enum('Open','Resolved') NOT NULL DEFAULT 'Open',
  `admin_response` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notifications` (
  `notif_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `type` varchar(80) NOT NULL,
  `message` text NOT NULL,
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_only` date GENERATED ALWAYS AS (cast(`created_date` as date)) STORED
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `notifications` (`notif_id`, `student_id`, `type`, `message`, `created_date`, `is_read`) VALUES
(1, 3, 'ENGAGEMENT_WARNING', 'You have 0.0 of 20 required engagement hours.', '2026-05-01 21:56:50', 0),
(2, 3, 'ENGAGEMENT_WARNING', 'You have 0.0 of 20 required engagement hours.', '2026-05-02 00:54:17', 0),
(125, 2, 'ENGAGEMENT_WARNING', 'You have 0.0 of 20 required engagement hours.', '2026-05-02 09:40:54', 0),
(136, 5, 'ENGAGEMENT_WARNING', 'You have 0.0 of 20 required engagement hours.', '2026-05-02 10:20:22', 0);

CREATE TABLE `performance` (
  `entry_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `drill_name` varchar(100) NOT NULL,
  `drill_date` date NOT NULL,
  `rating` enum('Excellent','Proficient','Satisfactory','Needs_Improvement','Unsatisfactory') NOT NULL,
  `remarks` text DEFAULT NULL,
  `officer_name` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `performance` (`entry_id`, `student_id`, `drill_name`, `drill_date`, `rating`, `remarks`, `officer_name`, `created_at`) VALUES
(1, 6, 'Marching', '2026-05-02', 'Proficient', 'NONE', NULL, '2026-05-02 08:07:22'),
(2, 6, 'Quiz', '2026-05-02', 'Excellent', NULL, NULL, '2026-05-02 13:13:21');

DELIMITER $$
CREATE TRIGGER `trg_performance_date` BEFORE INSERT ON `performance` FOR EACH ROW BEGIN
    IF NEW.drill_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid future date';
    END IF;
END
$$
DELIMITER ;

CREATE TABLE `points` (
  `transaction_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `amount` int(11) NOT NULL CHECK (`amount` > 0),
  `type` enum('award','deduct') NOT NULL,
  `reason` text NOT NULL,
  `transaction_date` date NOT NULL,
  `admin_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `points` (`transaction_id`, `student_id`, `amount`, `type`, `reason`, `transaction_date`, `admin_id`) VALUES
(1, 2, 5, 'award', 'Recitation', '2026-05-02', NULL);

DELIMITER $$
CREATE TRIGGER `trg_points_date` BEFORE INSERT ON `points` FOR EACH ROW BEGIN
    IF NEW.transaction_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid future date';
    END IF;
END
$$
DELIMITER ;

CREATE TABLE `read_announcements` (
  `student_id` int(11) NOT NULL,
  `announcement_id` int(11) NOT NULL,
  `read_date` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rotc_ranks` (
  `rank_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `rank_name` varchar(100) NOT NULL DEFAULT 'Cadet',
  `platoon` varchar(50) DEFAULT NULL,
  `battalion` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `students` (
  `student_id` int(11) NOT NULL,
  `full_name` varchar(150) NOT NULL,
  `program` enum('CWTS','ROTC') NOT NULL,
  `year_level` varchar(20) NOT NULL,
  `section` varchar(20) NOT NULL,
  `contact_no` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT NULL ON UPDATE current_timestamp(),
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `students` (`student_id`, `full_name`, `program`, `year_level`, `section`, `contact_no`, `email`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'Jose Gabriel Baterbonia', 'CWTS', '1st Year', 'BSIT 1-3', '09171234501', 'jgkbaterbonia2025@plm.edu.ph', '2026-04-28 15:02:00', NULL, 0),
(2, 'Ivan Rafael Capulong', 'CWTS', '1st Year', 'BSIT 1-3', '09171234502', 'irgcapulong2025@plm.edu.ph', '2026-04-28 15:02:00', NULL, 0),
(3, 'Marc Roden Famero', 'CWTS', '1st Year', 'BSIT 1-3', '09171234503', 'mrdfamero2025@plm.edu.ph', '2026-04-28 15:02:00', NULL, 0),
(4, 'Maximillian Kurt De Leon', 'CWTS', '1st Year', 'BSIT 1-3', '09171234501', 'mkdeleon2025@plm.edu.ph', '2026-04-28 15:04:42', NULL, 0),
(5, 'Emmanuel Salazar', 'CWTS', '1st Year', 'BSIT 1-3', '09171234501', 'esalazar2025@plm.edu.ph', '2026-04-28 15:07:21', NULL, 0),
(6, 'Gabrielle Keira Tinoko', 'ROTC', '1st Year', 'BSIT 1-3', '09171234504', 'gktinoko2025@plm.edu.ph', '2026-04-28 16:36:10', NULL, 0),
(7, 'John Alwyn Vallite', 'ROTC', '1st Year', 'BSIT 1-3', '09171234505', 'javallite2025@plm.edu.ph', '2026-04-28 16:36:10', NULL, 0),
(9, 'Test CWTS Student', 'CWTS', '1st Year', 'BSIT 1-3', '09001111111', 'cwts@test.com', '2026-04-29 11:28:23', NULL, 0),
(10, 'Test ROTC Student', 'ROTC', '1st Year', 'BSCE 1-1', NULL, NULL, '2026-04-29 11:29:17', NULL, 0),
(11, 'Bad Student', '', '1st Year', 'TEST 1-1', NULL, NULL, '2026-04-29 11:29:27', NULL, 0),
(12, 'Marcski', 'CWTS', '1st Year', 'BSIT 1-4', NULL, NULL, '2026-05-02 08:05:56', NULL, 0),
(13, 'Camacho, Rohan Daniel', 'ROTC', '1', 'BSCE 1-1', '09817363294', 'rohancamacho1@gmail.com', '2026-05-02 09:21:16', NULL, 0);

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('Admin','CWTS_Student','ROTC_Student','ROTC_Senior') NOT NULL,
  `student_id` int(11) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `last_login` datetime DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `users` (`user_id`, `username`, `password_hash`, `role`, `student_id`, `is_active`, `last_login`, `is_deleted`) VALUES
(1, 'admin', 'b0ee6afd12e7da9d94aabde4153fd8a3ec771002f11f038d359f30367ef1bc9b', 'Admin', NULL, 1, '2026-05-02 13:14:44', 0),
(9, 'jgbaterbonia2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'CWTS_Student', 1, 1, '2026-05-01 20:41:27', 0),
(10, 'ircapulong2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'CWTS_Student', 2, 1, '2026-05-02 13:25:09', 0),
(11, 'mrfamero2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'CWTS_Student', 3, 1, '2026-05-02 08:21:14', 0),
(12, 'mkdeleon2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'CWTS_Student', 4, 1, NULL, 0),
(13, 'esalazar2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'CWTS_Student', 5, 1, '2026-05-02 10:21:11', 0),
(14, 'gktinoko2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'ROTC_Student', 6, 1, '2026-05-02 13:14:09', 0),
(15, 'javallite2025', '1539498fbe81480199a59b69016b755effe03927772bcd8f9e8ec3884e2e64ae', 'ROTC_Student', 7, 1, '2026-05-02 13:19:05', 0),
(19, 'rdcamacho2025', 'd5d0296d59722588ef4253f073147a959321421d6397fa8d2a45918902023050', 'ROTC_Student', 13, 1, '2026-05-02 09:27:02', 0);

ALTER TABLE `announcements`
  ADD PRIMARY KEY (`announcement_id`),
  ADD KEY `idx_ann_pinned` (`is_pinned`,`posted_date`),
  ADD KEY `fk_ann_user` (`posted_by`);

ALTER TABLE `attendance`
  ADD PRIMARY KEY (`record_id`),
  ADD UNIQUE KEY `uq_attendance_entry` (`student_id`,`session_name`,`session_date`),
  ADD KEY `idx_attendance_session` (`session_name`,`session_date`),
  ADD KEY `fk_att_user` (`logged_by`);

ALTER TABLE `calendar_events`
  ADD PRIMARY KEY (`event_id`),
  ADD UNIQUE KEY `uq_event_title_date` (`title`,`event_date`,`program_type`),
  ADD KEY `idx_calendar_date` (`event_date`),
  ADD KEY `fk_cal_user` (`created_by`);

ALTER TABLE `cocc_ranks`
  ADD PRIMARY KEY (`rank_id`),
  ADD UNIQUE KEY `uq_cocc_rank_student` (`student_id`);

ALTER TABLE `demerits`
  ADD PRIMARY KEY (`demerit_id`),
  ADD KEY `fk_dem_student` (`student_id`);

ALTER TABLE `disputes`
  ADD PRIMARY KEY (`dispute_id`),
  ADD KEY `fk_disp_student` (`student_id`),
  ADD KEY `fk_disp_record` (`record_id`);

ALTER TABLE `engagement`
  ADD PRIMARY KEY (`activity_id`),
  ADD KEY `fk_eng_student` (`student_id`);

ALTER TABLE `feedback`
  ADD PRIMARY KEY (`feedback_id`),
  ADD KEY `fk_fb_student` (`student_id`);

ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notif_id`),
  ADD UNIQUE KEY `uq_notif_student_type_date` (`student_id`,`type`,`created_only`),
  ADD KEY `idx_notif_read` (`is_read`);

ALTER TABLE `performance`
  ADD PRIMARY KEY (`entry_id`),
  ADD KEY `fk_perf_student` (`student_id`);

ALTER TABLE `points`
  ADD PRIMARY KEY (`transaction_id`),
  ADD KEY `idx_points_student` (`student_id`),
  ADD KEY `fk_pts_admin` (`admin_id`);

ALTER TABLE `read_announcements`
  ADD PRIMARY KEY (`student_id`,`announcement_id`),
  ADD KEY `fk_ra_announcement` (`announcement_id`);

ALTER TABLE `rotc_ranks`
  ADD PRIMARY KEY (`rank_id`),
  ADD UNIQUE KEY `uq_rotc_rank_student` (`student_id`);

ALTER TABLE `students`
  ADD PRIMARY KEY (`student_id`);

ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `uq_username` (`username`),
  ADD KEY `fk_users_student` (`student_id`);

ALTER TABLE `announcements`
  MODIFY `announcement_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `attendance`
  MODIFY `record_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=52;

ALTER TABLE `calendar_events`
  MODIFY `event_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `cocc_ranks`
  MODIFY `rank_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `demerits`
  MODIFY `demerit_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `disputes`
  MODIFY `dispute_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `engagement`
  MODIFY `activity_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `feedback`
  MODIFY `feedback_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `notifications`
  MODIFY `notif_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=277;

ALTER TABLE `performance`
  MODIFY `entry_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `points`
  MODIFY `transaction_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `rotc_ranks`
  MODIFY `rank_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `students`
  MODIFY `student_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

ALTER TABLE `announcements`
  ADD CONSTRAINT `fk_ann_user` FOREIGN KEY (`posted_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `attendance`
  ADD CONSTRAINT `fk_att_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_att_user` FOREIGN KEY (`logged_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `calendar_events`
  ADD CONSTRAINT `fk_cal_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `cocc_ranks`
  ADD CONSTRAINT `fk_cocc_rank_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `demerits`
  ADD CONSTRAINT `fk_dem_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `disputes`
  ADD CONSTRAINT `fk_disp_record` FOREIGN KEY (`record_id`) REFERENCES `attendance` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_disp_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `engagement`
  ADD CONSTRAINT `fk_eng_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `feedback`
  ADD CONSTRAINT `fk_fb_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notif_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `performance`
  ADD CONSTRAINT `fk_perf_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `points`
  ADD CONSTRAINT `fk_pts_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pts_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `read_announcements`
  ADD CONSTRAINT `fk_ra_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `announcements` (`announcement_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_ra_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `rotc_ranks`
  ADD CONSTRAINT `fk_rotc_rank_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE SET NULL ON UPDATE CASCADE;
COMMIT;
