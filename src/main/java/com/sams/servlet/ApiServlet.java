package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.AuditDAO;
import com.sams.dao.CourseDAO;
import com.sams.dao.StudentDAO;
import com.sams.model.ApiKey;
import com.sams.model.AttendanceSummary;
import com.sams.model.Course;
import com.sams.model.StudentInfo;
import com.sams.util.JsonUtil;
import com.sams.util.WebUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REST API (JSON) for devices and integrations - protected by ApiKeyFilter.
 *
 *   GET  /api/v1/health                          public heartbeat
 *   GET  /api/v1/courses                         list courses
 *   GET  /api/v1/courses/{code}/students         students enrolled in a course
 *   GET  /api/v1/attendance?course=&roll=&from=&to=&limit=
 *   GET  /api/v1/students/{roll}/summary         attendance % per course
 *   POST /api/v1/attendance/mark                 mark attendance (single scan or bulk)
 */
@WebServlet("/api/v1/*")
public class ApiServlet extends HttpServlet {
    private final CourseDAO courses = new CourseDAO();
    private final StudentDAO students = new StudentDAO();
    private final AttendanceDAO attendance = new AttendanceDAO();
    private final AuditDAO audit = new AuditDAO();

    // ------------------------------------------------------------------ GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String[] p = path.replaceAll("^/|/$", "").split("/");
        try {
            if (path.equals("/health")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", "UP");
                m.put("time", new java.sql.Timestamp(System.currentTimeMillis()).toString());
                ok(resp, m);
            } else if (p.length == 1 && p[0].equals("courses")) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Course c : courses.findAll()) out.add(courseMap(c));
                ok(resp, out);
            } else if (p.length == 3 && p[0].equals("courses") && p[2].equals("students")) {
                Course c = courses.findByCode(p[1].toUpperCase());
                if (c == null) { fail(resp, 404, "Unknown course " + p[1]); return; }
                List<Map<String, Object>> out = new ArrayList<>();
                for (StudentInfo s : courses.findEnrolled(c.getId())) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("rollNo", s.getRollNo());
                    m.put("name", s.getFullName());
                    m.put("department", s.getDepartment());
                    m.put("semester", s.getSemester());
                    out.add(m);
                }
                ok(resp, out);
            } else if (p.length == 1 && p[0].equals("attendance")) {
                searchAttendance(req, resp);
            } else if (p.length == 3 && p[0].equals("students") && p[2].equals("summary")) {
                StudentInfo s = students.findByRollNo(p[1]);
                if (s == null) { fail(resp, 404, "Unknown roll number " + p[1]); return; }
                List<Map<String, Object>> out = new ArrayList<>();
                for (AttendanceSummary a : attendance.studentSummary(s.getUserId())) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseCode", a.getCourseCode());
                    m.put("courseName", a.getCourseName());
                    m.put("totalClasses", a.getTotal());
                    m.put("present", a.getPresent());
                    m.put("late", a.getLate());
                    m.put("absent", a.getAbsent());
                    m.put("percentage", a.getPercentage());
                    m.put("shortage", a.isLow());
                    out.add(m);
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rollNo", s.getRollNo());
                m.put("name", s.getFullName());
                m.put("courses", out);
                ok(resp, m);
            } else {
                fail(resp, 404, "Unknown endpoint " + path);
            }
        } catch (SQLException e) {
            getServletContext().log("API database error", e);
            fail(resp, 500, "Database error");
        }
    }

    private void searchAttendance(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Long courseId = null, studentId = null;
        String code = req.getParameter("course"), roll = req.getParameter("roll");
        if (!WebUtil.isBlank(code)) {
            Course c = courses.findByCode(code.trim().toUpperCase());
            if (c == null) { fail(resp, 404, "Unknown course " + code); return; }
            courseId = c.getId();
        }
        if (!WebUtil.isBlank(roll)) {
            StudentInfo s = students.findByRollNo(roll.trim());
            if (s == null) { fail(resp, 404, "Unknown roll number " + roll); return; }
            studentId = s.getUserId();
        }
        LocalDate from = WebUtil.parseDate(req.getParameter("from"), null);
        LocalDate to = WebUtil.parseDate(req.getParameter("to"), null);
        int limit = (int) WebUtil.parseLong(req.getParameter("limit"), 200);
        ok(resp, attendance.search(courseId, studentId, from == null ? null : Date.valueOf(from),
                                   to == null ? null : Date.valueOf(to), limit));
    }

    // ----------------------------------------------------------------- POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if (!path.equals("/attendance/mark")) { fail(resp, 404, "Unknown endpoint " + path); return; }
        try {
            Object parsed;
            try { parsed = JsonUtil.parse(readBody(req)); }
            catch (RuntimeException e) { fail(resp, 400, "Invalid JSON: " + e.getMessage()); return; }
            if (!(parsed instanceof Map)) { fail(resp, 400, "Body must be a JSON object"); return; }
            Map<?, ?> body = (Map<?, ?>) parsed;

            Course c = courses.findByCode(str(body.get("course")).toUpperCase());
            if (c == null) { fail(resp, 404, "Unknown course '" + str(body.get("course")) + "'"); return; }

            LocalDate date = body.get("date") == null ? LocalDate.now() : WebUtil.parseDate(str(body.get("date")), null);
            if (date == null || date.isAfter(LocalDate.now())) { fail(resp, 400, "'date' must be YYYY-MM-DD and not in the future"); return; }

            // accept either {"records":[{roll,status},...]} or a single {"roll":..,"status":..}
            List<?> records;
            if (body.get("records") instanceof List) records = (List<?>) body.get("records");
            else if (body.get("roll") != null) records = java.util.Collections.singletonList(body);
            else { fail(resp, 400, "Send 'records' (array) or 'roll' + 'status'"); return; }
            if (records.isEmpty() || records.size() > 500) { fail(resp, 400, "Between 1 and 500 records are allowed"); return; }

            Map<Long, String> toSave = new LinkedHashMap<>();
            List<Map<String, Object>> rejected = new ArrayList<>();
            for (Object o : records) {
                Map<?, ?> r = o instanceof Map ? (Map<?, ?>) o : new LinkedHashMap<>();
                String roll = str(r.get("roll"));
                String status = str(r.get("status")).toUpperCase();
                StudentInfo s = roll.isEmpty() ? null : students.findByRollNo(roll);
                String reason = null;
                if (s == null) reason = "unknown roll number";
                else if (!AttendanceDAO.isValidStatus(status)) reason = "status must be PRESENT, ABSENT or LATE";
                else if (!courses.isEnrolled(c.getId(), s.getUserId())) reason = "student not enrolled in " + c.getCode();
                if (reason != null) {
                    Map<String, Object> rj = new LinkedHashMap<>();
                    rj.put("roll", roll);
                    rj.put("reason", reason);
                    rejected.add(rj);
                } else {
                    toSave.put(s.getUserId(), status);
                }
            }
            int saved = toSave.isEmpty() ? 0 : attendance.saveBatch(c.getId(), Date.valueOf(date), toSave, null, 0, "API");
            ApiKey key = (ApiKey) req.getAttribute("apiKey");
            audit.log(null, "API_MARK", "key=" + (key == null ? "?" : key.getLabel()) + " " + c.getCode() + " " + date + " saved=" + saved,
                      WebUtil.clientIp(req));

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("course", c.getCode());
            out.put("date", date.toString());
            out.put("saved", saved);
            out.put("rejected", rejected);
            resp.setStatus(rejected.isEmpty() ? 200 : (saved == 0 ? 422 : 207));
            Map<String, Object> env = new LinkedHashMap<>();
            env.put("success", saved > 0);
            env.put("data", out);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(JsonUtil.toJson(env));
        } catch (SQLException e) {
            getServletContext().log("API database error", e);
            fail(resp, 500, "Database error");
        }
    }

    // -------------------------------------------------------------- helpers
    private static String str(Object o) { return o == null ? "" : o.toString().trim(); }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            char[] buf = new char[2048];
            int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
                if (sb.length() > 200_000) throw new IOException("Request body too large");
            }
        }
        return sb.toString();
    }

    private Map<String, Object> courseMap(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("department", c.getDepartment());
        m.put("semester", c.getSemester());
        m.put("teacher", c.getTeacherName());
        m.put("enrolled", c.getEnrolledCount());
        return m;
    }

    private static void ok(HttpServletResponse resp, Object data) throws IOException {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("success", true);
        env.put("data", data);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(env));
    }

    private static void fail(HttpServletResponse resp, int status, String msg) throws IOException {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("success", false);
        env.put("error", msg);
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(env));
    }
}
