package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.StudentDAO;
import com.sams.model.AttendanceSummary;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Student home: subject-wise attendance percentage with shortage warnings. */
@WebServlet("/student/dashboard")
public class StudentDashboardServlet extends BaseServlet {
    private final AttendanceDAO attendance = new AttendanceDAO();
    private final StudentDAO students = new StudentDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long id = user(req).getId();
        List<AttendanceSummary> rows = attendance.studentSummary(id);
        int total = 0, attended = 0, low = 0;
        for (AttendanceSummary s : rows) {
            total += s.getTotal();
            attended += s.getPresent() + s.getLate();
            if (s.isLow()) low++;
        }
        req.setAttribute("profile", students.findById(id));
        req.setAttribute("rows", rows);
        req.setAttribute("overall", total == 0 ? 0.0 : Math.round(1000.0 * attended / total) / 10.0);
        req.setAttribute("lowCount", low);
        render(req, resp, "student/dashboard", "My attendance");
    }
}
