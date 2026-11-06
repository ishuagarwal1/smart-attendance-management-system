package com.sams.servlet;

import com.sams.dao.UserDAO;
import com.sams.model.User;
import com.sams.util.PasswordUtil;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Change-password page available to every logged-in user. */
@WebServlet("/account/password")
public class AccountServlet extends BaseServlet {
    private final UserDAO users = new UserDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        render(req, resp, "common/password", "Change password");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User me = user(req);
        User db = users.findById(me.getId());
        String current = req.getParameter("current");
        String next = req.getParameter("next");
        String confirm = req.getParameter("confirm");

        String err = null;
        if (!PasswordUtil.verify(current, db.getPasswordHash())) err = "Current password is incorrect.";
        else if (next == null || !next.equals(confirm)) err = "New password and confirmation do not match.";
        else err = PasswordUtil.validateStrength(next);

        if (err != null) {
            req.setAttribute("error", err);
            render(req, resp, "common/password", "Change password");
            return;
        }
        users.updatePassword(me.getId(), next);
        log(req, "PASSWORD_CHANGED", null);
        WebUtil.flash(req, "success", "Your password was changed successfully.");
        redirect(req, resp, "/dashboard");
    }
}
