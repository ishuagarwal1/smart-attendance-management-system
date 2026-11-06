-- =====================================================================
--  Smart Attendance Management System (SAMS)  -  Database schema
--  Works on MySQL 8.x and MariaDB 10.x
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================
DROP DATABASE IF EXISTS sams_db;
CREATE DATABASE sams_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sams_db;

-- ---------------------------------------------------------------------
-- 1. USERS : every person who can log in (admin / teacher / student)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL,
    role           ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. STUDENTS : extra profile data for users whose role = STUDENT
-- ---------------------------------------------------------------------
CREATE TABLE students (
    user_id     BIGINT      NOT NULL,
    roll_no     VARCHAR(30) NOT NULL,
    department  VARCHAR(60) NOT NULL,
    semester    TINYINT     NOT NULL,
    phone       VARCHAR(20) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_students_roll (roll_no),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. COURSES : subjects taught by a teacher
-- ---------------------------------------------------------------------
CREATE TABLE courses (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    course_code  VARCHAR(20)  NOT NULL,
    course_name  VARCHAR(120) NOT NULL,
    department   VARCHAR(60)  NOT NULL,
    semester     TINYINT      NOT NULL,
    credits      TINYINT      NOT NULL DEFAULT 3,
    teacher_id   BIGINT       NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_courses_code (course_code),
    KEY idx_courses_teacher (teacher_id),
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. ENROLLMENTS : which student studies which course (many-to-many)
-- ---------------------------------------------------------------------
CREATE TABLE enrollments (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    course_id    BIGINT NOT NULL,
    student_id   BIGINT NOT NULL,
    enrolled_on  DATE   NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollment (course_id, student_id),
    CONSTRAINT fk_enr_course  FOREIGN KEY (course_id)  REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES users(id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. ATTENDANCE : one row per student, per course, per day
--    The UNIQUE key makes double marking impossible and lets us use
--    INSERT ... ON DUPLICATE KEY UPDATE to correct a mistake safely.
-- ---------------------------------------------------------------------
CREATE TABLE attendance (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    course_id   BIGINT      NOT NULL,
    student_id  BIGINT      NOT NULL,
    att_date    DATE        NOT NULL,
    status      ENUM('PRESENT','ABSENT','LATE') NOT NULL,
    remarks     VARCHAR(200) NULL,
    marked_by   BIGINT      NULL,
    marked_via  ENUM('WEB','API') NOT NULL DEFAULT 'WEB',
    marked_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attendance (course_id, student_id, att_date),
    KEY idx_att_student (student_id),
    KEY idx_att_date (att_date),
    CONSTRAINT fk_att_course  FOREIGN KEY (course_id)  REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_att_marker  FOREIGN KEY (marked_by)  REFERENCES users(id)   ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. API_KEYS : keys handed to external devices (RFID / biometric / apps)
--    Only the SHA-256 hash of a key is stored, never the key itself.
-- ---------------------------------------------------------------------
CREATE TABLE api_keys (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    label         VARCHAR(80)  NOT NULL,
    key_prefix    VARCHAR(12)  NOT NULL,
    key_hash      CHAR(64)     NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_by    BIGINT       NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at  TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_api_key_hash (key_hash),
    CONSTRAINT fk_key_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. AUDIT_LOG : who did what and when (security & accountability)
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    action      VARCHAR(50)  NOT NULL,
    details     VARCHAR(400) NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_time (created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Report view : attendance percentage per student per course
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_attendance_summary AS
SELECT  c.id            AS course_id,
        c.course_code,
        c.course_name,
        u.id            AS student_id,
        s.roll_no,
        u.full_name     AS student_name,
        COUNT(a.id)                                         AS total_classes,
        SUM(a.status = 'PRESENT')                           AS present_count,
        SUM(a.status = 'LATE')                              AS late_count,
        SUM(a.status = 'ABSENT')                            AS absent_count,
        ROUND(100 * SUM(a.status IN ('PRESENT','LATE')) / NULLIF(COUNT(a.id),0), 1) AS percentage
FROM enrollments e
JOIN courses  c ON c.id = e.course_id
JOIN users    u ON u.id = e.student_id
JOIN students s ON s.user_id = u.id
LEFT JOIN attendance a ON a.course_id = e.course_id AND a.student_id = e.student_id
GROUP BY c.id, c.course_code, c.course_name, u.id, s.roll_no, u.full_name;

-- ---------------------------------------------------------------------
-- Application database user (used by src/main/resources/db.properties)
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'sams_user'@'localhost' IDENTIFIED BY 'Sams@2026';
GRANT ALL PRIVILEGES ON sams_db.* TO 'sams_user'@'localhost';
FLUSH PRIVILEGES;
