package com.sams.model;

import java.sql.Date;

/** One attendance entry: a student, a course, a day and a status. */
public class AttendanceRecord {
    private long id;
    private long courseId;
    private long studentId;
    private Date date;
    private String status;
    private String remarks;
    private String markedVia;
    private String markedByName;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCourseId() { return courseId; }
    public void setCourseId(long courseId) { this.courseId = courseId; }
    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getMarkedVia() { return markedVia; }
    public void setMarkedVia(String markedVia) { this.markedVia = markedVia; }
    public String getMarkedByName() { return markedByName; }
    public void setMarkedByName(String markedByName) { this.markedByName = markedByName; }
}
