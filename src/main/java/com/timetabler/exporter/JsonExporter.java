package com.timetabler.exporter;

import com.timetabler.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Serializes timetables into JSON for programmatic integration and APIs.
 */
public class JsonExporter {

    public static void exportToFile(Timetable timetable, TimetableData data, String filePath) throws IOException {
        String json = toJson(timetable, data);
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, json);
    }

    public static String toJson(Timetable timetable, TimetableData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"").append(timetable.isValid() ? "FEASIBLE" : "INFEASIBLE").append("\",\n");
        sb.append("  \"fitness\": ").append(timetable.getFitness()).append(",\n");
        sb.append("  \"hardConflicts\": ").append(timetable.getHardConflicts()).append(",\n");
        sb.append("  \"softPenalty\": ").append(timetable.getSoftConflicts()).append(",\n");
        sb.append("  \"totalSessions\": ").append(timetable.getSlots().size()).append(",\n");

        sb.append("  \"schedule\": [\n");
        List<ScheduleSlot> slots = timetable.getSlots();
        for (int i = 0; i < slots.size(); i++) {
            ScheduleSlot slot = slots.get(i);
            TimeSlot ts = slot.getTimeSlot();
            ClassSession session = slot.getSession();
            StudentGroup group = data.getStudentGroup(session.getStudentGroupId());
            Course course = data.getCourse(session.getCourseId());
            Room room = slot.getRoom();
            Instructor inst = slot.getInstructor();

            sb.append("    {\n");
            sb.append("      \"sessionId\": \"").append(session.getId()).append("\",\n");
            sb.append("      \"day\": \"").append(ts.getDay().name()).append("\",\n");
            sb.append("      \"slotIndex\": ").append(ts.getSlotIndex()).append(",\n");
            sb.append("      \"timeRange\": \"").append(ts.getTimeRange()).append("\",\n");
            sb.append("      \"batch\": {\"id\": \"").append(group != null ? group.getId() : "").append("\", \"name\": \"").append(group != null ? group.getName() : "").append("\"},\n");
            sb.append("      \"course\": {\"id\": \"").append(course != null ? course.getId() : "").append("\", \"code\": \"").append(course != null ? course.getCode() : "").append("\", \"name\": \"").append(course != null ? course.getName() : "").append("\", \"isLab\": ").append(course != null && course.requiresLab()).append("},\n");
            sb.append("      \"room\": {\"id\": \"").append(room.getId()).append("\", \"name\": \"").append(room.getName()).append("\", \"type\": \"").append(room.getType().name()).append("\", \"capacity\": ").append(room.getCapacity()).append("},\n");
            sb.append("      \"instructor\": {\"id\": \"").append(inst.getId()).append("\", \"name\": \"").append(inst.getName()).append("\", \"email\": \"").append(inst.getEmail()).append("\"}\n");
            sb.append("    }").append(i < slots.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }
}
