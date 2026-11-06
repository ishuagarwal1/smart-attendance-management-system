package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.AuditDAO;
import com.sams.util.Config;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin: defaulter list (below a chosen %) and the audit trail. */
@WebServlet("/admin/reports")
public class AdminReportServlet extends BaseServlet {
    private final AttendanceDAO attendance = new AttendanceDAO();
    private final AuditDAO auditDao = new AuditDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        double t = Math.max(1, Math.min(100, WebUtil.parseLong(req.getParameter("below"), (long) Config.threshold())));
        req.setAttribute("below", (int) t);
        req.setAttribute("defaulters", attendance.belowThreshold(t));
        req.setAttribute("audit", auditDao.recent(25));
        render(req, resp, "admin/reports", "Reports & audit log");
    }
}
