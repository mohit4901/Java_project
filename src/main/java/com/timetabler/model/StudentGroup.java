package com.timetabler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a cohort / batch / section of students (e.g. CS-Year-2, Sec-A).
 */
public class StudentGroup {
    private final String id;
    private final String name;
    private final int numberOfStudents;
    private final List<String> enrolledCourseIds;

    public StudentGroup(String id, String name, int numberOfStudents) {
        this.id = id;
        this.name = name;
        this.numberOfStudents = numberOfStudents;
        this.enrolledCourseIds = new ArrayList<>();
    }

    public void enrollCourse(String courseId) {
        if (!this.enrolledCourseIds.contains(courseId)) {
            this.enrolledCourseIds.add(courseId);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNumberOfStudents() {
        return numberOfStudents;
    }

    public List<String> getEnrolledCourseIds() {
        return Collections.unmodifiableList(enrolledCourseIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentGroup that = (StudentGroup) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + numberOfStudents + " students)";
    }
}
