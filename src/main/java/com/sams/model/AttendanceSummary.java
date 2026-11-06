package com.sams.model;

/** Aggregated attendance of one student in one course. */
public class AttendanceSummary {
    private long courseId;
    private String courseCode;
    private String courseName;
    private long studentId;
    private String rollNo;
    private String studentName;
    private int total;
    private int present;
    private int late;
    private int absent;
    private double percentage;
    private double threshold = 75.0;

    public long getCourseId() { return courseId; }
    public void setCourseId(long courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPresent() { return present; }
    public void setPresent(int present) { this.present = present; }
    public int getLate() { return late; }
    public void setLate(int late) { this.late = late; }
    public int getAbsent() { return absent; }
    public void setAbsent(int absent) { this.absent = absent; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    /** True when the student is below the minimum required percentage. */
    public boolean isLow() { return total > 0 && percentage < threshold; }

    /** CSS class used by the progress bar: good / ok / low / none. */
    public String getLevel() {
        if (total == 0) return "none";
        if (percentage >= 85) return "good";
        if (percentage >= threshold) return "ok";
        return "low";
    }

    /** Classes that must be attended in a row to climb back to the threshold. */
    public int getClassesNeeded() {
        if (!isLow()) return 0;
        int attended = present + late;
        // (attended + x) / (total + x) >= t/100  =>  x >= (t*total - 100*attended) / (100 - t)
        double x = (threshold * total - 100.0 * attended) / (100.0 - threshold);
        return (int) Math.ceil(x);
    }
}
