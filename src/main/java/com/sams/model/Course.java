package com.sams.model;

import java.sql.Date;

/** A subject / course taught by one teacher. */
public class Course {
    private long id;
    private String code;
    private String name;
    private String department;
    private int semester;
    private int credits;
    private Long teacherId;
    private String teacherName;
    private int enrolledCount;
    private int classesHeld;
    private double avgPercentage;
    private Date lastMarked;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
    public int getClassesHeld() { return classesHeld; }
    public void setClassesHeld(int classesHeld) { this.classesHeld = classesHeld; }
    public double getAvgPercentage() { return avgPercentage; }
    public void setAvgPercentage(double avgPercentage) { this.avgPercentage = avgPercentage; }
    public Date getLastMarked() { return lastMarked; }
    public void setLastMarked(Date lastMarked) { this.lastMarked = lastMarked; }
}
