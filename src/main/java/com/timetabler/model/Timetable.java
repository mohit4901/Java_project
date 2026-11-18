package com.timetabler.model;

import java.util.*;

/**
 * Represents a complete scheduled timetable solution along with its evaluation metrics.
 */
public class Timetable {
    private final List<ScheduleSlot> slots;
    private double fitness;
    private int hardConflicts;
    private int softConflicts;
    private final List<String> diagnosticViolations;

    public Timetable(List<ScheduleSlot> slots) {
        this.slots = new ArrayList<>(slots);
        this.fitness = 0.0;
        this.hardConflicts = 0;
        this.softConflicts = 0;
        this.diagnosticViolations = new ArrayList<>();
    }

    public List<ScheduleSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /**
     * Updates an existing session's scheduled slot.
     */
    public synchronized boolean updateSlot(String sessionId, TimeSlot newTimeSlot, Room newRoom, Instructor newInstructor) {
        for (int i = 0; i < slots.size(); i++) {
            ScheduleSlot current = slots.get(i);
            if (current.getSession().getId().equals(sessionId)) {
                slots.set(i, new ScheduleSlot(current.getSession(), newTimeSlot, newRoom, newInstructor));
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a scheduled session.
     */
    public synchronized boolean removeSlot(String sessionId) {
        return slots.removeIf(s -> s.getSession().getId().equals(sessionId));
    }

    /**
     * Adds a newly scheduled slot.
     */
    public synchronized void addSlot(ScheduleSlot slot) {
        slots.removeIf(s -> s.getSession().getId().equals(slot.getSession().getId()));
        slots.add(slot);
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public int getHardConflicts() {
        return hardConflicts;
    }

    public void setHardConflicts(int hardConflicts) {
        this.hardConflicts = hardConflicts;
    }

    public int getSoftConflicts() {
        return softConflicts;
    }

    public void setSoftConflicts(int softConflicts) {
        this.softConflicts = softConflicts;
    }

    public List<String> getDiagnosticViolations() {
        return Collections.unmodifiableList(diagnosticViolations);
    }

    public void setDiagnosticViolations(List<String> diagnosticViolations) {
        this.diagnosticViolations.clear();
        if (diagnosticViolations != null) {
            this.diagnosticViolations.addAll(diagnosticViolations);
        }
    }

    public boolean isValid() {
        return hardConflicts == 0;
    }

    /**
     * Get slots for a specific student group sorted chronologically.
     */
    public List<ScheduleSlot> getSlotsForGroup(String groupId) {
        List<ScheduleSlot> result = new ArrayList<>();
        for (ScheduleSlot s : slots) {
            if (s.getSession().getStudentGroupId().equals(groupId)) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(ScheduleSlot::getTimeSlot));
        return result;
    }

    /**
     * Get slots for a specific instructor sorted chronologically.
     */
    public List<ScheduleSlot> getSlotsForInstructor(String instructorId) {
        List<ScheduleSlot> result = new ArrayList<>();
        for (ScheduleSlot s : slots) {
            if (s.getInstructor().getId().equals(instructorId)) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(ScheduleSlot::getTimeSlot));
        return result;
    }

    /**
     * Get slots for a specific room sorted chronologically.
     */
    public List<ScheduleSlot> getSlotsForRoom(String roomId) {
        List<ScheduleSlot> result = new ArrayList<>();
        for (ScheduleSlot s : slots) {
            if (s.getRoom().getId().equals(roomId)) {
                result.add(s);
            }
        }
        result.sort(Comparator.comparing(ScheduleSlot::getTimeSlot));
        return result;
    }
}
