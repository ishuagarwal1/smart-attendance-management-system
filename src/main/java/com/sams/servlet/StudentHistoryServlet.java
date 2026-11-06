package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.CourseDAO;
import com.sams.model.Course;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Student: day-by-day attendance in one course. */
@WebServlet("/student/history")
public class StudentHistoryServlet extends BaseServlet {
    private final AttendanceDAO attendance = new AttendanceDAO();
    private final CourseDAO courses = new CourseDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long uid = user(req).getId();
        Course c = courses.findById(WebUtil.parseLong(req.getParameter("courseId"), -1));
        if (c == null || !courses.isEnrolled(c.getId(), uid)) { resp.sendError(404, "Course not found"); return; }
        req.setAttribute("course", c);
        req.setAttribute("records", attendance.history(uid, c.getId()));
        req.setAttribute("summary", attendance.studentSummary(uid).stream()
                .filter(s -> s.getCourseId() == c.getId()).findFirst().orElse(null));
        render(req, resp, "student/history", "History - " + c.getCode());
    }
}
