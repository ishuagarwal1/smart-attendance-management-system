package com.sams.servlet;

import com.sams.dao.ApiKeyDAO;
import com.sams.util.ApiKeyUtil;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin: issue, revoke and delete API keys for devices / external systems. */
@WebServlet("/admin/apikeys")
public class AdminApiKeyServlet extends BaseServlet {
    private final ApiKeyDAO keys = new ApiKeyDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // a freshly generated key is shown exactly once, then removed from the session
        Object fresh = req.getSession().getAttribute("newApiKey");
        if (fresh != null) {
            req.setAttribute("newApiKey", fresh);
            req.getSession().removeAttribute("newApiKey");
        }
        req.setAttribute("keys", keys.findAll());
        render(req, resp, "admin/apikeys", "API keys");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = WebUtil.trim(req.getParameter("action"));
        if ("create".equals(action)) {
            String label = WebUtil.trim(req.getParameter("label"));
            if (label.isEmpty() || label.length() > 80) {
                WebUtil.flash(req, "error", "Enter a label (max 80 characters), e.g. 'Room 204 RFID reader'.");
            } else {
                String key = ApiKeyUtil.generate();
                keys.create(label, ApiKeyUtil.prefix(key), ApiKeyUtil.sha256(key), user(req).getId());
                req.getSession().setAttribute("newApiKey", key);
                log(req, "APIKEY_CREATED", label);
                WebUtil.flash(req, "success", "API key created. Copy it now - it will not be shown again.");
            }
        } else if ("toggle".equals(action)) {
            keys.setActive(WebUtil.parseLong(req.getParameter("id"), -1), "1".equals(req.getParameter("active")));
            log(req, "APIKEY_TOGGLED", "id=" + req.getParameter("id"));
            WebUtil.flash(req, "success", "API key updated.");
        } else if ("delete".equals(action)) {
            keys.delete(WebUtil.parseLong(req.getParameter("id"), -1));
            log(req, "APIKEY_DELETED", "id=" + req.getParameter("id"));
            WebUtil.flash(req, "success", "API key deleted.");
        } else { resp.sendError(400); return; }
        redirect(req, resp, "/admin/apikeys");
    }
}
