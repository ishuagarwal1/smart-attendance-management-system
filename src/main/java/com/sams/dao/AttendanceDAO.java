package com.sams.dao;

import com.sams.model.AttendanceRecord;
import com.sams.model.AttendanceSummary;
import com.sams.model.DailyStat;
import com.sams.util.Config;
import com.sams.util.DBUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data access for attendance records and the reports built on them. */
public class AttendanceDAO {

    public static final String PRESENT = "PRESENT", ABSENT = "ABSENT", LATE = "LATE";

    public static boolean isValidStatus(String s) {
        return PRESENT.equals(s) || ABSENT.equals(s) || LATE.equals(s);
    }

    // ------------------------------------------------------------ marking
    /** Existing records of a course on one date: studentId -> record. */
    public Map<Long, AttendanceRecord> findByCourseAndDate(long courseId, Date date) throws SQLException {
        String sql = "SELECT id, student_id, status, remarks, marked_via FROM attendance WHERE course_id = ? AND att_date = ?";
        Map<Long, AttendanceRecord> map = new HashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setId(rs.getLong("id"));
                    r.setCourseId(courseId);
                    r.setStudentId(rs.getLong("student_id"));
                    r.setDate(date);
                    r.setStatus(rs.getString("status"));
                    r.setRemarks(rs.getString("remarks"));
                    r.setMarkedVia(rs.getString("marked_via"));
                    map.put(r.getStudentId(), r);
                }
            }
        }
        return map;
    }

    /**
     * Saves a whole class in one transaction. A second save for the same day
     * UPDATES the rows (UNIQUE key + ON DUPLICATE KEY UPDATE) - so teachers can fix mistakes.
     *
     * @return number of students saved
     */
    public int saveBatch(long courseId, Date date, Map<Long, String> statuses, Map<Long, String> remarks,
                         long markedBy, String via) throws SQLException {
        String sql = "INSERT INTO attendance (course_id, student_id, att_date, status, remarks, marked_by, marked_via) "
                   + "VALUES (?,?,?,?,?,?,?) "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status), remarks = VALUES(remarks), "
                   + "marked_by = VALUES(marked_by), marked_via = VALUES(marked_via)";
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Map.Entry<Long, String> e : statuses.entrySet()) {
                    ps.setLong(1, courseId);
                    ps.setLong(2, e.getKey());
                    ps.setDate(3, date);
                    ps.setString(4, e.getValue());
                    String rem = remarks == null ? null : remarks.get(e.getKey());
                    if (rem == null || rem.isEmpty()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, rem);
                    if (markedBy > 0) ps.setLong(6, markedBy); else ps.setNull(6, Types.BIGINT);
                    ps.setString(7, via);
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
                return statuses.size();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    // ------------------------------------------------------------ reports
    private AttendanceSummary mapSummary(ResultSet rs) throws SQLException {
        AttendanceSummary s = new AttendanceSummary();
        s.setCourseId(rs.getLong("course_id"));
        s.setCourseCode(rs.getString("course_code"));
        s.setCourseName(rs.getString("course_name"));
        s.setStudentId(rs.getLong("student_id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setStudentName(rs.getString("student_name"));
        s.setTotal(rs.getInt("total_classes"));
        s.setPresent(rs.getInt("present_count"));
        s.setLate(rs.getInt("late_count"));
        s.setAbsent(rs.getInt("absent_count"));
        s.setPercentage(rs.getDouble("percentage"));
        s.setThreshold(Config.threshold());
        return s;
    }

    private List<AttendanceSummary> summaries(String where, Object param) throws SQLException {
        List<AttendanceSummary> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM v_attendance_summary " + where)) {
            if (param instanceof Long) ps.setLong(1, (Long) param); else if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapSummary(rs)); }
        }
        return list;
    }

    /** Every student of one course (teacher report). */
    public List<AttendanceSummary> courseSummary(long courseId) throws SQLException {
        return summaries("WHERE course_id = ? ORDER BY roll_no", courseId);
    }

    /** Every course of one student (student dashboard). */
    public List<AttendanceSummary> studentSummary(long studentId) throws SQLException {
        return summaries("WHERE student_id = ? ORDER BY course_code", studentId);
    }

    /** Defaulters: all (student, course) pairs below the given percentage. */
    public List<AttendanceSummary> belowThreshold(double threshold) throws SQLException {
        return summaries("WHERE total_classes > 0 AND percentage < ? ORDER BY percentage, roll_no", threshold);
    }

    /** Date-wise history of one student in one course (newest first). */
    public List<AttendanceRecord> history(long studentId, long courseId) throws SQLException {
        String sql = "SELECT a.id, a.att_date, a.status, a.remarks, a.marked_via, u.full_name "
                   + "FROM attendance a LEFT JOIN users u ON u.id = a.marked_by "
                   + "WHERE a.student_id = ? AND a.course_id = ? ORDER BY a.att_date DESC";
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setId(rs.getLong("id"));
                    r.setStudentId(studentId);
                    r.setCourseId(courseId);
                    r.setDate(rs.getDate("att_date"));
                    r.setStatus(rs.getString("status"));
                    r.setRemarks(rs.getString("remarks"));
                    r.setMarkedVia(rs.getString("marked_via"));
                    r.setMarkedByName(rs.getString("full_name"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** Raw rows for the REST API / CSV: optional filters (null = ignore). */
    public List<Map<String, Object>> search(Long courseId, Long studentId, Date from, Date to, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT a.att_date, c.course_code, s.roll_no, u.full_name, a.status, a.marked_via "
          + "FROM attendance a JOIN courses c ON c.id = a.course_id "
          + "JOIN users u ON u.id = a.student_id JOIN students s ON s.user_id = u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (courseId != null)  { sql.append("AND a.course_id = ? ");  params.add(courseId); }
        if (studentId != null) { sql.append("AND a.student_id = ? "); params.add(studentId); }
        if (from != null)      { sql.append("AND a.att_date >= ? ");  params.add(from); }
        if (to != null)        { sql.append("AND a.att_date <= ? ");  params.add(to); }
        sql.append("ORDER BY a.att_date DESC, s.roll_no LIMIT ").append(Math.max(1, Math.min(limit, 1000)));
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", rs.getDate("att_date").toString());
                    row.put("courseCode", rs.getString("course_code"));
                    row.put("rollNo", rs.getString("roll_no"));
                    row.put("studentName", rs.getString("full_name"));
                    row.put("status", rs.getString("status"));
                    row.put("markedVia", rs.getString("marked_via"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------- dashboard figures
    /** Present(+late) and total records per day for the last N days. */
    public List<DailyStat> trend(int days) throws SQLException {
        String sql = "SELECT att_date, SUM(status IN ('PRESENT','LATE')) AS p, COUNT(*) AS t FROM attendance "
                   + "WHERE att_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) GROUP BY att_date ORDER BY att_date";
        List<DailyStat> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new DailyStat(rs.getDate("att_date"), rs.getInt("p"), rs.getInt("t")));
            }
        }
        return list;
    }

    /** Overall percentage across the whole college (0 if there is no data). */
    public double overallPercentage() throws SQLException {
        String sql = "SELECT ROUND(100 * SUM(status IN ('PRESENT','LATE')) / NULLIF(COUNT(*),0), 1) FROM attendance";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public int countRecords() throws SQLException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM attendance");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
