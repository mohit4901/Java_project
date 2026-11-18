package com.timetabler.model;

import java.util.Objects;

/**
 * Represents an academic course/subject.
 */
public class Course {
    private final String id;
    private final String code;
    private final String name;
    private final int weeklyLectures;
    private final boolean requiresLab;
    private final RoomType requiredRoomType;

    public Course(String id, String code, String name, int weeklyLectures, boolean requiresLab, RoomType requiredRoomType) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.weeklyLectures = weeklyLectures;
        this.requiresLab = requiresLab;
        this.requiredRoomType = requiredRoomType;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getWeeklyLectures() {
        return weeklyLectures;
    }

    public boolean requiresLab() {
        return requiresLab;
    }

    public RoomType getRequiredRoomType() {
        return requiredRoomType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return id.equals(course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return code + " - " + name + (requiresLab ? " (Lab)" : "");
    }
}
