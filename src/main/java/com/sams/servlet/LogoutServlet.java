package com.sams.servlet;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Ends the session (POST only, protected by the CSRF token). */
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        log(req, "LOGOUT", null);
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        redirect(req, resp, "/login?loggedout=1");
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        redirect(req, resp, "/login");
    }
}
