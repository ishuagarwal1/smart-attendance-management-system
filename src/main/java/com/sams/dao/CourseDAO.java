package com.sams.dao;

import com.sams.model.Course;
import com.sams.model.StudentInfo;
import com.sams.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Data access for courses and enrollments. */
public class CourseDAO {

    /** Course columns + teacher name + live statistics computed by sub-queries. */
    private static final String SELECT =
        "SELECT c.id, c.course_code, c.course_name, c.department, c.semester, c.credits, c.teacher_id, "
      + "  t.full_name AS teacher_name, "
      + "  (SELECT COUNT(*) FROM enrollments e WHERE e.course_id = c.id) AS enrolled, "
      + "  (SELECT COUNT(DISTINCT a.att_date) FROM attendance a WHERE a.course_id = c.id) AS classes_held, "
      + "  (SELECT ROUND(100 * SUM(a.status IN ('PRESENT','LATE')) / NULLIF(COUNT(*),0), 1) "
      + "     FROM attendance a WHERE a.course_id = c.id) AS avg_pct, "
      + "  (SELECT MAX(a.att_date) FROM attendance a WHERE a.course_id = c.id) AS last_marked "
      + "FROM courses c LEFT JOIN users t ON t.id = c.teacher_id ";

    private Course map(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getLong("id"));
        c.setCode(rs.getString("course_code"));
        c.setName(rs.getString("course_name"));
        c.setDepartment(rs.getString("department"));
        c.setSemester(rs.getInt("semester"));
        c.setCredits(rs.getInt("credits"));
        long tid = rs.getLong("teacher_id");
        c.setTeacherId(rs.wasNull() ? null : tid);
        c.setTeacherName(rs.getString("teacher_name"));
        c.setEnrolledCount(rs.getInt("enrolled"));
        c.setClassesHeld(rs.getInt("classes_held"));
        c.setAvgPercentage(rs.getDouble("avg_pct"));
        c.setLastMarked(rs.getDate("last_marked"));
        return c;
    }

    private List<Course> query(String where, Long param) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + where)) {
            if (param != null) ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Course> findAll() throws SQLException {
        return query("ORDER BY c.course_code", null);
    }

    public List<Course> findByTeacher(long teacherId) throws SQLException {
        return query("WHERE c.teacher_id = ? ORDER BY c.course_code", teacherId);
    }

    public List<Course> findByStudent(long studentId) throws SQLException {
        return query("JOIN enrollments en ON en.course_id = c.id WHERE en.student_id = ? ORDER BY c.course_code", studentId);
    }

    public Course findById(long id) throws SQLException {
        List<Course> l = query("WHERE c.id = ?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    public Course findByCode(String code) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE c.course_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public long create(Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_code, course_name, department, semester, credits, teacher_id) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setString(3, course.getDepartment());
            ps.setInt(4, course.getSemester());
            ps.setInt(5, course.getCredits());
            if (course.getTeacherId() == null) ps.setNull(6, Types.BIGINT); else ps.setLong(6, course.getTeacherId());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        }
    }

    public void assignTeacher(long courseId, Long teacherId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE courses SET teacher_id = ? WHERE id = ?")) {
            if (teacherId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, teacherId);
            ps.setLong(2, courseId);
            ps.executeUpdate();
        }
    }

    public void delete(long courseId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            ps.setLong(1, courseId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------- enrollments
    public boolean enroll(long courseId, long studentId) throws SQLException {
        String sql = "INSERT IGNORE INTO enrollments (course_id, student_id) VALUES (?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setLong(2, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    public void unenroll(long courseId, long studentId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM enrollments WHERE course_id = ? AND student_id = ?")) {
            ps.setLong(1, courseId);
            ps.setLong(2, studentId);
            ps.executeUpdate();
        }
    }

    /** Active students enrolled in the course, ordered by roll number. */
    public List<StudentInfo> findEnrolled(long courseId) throws SQLException {
        return students("JOIN enrollments e ON e.student_id = u.id WHERE e.course_id = ? AND u.active = 1 ORDER BY s.roll_no", courseId);
    }

    /** Active students that are NOT yet enrolled in the course. */
    public List<StudentInfo> findNotEnrolled(long courseId) throws SQLException {
        return students("WHERE u.active = 1 AND u.id NOT IN (SELECT student_id FROM enrollments WHERE course_id = ?) ORDER BY s.roll_no", courseId);
    }

    private List<StudentInfo> students(String tail, long courseId) throws SQLException {
        List<StudentInfo> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(StudentDAO.SELECT + tail)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(StudentDAO.map(rs)); }
        }
        return list;
    }

    public boolean isEnrolled(long courseId, long studentId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM enrollments WHERE course_id = ? AND student_id = ?")) {
            ps.setLong(1, courseId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
