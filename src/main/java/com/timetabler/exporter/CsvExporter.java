package com.timetabler.exporter;

import com.timetabler.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exports timetables to standard Comma-Separated Values (CSV) format.
 */
public class CsvExporter {

    public static void exportToFile(Timetable timetable, TimetableData data, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Day,PeriodIndex,TimeRange,BatchID,BatchName,CourseCode,CourseName,IsLab,RoomID,RoomName,RoomType,FacultyID,FacultyName\n");

        List<ScheduleSlot> slots = timetable.getSlots();
        for (ScheduleSlot slot : slots) {
            TimeSlot ts = slot.getTimeSlot();
            ClassSession session = slot.getSession();
            StudentGroup group = data.getStudentGroup(session.getStudentGroupId());
            Course course = data.getCourse(session.getCourseId());
            Room room = slot.getRoom();
            Instructor inst = slot.getInstructor();

            sb.append(escape(ts.getDay().getDisplayName())).append(",")
              .append(ts.getSlotIndex() + 1).append(",")
              .append(escape(ts.getTimeRange())).append(",")
              .append(escape(group != null ? group.getId() : session.getStudentGroupId())).append(",")
              .append(escape(group != null ? group.getName() : "")).append(",")
              .append(escape(course != null ? course.getCode() : "")).append(",")
              .append(escape(course != null ? course.getName() : "")).append(",")
              .append(course != null && course.requiresLab() ? "true" : "false").append(",")
              .append(escape(room.getId())).append(",")
              .append(escape(room.getName())).append(",")
              .append(escape(room.getType().getLabel())).append(",")
              .append(escape(inst.getId())).append(",")
              .append(escape(inst.getName())).append("\n");
        }

        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, sb.toString());
    }

    private static String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
