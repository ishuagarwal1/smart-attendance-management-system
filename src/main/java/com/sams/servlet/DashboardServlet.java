package com.sams.servlet;

import com.sams.model.User;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** /dashboard sends each person to the home page of their role. */
@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User u = user(req);
        String target = u.isAdmin() ? "/admin/dashboard" : u.isTeacher() ? "/teacher/dashboard" : "/student/dashboard";
        redirect(req, resp, target);
    }
}
