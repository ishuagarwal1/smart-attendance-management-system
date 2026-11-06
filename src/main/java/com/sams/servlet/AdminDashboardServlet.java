package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.CourseDAO;
import com.sams.dao.UserDAO;
import com.sams.util.Config;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin home: headline numbers, 14-day attendance trend and the first defaulters. */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends BaseServlet {
    private final UserDAO users = new UserDAO();
    private final CourseDAO courses = new CourseDAO();
    private final AttendanceDAO attendance = new AttendanceDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, Integer> counts = users.countByRole();
        req.setAttribute("studentCount", counts.get("STUDENT"));
        req.setAttribute("teacherCount", counts.get("TEACHER"));
        req.setAttribute("courseCount", courses.findAll().size());
        req.setAttribute("overall", attendance.overallPercentage());
        req.setAttribute("recordCount", attendance.countRecords());
        req.setAttribute("trend", attendance.trend(14));
        req.setAttribute("defaulters", attendance.belowThreshold(Config.threshold()));
        render(req, resp, "admin/dashboard", "Admin dashboard");
    }
}
