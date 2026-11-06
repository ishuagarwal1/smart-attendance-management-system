package com.sams.servlet;

import com.sams.dao.CourseDAO;
import com.sams.dao.UserDAO;
import com.sams.model.Course;
import com.sams.util.WebUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin: create / delete courses and assign a teacher. */
@WebServlet("/admin/courses")
public class AdminCourseServlet extends BaseServlet {
    private final CourseDAO courses = new CourseDAO();
    private final UserDAO users = new UserDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.setAttribute("courses", courses.findAll());
        req.setAttribute("teachers", users.findActiveByRole("TEACHER"));
        render(req, resp, "admin/courses", "Courses");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String action = WebUtil.trim(req.getParameter("action"));
        switch (action) {
            case "create": {
                Course c = new Course();
                c.setCode(WebUtil.trim(req.getParameter("code")).toUpperCase());
                c.setName(WebUtil.trim(req.getParameter("name")));
                c.setDepartment(WebUtil.trim(req.getParameter("department")));
                c.setSemester((int) WebUtil.parseLong(req.getParameter("semester"), 1));
                c.setCredits((int) WebUtil.parseLong(req.getParameter("credits"), 3));
                long t = WebUtil.parseLong(req.getParameter("teacherId"), -1);
                c.setTeacherId(t > 0 ? t : null);
                if (c.getCode().isEmpty() || c.getName().isEmpty() || c.getDepartment().isEmpty()) {
                    WebUtil.flash(req, "error", "Code, name and department are required.");
                } else if (courses.findByCode(c.getCode()) != null) {
                    WebUtil.flash(req, "error", "A course with code " + c.getCode() + " already exists.");
                } else {
                    courses.create(c);
                    log(req, "COURSE_CREATED", c.getCode());
                    WebUtil.flash(req, "success", "Course " + c.getCode() + " created.");
                }
                break;
            }
            case "assign": {
                long id = WebUtil.parseLong(req.getParameter("courseId"), -1);
                long t = WebUtil.parseLong(req.getParameter("teacherId"), -1);
                courses.assignTeacher(id, t > 0 ? t : null);
                log(req, "COURSE_ASSIGNED", "course=" + id + " teacher=" + t);
                WebUtil.flash(req, "success", "Teacher assignment updated.");
                break;
            }
            case "delete": {
                long id = WebUtil.parseLong(req.getParameter("courseId"), -1);
                Course c = courses.findById(id);
                if (c != null) {
                    courses.delete(id);
                    log(req, "COURSE_DELETED", c.getCode());
                    WebUtil.flash(req, "success", "Course " + c.getCode() + " and its attendance were deleted.");
                }
                break;
            }
            default:
                resp.sendError(400);
                return;
        }
        redirect(req, resp, "/admin/courses");
    }
}
