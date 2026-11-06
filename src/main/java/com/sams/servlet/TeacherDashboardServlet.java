package com.sams.servlet;

import com.sams.dao.CourseDAO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Teacher home: the courses assigned to this teacher with quick statistics. */
@WebServlet("/teacher/dashboard")
public class TeacherDashboardServlet extends BaseServlet {
    private final CourseDAO courses = new CourseDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.setAttribute("courses", courses.findByTeacher(user(req).getId()));
        render(req, resp, "teacher/dashboard", "My courses");
    }
}
