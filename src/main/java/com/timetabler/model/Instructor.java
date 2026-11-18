package com.timetabler.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a teacher / professor who conducts classes.
 */
public class Instructor {
    private final String id;
    private final String name;
    private final String email;
    private final String department;
    private final Set<String> qualifiedCourseIds;
    private final int maxDailyHours;

    public Instructor(String id, String name, String email, String department, int maxDailyHours) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.maxDailyHours = maxDailyHours;
        this.qualifiedCourseIds = new HashSet<>();
    }

    public void addQualifiedCourse(String courseId) {
        this.qualifiedCourseIds.add(courseId);
    }

    public boolean canTeach(String courseId) {
        return qualifiedCourseIds.contains(courseId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public Set<String> getQualifiedCourseIds() {
        return Collections.unmodifiableSet(qualifiedCourseIds);
    }

    public int getMaxDailyHours() {
        return maxDailyHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instructor that = (Instructor) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " [" + department + "]";
    }
}
