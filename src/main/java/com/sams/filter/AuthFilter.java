package com.sams.filter;

import com.sams.model.User;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Login + role check for every protected area: /admin/*, /teacher/*, /student/*, /account/*, /dashboard. */
@WebFilter(urlPatterns = {"/admin/*", "/teacher/*", "/student/*", "/account/*", "/dashboard"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?expired=1");
            return;
        }
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean allowed =
            (path.startsWith("/admin/")   && user.isAdmin())   ||
            (path.startsWith("/teacher/") && user.isTeacher()) ||
            (path.startsWith("/student/") && user.isStudent()) ||
            path.startsWith("/account/") || path.equals("/dashboard");
        if (!allowed) {
            resp.sendError(403, "You do not have permission to open this page.");
            return;
        }
        chain.doFilter(rq, rs);
    }
}
