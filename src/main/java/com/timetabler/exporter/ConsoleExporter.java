package com.timetabler.exporter;

import com.timetabler.model.*;

import java.util.*;

/**
 * Formats timetables into formatted ASCII grid views for the terminal.
 */
public class ConsoleExporter {

    public static void printSummary(Timetable timetable, TimetableData data) {
        System.out.println("==========================================================================");
        System.out.println("                         TIMETABLE SUMMARY                                ");
        System.out.println("==========================================================================");
        System.out.printf(" Status:             %s%n", timetable.isValid() ? "FEASIBLE (0 Hard Conflicts)" : "INFEASIBLE (" + timetable.getHardConflicts() + " Conflicts)");
        System.out.printf(" Fitness Score:      %.4f%n", timetable.getFitness());
        System.out.printf(" Hard Conflicts:     %d%n", timetable.getHardConflicts());
        System.out.printf(" Soft Penalty:       %d%n", timetable.getSoftConflicts());
        System.out.printf(" Total Sessions:     %d scheduled%n", timetable.getSlots().size());
        System.out.printf(" Student Batches:    %d%n", data.getStudentGroups().size());
        System.out.printf(" Instructors:        %d%n", data.getInstructors().size());
        System.out.printf(" Rooms Available:    %d%n", data.getRooms().size());
        System.out.println("==========================================================================");

        if (!timetable.getDiagnosticViolations().isEmpty()) {
            System.out.println("Violations Diagnostic:");
            for (String v : timetable.getDiagnosticViolations()) {
                System.out.println("  * " + v);
            }
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    public static void printGroupTimetable(Timetable timetable, TimetableData data, String groupId) {
        StudentGroup group = data.getStudentGroup(groupId);
        if (group == null) {
            System.out.println("Group not found: " + groupId);
            return;
        }

        System.out.println("\n============================================================================================================");
        System.out.println(" WEEKLY TIMETABLE FOR BATCH: " + group.getName() + " (" + group.getNumberOfStudents() + " Students)");
        System.out.println("============================================================================================================");

        List<ScheduleSlot> groupSlots = timetable.getSlotsForGroup(groupId);
        printGrid(groupSlots, data, false);
    }

    public static void printInstructorTimetable(Timetable timetable, TimetableData data, String instructorId) {
        Instructor inst = data.getInstructor(instructorId);
        if (inst == null) {
            System.out.println("Instructor not found: " + instructorId);
            return;
        }

        System.out.println("\n============================================================================================================");
        System.out.println(" FACULTY SCHEDULE FOR: " + inst.getName() + " [" + inst.getDepartment() + "]");
        System.out.println("============================================================================================================");

        List<ScheduleSlot> instSlots = timetable.getSlotsForInstructor(instructorId);
        printGrid(instSlots, data, true);
    }

    private static void printGrid(List<ScheduleSlot> slots, TimetableData data, boolean showBatch) {
        // Map: Day -> (slotIndex -> ScheduleSlot)
        Map<DayOfWeek, Map<Integer, ScheduleSlot>> matrix = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            matrix.put(d, new HashMap<>());
        }
        for (ScheduleSlot s : slots) {
            matrix.get(s.getTimeSlot().getDay()).put(s.getTimeSlot().getSlotIndex(), s);
        }

        int slotCount = 6;
        String[] periodLabels = {
                "P1 (09-10)", "P2 (10-11)", "P3 (11-12)", "P4 (13-14)", "P5 (14-15)", "P6 (15-16)"
        };

        // Header
        System.out.printf("| %-9s |", "DAY");
        for (String pLabel : periodLabels) {
            System.out.printf(" %-22s |", pLabel);
        }
        System.out.println();
        System.out.println("+-----------+" + "------------------------+".repeat(slotCount));

        for (DayOfWeek day : DayOfWeek.values()) {
            System.out.printf("| %-9s |", day.getDisplayName());
            Map<Integer, ScheduleSlot> daySlots = matrix.get(day);

            for (int p = 0; p < slotCount; p++) {
                ScheduleSlot s = daySlots.get(p);
                if (s == null) {
                    System.out.printf(" %-22s |", "       ---");
                } else {
                    Course c = data.getCourse(s.getSession().getCourseId());
                    String cellText;
                    if (showBatch) {
                        StudentGroup g = data.getStudentGroup(s.getSession().getStudentGroupId());
                        cellText = String.format("%s (%s)", c.getCode(), g != null ? g.getId().replace("GRP_", "") : "");
                    } else {
                        cellText = String.format("%s [%s]", c.getCode(), s.getRoom().getName().replaceAll("\\(.*\\)", "").trim());
                    }
                    if (cellText.length() > 22) {
                        cellText = cellText.substring(0, 22);
                    }
                    System.out.printf(" %-22s |", cellText);
                }
            }
            System.out.println();
        }
        System.out.println("+-----------+" + "------------------------+".repeat(slotCount));
    }
}
