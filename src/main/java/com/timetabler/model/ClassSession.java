package com.timetabler.model;

import java.util.Objects;

/**
 * An atomic lecture or lab session to be placed into the timetable.
 */
public class ClassSession {
    private final String id;
    private final String courseId;
    private final String studentGroupId;
    private final int sessionNumber; // e.g. session 1 of 3

    public ClassSession(String id, String courseId, String studentGroupId, int sessionNumber) {
        this.id = id;
        this.courseId = courseId;
        this.studentGroupId = studentGroupId;
        this.sessionNumber = sessionNumber;
    }

    public String getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getStudentGroupId() {
        return studentGroupId;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassSession that = (ClassSession) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Session[" + id + ", course=" + courseId + ", group=" + studentGroupId + "]";
    }
}
