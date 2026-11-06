package com.sams.servlet;

import com.sams.dao.UserDAO;
import com.sams.model.User;
import com.sams.util.PasswordUtil;
import com.sams.util.WebUtil;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Login form + authentication, with a simple brute-force lock-out (5 failures = 5 minutes). */
@WebServlet(urlPatterns = {"/login", ""})
public class LoginServlet extends BaseServlet {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 5 * 60 * 1000L;
    private static final Map<String, long[]> FAILS = new ConcurrentHashMap<>();   // key -> {count, lockedUntil}
    private final UserDAO users = new UserDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (user(req) != null) { redirect(req, resp, "/dashboard"); return; }
        if (req.getParameter("expired") != null) req.setAttribute("info", "Please sign in to continue.");
        if (req.getParameter("loggedout") != null) req.setAttribute("info", "You have been signed out.");
        render(req, resp, "common/login", "Sign in");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String username = WebUtil.trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String key = username.toLowerCase() + "|" + WebUtil.clientIp(req);

        long[] f = FAILS.get(key);
        if (f != null && f[1] > System.currentTimeMillis()) {
            req.setAttribute("error", "Too many failed attempts. Try again in a few minutes.");
            req.setAttribute("username", username);
            render(req, resp, "common/login", "Sign in");
            return;
        }

        User u = users.findByUsername(username);
        boolean ok = u != null && u.isActive() && PasswordUtil.verify(password, u.getPasswordHash());
        if (!ok) {
            f = FAILS.computeIfAbsent(key, k -> new long[]{0, 0});
            if (++f[0] >= MAX_FAILS) { f[1] = System.currentTimeMillis() + LOCK_MS; f[0] = 0; }
            audit.log(u == null ? null : u.getId(), "LOGIN_FAILED", "username=" + username, WebUtil.clientIp(req));
            req.setAttribute("error", (u != null && !u.isActive()) ? "This account has been deactivated."
                                                                  : "Invalid username or password.");
            req.setAttribute("username", username);
            render(req, resp, "common/login", "Sign in");
            return;
        }
        FAILS.remove(key);

        // session fixation protection: throw away the old session, start a fresh one
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        u.setPasswordHash(null);                  // never keep the hash in the session
        session.setAttribute("user", u);
        // CSRFfilter will create the new token on the next request

        users.touchLogin(u.getId());
        audit.log(u.getId(), "LOGIN", "role=" + u.getRole(), WebUtil.clientIp(req));
        redirect(req, resp, "/dashboard");
    }
}
