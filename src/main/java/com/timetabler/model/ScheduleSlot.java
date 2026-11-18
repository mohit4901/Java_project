package com.timetabler.model;

import java.util.Objects;

/**
 * An assigned entry in the timetable: Session + TimeSlot + Room + Instructor.
 */
public class ScheduleSlot {
    private final ClassSession session;
    private final TimeSlot timeSlot;
    private final Room room;
    private final Instructor instructor;

    public ScheduleSlot(ClassSession session, TimeSlot timeSlot, Room room, Instructor instructor) {
        this.session = session;
        this.timeSlot = timeSlot;
        this.room = room;
        this.instructor = instructor;
    }

    public ClassSession getSession() {
        return session;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public Room getRoom() {
        return room;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleSlot that = (ScheduleSlot) o;
        return Objects.equals(session, that.session) &&
               Objects.equals(timeSlot, that.timeSlot) &&
               Objects.equals(room, that.room) &&
               Objects.equals(instructor, that.instructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, timeSlot, room, instructor);
    }

    @Override
    public String toString() {
        return "Slot[" + session.getId() + " at " + timeSlot + " in " + room.getName() + " by " + instructor.getName() + "]";
    }
}
