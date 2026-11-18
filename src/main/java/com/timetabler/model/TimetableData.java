package com.timetabler.model;

import java.util.*;

/**
 * Encapsulates the entire input dataset and lookup indices for the scheduling problem.
 */
public class TimetableData {
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<String, Instructor> instructors = new LinkedHashMap<>();
    private final Map<String, StudentGroup> studentGroups = new LinkedHashMap<>();
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    private final List<ClassSession> classSessions = new ArrayList<>();

    public void addCourse(Course course) {
        courses.put(course.getId(), course);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public void addInstructor(Instructor instructor) {
        instructors.put(instructor.getId(), instructor);
    }

    public void addStudentGroup(StudentGroup group) {
        studentGroups.put(group.getId(), group);
    }

    /**
     * Helper to add or update a course and enroll a student group with a qualified instructor.
     */
    public synchronized Course addOrUpdateLectureCourse(String code, String name, String groupId,
                                                        String instructorId, int weeklyLectures,
                                                        boolean requiresLab, RoomType roomType) {
        String courseId = code.toUpperCase().trim();
        Course course = new Course(courseId, courseId, name, weeklyLectures, requiresLab, roomType);
        courses.put(courseId, course);

        // Assign instructor qualification
        Instructor inst = instructors.get(instructorId);
        if (inst != null) {
            inst.addQualifiedCourse(courseId);
        }

        // Enroll student group
        StudentGroup group = studentGroups.get(groupId);
        if (group != null) {
            group.enrollCourse(courseId);
        }

        // Regenerate sessions
        generateClassSessions();
        return course;
    }

    public void addTimeSlot(TimeSlot slot) {
        timeSlots.add(slot);
    }

    /**
     * Automatically generates all ClassSession instances needed based on
     * student groups and enrolled courses' weeklyLectures count.
     */
    public void generateClassSessions() {
        classSessions.clear();
        for (StudentGroup group : studentGroups.values()) {
            for (String courseId : group.getEnrolledCourseIds()) {
                Course course = courses.get(courseId);
                if (course != null) {
                    for (int i = 1; i <= course.getWeeklyLectures(); i++) {
                        String sessionId = group.getId() + "_" + course.getCode() + "_S" + i;
                        classSessions.add(new ClassSession(sessionId, courseId, group.getId(), i));
                    }
                }
            }
        }
    }

    public Course getCourse(String id) {
        return courses.get(id);
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public Instructor getInstructor(String id) {
        return instructors.get(id);
    }

    public StudentGroup getStudentGroup(String id) {
        return studentGroups.get(id);
    }

    public Map<String, Course> getCourses() {
        return Collections.unmodifiableMap(courses);
    }

    public Map<String, Room> getRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public Map<String, Instructor> getInstructors() {
        return Collections.unmodifiableMap(instructors);
    }

    public Map<String, StudentGroup> getStudentGroups() {
        return Collections.unmodifiableMap(studentGroups);
    }

    public List<TimeSlot> getTimeSlots() {
        return Collections.unmodifiableList(timeSlots);
    }

    public TimeSlot getTimeSlot(String id) {
        for (TimeSlot ts : timeSlots) {
            if (ts.getId().equalsIgnoreCase(id)) return ts;
        }
        return null;
    }

    public TimeSlot getTimeSlot(DayOfWeek day, int slotIndex) {
        for (TimeSlot ts : timeSlots) {
            if (ts.getDay() == day && ts.getSlotIndex() == slotIndex) return ts;
        }
        return null;
    }

    public List<ClassSession> getClassSessions() {
        return Collections.unmodifiableList(classSessions);
    }

    /**
     * Find all instructors qualified to teach a given course.
     */
    public List<Instructor> getQualifiedInstructors(String courseId) {
        List<Instructor> result = new ArrayList<>();
        for (Instructor inst : instructors.values()) {
            if (inst.canTeach(courseId)) {
                result.add(inst);
            }
        }
        return result;
    }

    /**
     * Find all compatible rooms for a given course and student group.
     */
    public List<Room> getCompatibleRooms(Course course, StudentGroup group) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (course.requiresLab()) {
                if (r.getType() == course.getRequiredRoomType() && r.getCapacity() >= group.getNumberOfStudents()) {
                    result.add(r);
                }
            } else {
                if (!r.getType().isLab() && r.getCapacity() >= group.getNumberOfStudents()) {
                    result.add(r);
                }
            }
        }
        if (result.isEmpty()) {
            // fallback if strictly filtered yields empty
            for (Room r : rooms.values()) {
                if (r.getCapacity() >= group.getNumberOfStudents()) {
                    result.add(r);
                }
            }
        }
        return result;
    }
}
