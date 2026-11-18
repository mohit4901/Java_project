package com.timetabler.constraint;

import com.timetabler.model.*;

import java.util.*;

/**
 * Evaluates timetable candidate solutions against hard and soft constraints.
 * Calculates conflict counts, penalties, and overall fitness score.
 */
public class FitnessEvaluator {

    // Hard constraint penalties (very high to enforce feasibility)
    public static final int HARD_PENALTY_WEIGHT = 1000;

    // Soft constraint weights
    public static final int SOFT_SAME_DAY_COURSE_WEIGHT = 15;
    public static final int SOFT_STUDENT_IDLE_GAP_WEIGHT = 10;
    public static final int SOFT_INSTRUCTOR_OVERLOAD_WEIGHT = 25;
    public static final int SOFT_CONSECUTIVE_OVERLOAD_WEIGHT = 20;

    private final TimetableData data;

    public FitnessEvaluator(TimetableData data) {
        this.data = data;
    }

    /**
     * Evaluation result holding fitness, hard conflict count, soft penalty, and violations list.
     */
    public static class EvaluationResult {
        public final double fitness;
        public final int hardConflicts;
        public final int softPenalty;
        public final List<String> diagnosticViolations;

        public EvaluationResult(double fitness, int hardConflicts, int softPenalty, List<String> diagnosticViolations) {
            this.fitness = fitness;
            this.hardConflicts = hardConflicts;
            this.softPenalty = softPenalty;
            this.diagnosticViolations = diagnosticViolations;
        }
    }

    /**
     * Fast evaluation used in the genetic algorithm inner loop.
     */
    public EvaluationResult evaluate(List<ScheduleSlot> slots, boolean collectDiagnostics) {
        int hardConflicts = 0;
        int softPenalty = 0;
        List<String> diagnostics = collectDiagnostics ? new ArrayList<>() : null;

        // Index structures for fast lookup:
        // Key: (TimeSlotId, RoomId) -> list of sessions
        Map<String, List<ScheduleSlot>> roomSlotMap = new HashMap<>();
        // Key: (TimeSlotId, InstructorId) -> list of sessions
        Map<String, List<ScheduleSlot>> instructorSlotMap = new HashMap<>();
        // Key: (TimeSlotId, GroupId) -> list of sessions
        Map<String, List<ScheduleSlot>> groupSlotMap = new HashMap<>();

        // Group sessions by studentGroup and day for soft constraint evaluation
        // Key: GroupId -> (Day -> List<ScheduleSlot>)
        Map<String, Map<DayOfWeek, List<ScheduleSlot>>> groupDayMap = new HashMap<>();
        // Key: InstructorId -> (Day -> List<ScheduleSlot>)
        Map<String, Map<DayOfWeek, List<ScheduleSlot>>> instructorDayMap = new HashMap<>();

        for (ScheduleSlot slot : slots) {
            TimeSlot ts = slot.getTimeSlot();
            Room room = slot.getRoom();
            Instructor inst = slot.getInstructor();
            ClassSession session = slot.getSession();
            String groupId = session.getStudentGroupId();
            Course course = data.getCourse(session.getCourseId());
            StudentGroup group = data.getStudentGroup(groupId);

            // 1. Room Double-booking
            String roomKey = ts.getId() + "@" + room.getId();
            roomSlotMap.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(slot);

            // 2. Instructor Double-booking
            String instKey = ts.getId() + "@" + inst.getId();
            instructorSlotMap.computeIfAbsent(instKey, k -> new ArrayList<>()).add(slot);

            // 3. Student Group Double-booking
            String grpKey = ts.getId() + "@" + groupId;
            groupSlotMap.computeIfAbsent(grpKey, k -> new ArrayList<>()).add(slot);

            // 4. Room Capacity Constraint
            if (group != null && room.getCapacity() < group.getNumberOfStudents()) {
                hardConflicts++;
                if (collectDiagnostics) {
                    diagnostics.add(String.format("Room Capacity: %s (cap %d) cannot hold %s (%d students) at %s",
                            room.getName(), room.getCapacity(), group.getName(), group.getNumberOfStudents(), ts));
                }
            }

            // 5. Room Type Constraint (Lab vs Lecture)
            if (course != null && course.requiresLab()) {
                if (room.getType() != course.getRequiredRoomType()) {
                    hardConflicts++;
                    if (collectDiagnostics) {
                        diagnostics.add(String.format("Room Type Mismatch: %s requires %s, but scheduled in %s (%s) at %s",
                                course.getName(), course.getRequiredRoomType().getLabel(), room.getName(), room.getType().getLabel(), ts));
                    }
                }
            }

            // 6. Instructor Qualification Constraint
            if (course != null && !inst.canTeach(course.getId())) {
                hardConflicts++;
                if (collectDiagnostics) {
                    diagnostics.add(String.format("Instructor Unqualified: %s is not certified to teach %s",
                            inst.getName(), course.getName()));
                }
            }

            // Index for soft constraints
            groupDayMap
                    .computeIfAbsent(groupId, k -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(ts.getDay(), k -> new ArrayList<>())
                    .add(slot);

            instructorDayMap
                    .computeIfAbsent(inst.getId(), k -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(ts.getDay(), k -> new ArrayList<>())
                    .add(slot);
        }

        // Evaluate double bookings
        for (Map.Entry<String, List<ScheduleSlot>> entry : roomSlotMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                int clashes = entry.getValue().size() - 1;
                hardConflicts += clashes;
                if (collectDiagnostics) {
                    diagnostics.add(String.format("Room Clash (%d extra): Room %s booked multiple times at %s",
                            clashes, entry.getValue().get(0).getRoom().getName(), entry.getValue().get(0).getTimeSlot()));
                }
            }
        }

        for (Map.Entry<String, List<ScheduleSlot>> entry : instructorSlotMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                int clashes = entry.getValue().size() - 1;
                hardConflicts += clashes;
                if (collectDiagnostics) {
                    diagnostics.add(String.format("Instructor Clash (%d extra): %s double-booked at %s",
                            clashes, entry.getValue().get(0).getInstructor().getName(), entry.getValue().get(0).getTimeSlot()));
                }
            }
        }

        for (Map.Entry<String, List<ScheduleSlot>> entry : groupSlotMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                int clashes = entry.getValue().size() - 1;
                hardConflicts += clashes;
                if (collectDiagnostics) {
                    StudentGroup g = data.getStudentGroup(entry.getValue().get(0).getSession().getStudentGroupId());
                    diagnostics.add(String.format("Group Clash (%d extra): Batch %s has multiple classes at %s",
                            clashes, (g != null ? g.getName() : "Unknown"), entry.getValue().get(0).getTimeSlot()));
                }
            }
        }

        // --- SOFT CONSTRAINTS ---

        // Soft 1: Subject distribution across days for student groups
        // If the same course is scheduled multiple times in the same day for a group, apply penalty
        for (Map.Entry<String, Map<DayOfWeek, List<ScheduleSlot>>> grpEntry : groupDayMap.entrySet()) {
            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : grpEntry.getValue().entrySet()) {
                Map<String, Integer> courseCounts = new HashMap<>();
                for (ScheduleSlot s : dayEntry.getValue()) {
                    String cid = s.getSession().getCourseId();
                    courseCounts.put(cid, courseCounts.getOrDefault(cid, 0) + 1);
                }
                for (Map.Entry<String, Integer> cCount : courseCounts.entrySet()) {
                    if (cCount.getValue() > 1) {
                        int excess = cCount.getValue() - 1;
                        softPenalty += excess * SOFT_SAME_DAY_COURSE_WEIGHT;
                    }
                }

                // Soft 2: Idle gaps for students
                List<ScheduleSlot> daySlots = new ArrayList<>(dayEntry.getValue());
                daySlots.sort(Comparator.comparingInt(s -> s.getTimeSlot().getSlotIndex()));
                for (int i = 0; i < daySlots.size() - 1; i++) {
                    int gap = daySlots.get(i + 1).getTimeSlot().getSlotIndex() - daySlots.get(i).getTimeSlot().getSlotIndex() - 1;
                    if (gap > 0) {
                        softPenalty += gap * SOFT_STUDENT_IDLE_GAP_WEIGHT;
                    }
                }

                // Soft 3: Excessive consecutive hours (e.g. >= 4 consecutive without gap)
                int currentStreak = 1;
                for (int i = 0; i < daySlots.size() - 1; i++) {
                    if (daySlots.get(i + 1).getTimeSlot().getSlotIndex() == daySlots.get(i).getTimeSlot().getSlotIndex() + 1) {
                        currentStreak++;
                        if (currentStreak >= 4) {
                            softPenalty += SOFT_CONSECUTIVE_OVERLOAD_WEIGHT;
                        }
                    } else {
                        currentStreak = 1;
                    }
                }
            }
        }

        // Soft 4: Instructor daily overload (beyond instructor maxDailyHours)
        for (Map.Entry<String, Map<DayOfWeek, List<ScheduleSlot>>> instEntry : instructorDayMap.entrySet()) {
            Instructor inst = data.getInstructor(instEntry.getKey());
            int maxDaily = (inst != null) ? inst.getMaxDailyHours() : 4;
            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : instEntry.getValue().entrySet()) {
                int dailyLectures = dayEntry.getValue().size();
                if (dailyLectures > maxDaily) {
                    softPenalty += (dailyLectures - maxDaily) * SOFT_INSTRUCTOR_OVERLOAD_WEIGHT;
                }
            }
        }

        // Fitness function: 1.0 / (1.0 + hardConflicts * 1000 + softPenalty)
        // Normalized between 0.0 and 1.0. A perfectly conflict-free timetable has hardConflicts == 0.
        double fitness = 1.0 / (1.0 + (hardConflicts * HARD_PENALTY_WEIGHT) + softPenalty);

        return new EvaluationResult(fitness, hardConflicts, softPenalty, diagnostics);
    }
}
