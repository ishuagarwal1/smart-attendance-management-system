package com.sams.servlet;

import com.sams.dao.AuditDAO;
import com.sams.model.User;
import com.sams.util.WebUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class of all page servlets. Subclasses override get()/post() which may throw
 * any Exception (typically SQLException) - it is converted to a ServletException and
 * shown by the common error page.
 */
public abstract class BaseServlet extends HttpServlet {
    protected final AuditDAO audit = new AuditDAO();

    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        run(req, resp, false);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        run(req, resp, true);
    }

    private void run(HttpServletRequest req, HttpServletResponse resp, boolean isPost) throws ServletException, IOException {
        try {
            if (isPost) post(req, resp); else get(req, resp);
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected User user(HttpServletRequest req) { return WebUtil.currentUser(req); }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected void log(HttpServletRequest req, String action, String details) {
        User u = user(req);
        audit.log(u == null ? null : u.getId(), action, details, WebUtil.clientIp(req));
    }
}
