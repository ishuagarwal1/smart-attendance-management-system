package com.sams.servlet;

import com.sams.dao.CourseDAO;
import com.sams.model.Course;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin: choose which students belong to a course. */
@WebServlet("/admin/enrollments")
public class AdminEnrollmentServlet extends BaseServlet {
    private final CourseDAO courses = new CourseDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Course c = courses.findById(WebUtil.parseLong(req.getParameter("courseId"), -1));
        if (c == null) { resp.sendError(404, "Course not found"); return; }
        req.setAttribute("course", c);
        req.setAttribute("enrolled", courses.findEnrolled(c.getId()));
        req.setAttribute("available", courses.findNotEnrolled(c.getId()));
        render(req, resp, "admin/enrollments", "Enrollments - " + c.getCode());
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long courseId = WebUtil.parseLong(req.getParameter("courseId"), -1);
        Course c = courses.findById(courseId);
        if (c == null) { resp.sendError(404); return; }
        String action = WebUtil.trim(req.getParameter("action"));

        if ("enroll".equals(action)) {
            String[] ids = req.getParameterValues("studentId");
            int n = 0;
            if (ids != null) for (String s : ids) if (courses.enroll(courseId, WebUtil.parseLong(s, -1))) n++;
            log(req, "ENROLLED", c.getCode() + " +" + n);
            WebUtil.flash(req, "success", n + " student(s) enrolled in " + c.getCode() + ".");
        } else if ("remove".equals(action)) {
            courses.unenroll(courseId, WebUtil.parseLong(req.getParameter("studentId"), -1));
            log(req, "UNENROLLED", c.getCode());
            WebUtil.flash(req, "success", "Student removed from " + c.getCode() + ".");
        } else { resp.sendError(400); return; }
        redirect(req, resp, "/admin/enrollments?courseId=" + courseId);
    }
}
