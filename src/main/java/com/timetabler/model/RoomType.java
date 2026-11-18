package com.timetabler.model;

/**
 * Types of educational spaces.
 */
public enum RoomType {
    LECTURE_HALL("Lecture Hall"),
    CLASSROOM("Standard Classroom"),
    COMPUTER_LAB("Computer Lab"),
    SCIENCE_LAB("Science/Electronics Lab");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLab() {
        return this == COMPUTER_LAB || this == SCIENCE_LAB;
    }
}
