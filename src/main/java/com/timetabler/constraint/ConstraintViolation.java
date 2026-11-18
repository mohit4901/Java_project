package com.timetabler.constraint;

/**
 * Details of a constraint violation (hard or soft).
 */
public class ConstraintViolation {
    public enum Type {
        HARD,
        SOFT
    }

    private final Type type;
    private final String ruleName;
    private final String description;
    private final int penaltyWeight;

    public ConstraintViolation(Type type, String ruleName, String description, int penaltyWeight) {
        this.type = type;
        this.ruleName = ruleName;
        this.description = description;
        this.penaltyWeight = penaltyWeight;
    }

    public Type getType() {
        return type;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDescription() {
        return description;
    }

    public int getPenaltyWeight() {
        return penaltyWeight;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + ruleName + ": " + description;
    }
}
