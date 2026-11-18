package com.timetabler.model;

import java.util.Objects;

/**
 * Represents an academic time slot (e.g. Monday 09:00 - 10:00).
 */
public class TimeSlot implements Comparable<TimeSlot> {
    private final String id;
    private final DayOfWeek day;
    private final int slotIndex; // 0, 1, 2, ... within the day
    private final String startTime;
    private final String endTime;

    public TimeSlot(String id, DayOfWeek day, int slotIndex, String startTime, String endTime) {
        this.id = id;
        this.day = day;
        this.slotIndex = slotIndex;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    @Override
    public int compareTo(TimeSlot o) {
        int dayComp = this.day.compareTo(o.day);
        if (dayComp != 0) return dayComp;
        return Integer.compare(this.slotIndex, o.slotIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return id.equals(timeSlot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return day.getShortName() + " " + getTimeRange();
    }
}
