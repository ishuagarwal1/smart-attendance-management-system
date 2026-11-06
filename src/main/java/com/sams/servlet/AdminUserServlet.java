package com.sams.servlet;

import com.sams.dao.UserDAO;
import com.sams.model.StudentInfo;
import com.sams.model.User;
import com.sams.util.PasswordUtil;
import com.sams.util.WebUtil;
import java.util.regex.Pattern;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin: list users, create teachers / students / admins, activate or deactivate accounts. */
@WebServlet("/admin/users")
public class AdminUserServlet extends BaseServlet {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private final UserDAO users = new UserDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.setAttribute("users", users.findAll());
        render(req, resp, "admin/users", "Users");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = WebUtil.trim(req.getParameter("action"));
        if ("toggle".equals(action)) {
            long id = WebUtil.parseLong(req.getParameter("id"), -1);
            User target = users.findById(id);
            if (target == null) WebUtil.flash(req, "error", "User not found.");
            else if (target.getId() == user(req).getId()) WebUtil.flash(req, "error", "You cannot deactivate your own account.");
            else {
                users.setActive(id, !target.isActive());
                log(req, target.isActive() ? "USER_DEACTIVATED" : "USER_ACTIVATED", target.getUsername());
                WebUtil.flash(req, "success", target.getUsername() + (target.isActive() ? " deactivated." : " activated."));
            }
            redirect(req, resp, "/admin/users");
            return;
        }
        if (!"create".equals(action)) { resp.sendError(400); return; }

        User u = new User();
        u.setUsername(WebUtil.trim(req.getParameter("username")));
        u.setFullName(WebUtil.trim(req.getParameter("fullName")));
        u.setEmail(WebUtil.trim(req.getParameter("email")).toLowerCase());
        u.setRole(WebUtil.trim(req.getParameter("role")));
        String password = req.getParameter("password");
        StudentInfo p = new StudentInfo();
        p.setRollNo(WebUtil.trim(req.getParameter("rollNo")));
        p.setDepartment(WebUtil.trim(req.getParameter("department")));
        p.setSemester((int) WebUtil.parseLong(req.getParameter("semester"), 1));
        p.setPhone(WebUtil.trim(req.getParameter("phone")));

        String err = null;
        if (!USERNAME.matcher(u.getUsername()).matches()) err = "Username must be 3-30 letters, digits, dot, dash or underscore.";
        else if (u.getFullName().isEmpty()) err = "Full name is required.";
        else if (!EMAIL.matcher(u.getEmail()).matches()) err = "Enter a valid e-mail address.";
        else if (!("ADMIN".equals(u.getRole()) || "TEACHER".equals(u.getRole()) || "STUDENT".equals(u.getRole()))) err = "Choose a role.";
        else if ((err = PasswordUtil.validateStrength(password)) != null) { /* message set */ }
        else if (users.usernameExists(u.getUsername())) err = "That username is already taken.";
        else if (users.emailExists(u.getEmail())) err = "That e-mail is already registered.";
        else if (u.isStudent()) {
            if (p.getRollNo().isEmpty() || p.getDepartment().isEmpty()) err = "Roll number and department are required for students.";
            else if (p.getSemester() < 1 || p.getSemester() > 12) err = "Semester must be between 1 and 12.";
            else if (users.rollNoExists(p.getRollNo())) err = "That roll number already exists.";
        }
        if (err != null) {
            WebUtil.flash(req, "error", err);
            redirect(req, resp, "/admin/users");
            return;
        }
        users.create(u, password, p);
        log(req, "USER_CREATED", u.getRole() + ":" + u.getUsername());
        WebUtil.flash(req, "success", "Account '" + u.getUsername() + "' created.");
        redirect(req, resp, "/admin/users");
    }
}
