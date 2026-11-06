package com.sams.servlet;

import com.sams.dao.AttendanceDAO;
import com.sams.dao.CourseDAO;
import com.sams.model.AttendanceSummary;
import com.sams.model.Course;
import com.sams.util.WebUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Teacher: course-wise attendance report on screen (/teacher/report) or as CSV (/teacher/export). */
@WebServlet(urlPatterns = {"/teacher/report", "/teacher/export"})
public class TeacherReportServlet extends BaseServlet {
    private final CourseDAO courses = new CourseDAO();
    private final AttendanceDAO attendance = new AttendanceDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Course c = courses.findById(WebUtil.parseLong(req.getParameter("courseId"), -1));
        if (c == null) { resp.sendError(404, "Course not found"); return; }
        if (c.getTeacherId() == null || c.getTeacherId() != user(req).getId()) {
            resp.sendError(403, "This course is not assigned to you.");
            return;
        }
        List<AttendanceSummary> rows = attendance.courseSummary(c.getId());

        if (req.getServletPath().endsWith("/export")) {
            writeCsv(resp, c, rows);
            log(req, "REPORT_EXPORTED", c.getCode());
            return;
        }
        int low = 0;
        for (AttendanceSummary s : rows) if (s.isLow()) low++;
        req.setAttribute("course", c);
        req.setAttribute("rows", rows);
        req.setAttribute("lowCount", low);
        render(req, resp, "teacher/report", "Report - " + c.getCode());
    }

    private void writeCsv(HttpServletResponse resp, Course c, List<AttendanceSummary> rows) throws IOException {
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"attendance_" + c.getCode() + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write('\uFEFF');                                   // BOM so Excel reads UTF-8 correctly
        w.println("Roll No,Student,Total Classes,Present,Late,Absent,Percentage,Status");
        for (AttendanceSummary s : rows) {
            w.println(WebUtil.csv(s.getRollNo()) + "," + WebUtil.csv(s.getStudentName()) + "," + s.getTotal() + ","
                    + s.getPresent() + "," + s.getLate() + "," + s.getAbsent() + "," + s.getPercentage() + ","
                    + (s.getTotal() == 0 ? "NO DATA" : s.isLow() ? "SHORTAGE" : "OK"));
        }
    }
}
