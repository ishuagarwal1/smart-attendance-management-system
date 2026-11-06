package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.CourseDAO;
import com.sams.model.Course;
import com.sams.model.StudentInfo;
import com.sams.util.WebUtil;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Teacher: take (or correct) attendance of one course for one date. */
@WebServlet("/teacher/attendance")
public class TeacherAttendanceServlet extends BaseServlet {
    private final CourseDAO courses = new CourseDAO();
    private final AttendanceDAO attendance = new AttendanceDAO();

    /** Loads the course and checks that it belongs to the logged-in teacher. */
    private Course ownCourse(HttpServletRequest req, HttpServletResponse resp, String param) throws Exception {
        Course c = courses.findById(WebUtil.parseLong(req.getParameter(param), -1));
        if (c == null) { resp.sendError(404, "Course not found"); return null; }
        if (c.getTeacherId() == null || c.getTeacherId() != user(req).getId()) {
            resp.sendError(403, "This course is not assigned to you.");
            return null;
        }
        return c;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Course c = ownCourse(req, resp, "courseId");
        if (c == null) return;
        LocalDate today = LocalDate.now();
        LocalDate date = WebUtil.parseDate(req.getParameter("date"), today);
        if (date.isAfter(today)) date = today;

        List<StudentInfo> students = courses.findEnrolled(c.getId());
        req.setAttribute("course", c);
        req.setAttribute("students", students);
        req.setAttribute("date", date.toString());
        req.setAttribute("today", today.toString());
        req.setAttribute("existing", attendance.findByCourseAndDate(c.getId(), WebUtil.sqlDate(date)));
        render(req, resp, "teacher/attendance", "Mark attendance - " + c.getCode());
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Course c = ownCourse(req, resp, "courseId");
        if (c == null) return;
        LocalDate today = LocalDate.now();
        LocalDate date = WebUtil.parseDate(req.getParameter("date"), null);
        if (date == null || date.isAfter(today)) {
            WebUtil.flash(req, "error", "Choose a valid date (not in the future).");
            redirect(req, resp, "/teacher/attendance?courseId=" + c.getId());
            return;
        }
        Map<Long, String> statuses = new LinkedHashMap<>();
        Map<Long, String> remarks = new LinkedHashMap<>();
        for (StudentInfo s : courses.findEnrolled(c.getId())) {
            String st = WebUtil.trim(req.getParameter("status_" + s.getUserId()));
            if (!AttendanceDAO.isValidStatus(st)) {
                WebUtil.flash(req, "error", "Please mark every student before saving (missing: " + s.getFullName() + ").");
                redirect(req, resp, "/teacher/attendance?courseId=" + c.getId() + "&date=" + date);
                return;
            }
            statuses.put(s.getUserId(), st);
            String rem = WebUtil.trim(req.getParameter("remark_" + s.getUserId()));
            remarks.put(s.getUserId(), rem.length() > 200 ? rem.substring(0, 200) : rem);
        }
        if (statuses.isEmpty()) {
            WebUtil.flash(req, "error", "No students are enrolled in this course yet.");
        } else {
            int n = attendance.saveBatch(c.getId(), WebUtil.sqlDate(date), statuses, remarks, user(req).getId(), "WEB");
            log(req, "ATTENDANCE_SAVED", c.getCode() + " " + date + " students=" + n);
            WebUtil.flash(req, "success", "Attendance for " + c.getCode() + " on " + date + " saved (" + n + " students).");
        }
        redirect(req, resp, "/teacher/attendance?courseId=" + c.getId() + "&date=" + date);
    }
}
