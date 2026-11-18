package com.timetabler.exporter;

import com.timetabler.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generates an interactive, modern HTML5 + CSS3 web application for viewing,
 * filtering, editing slots directly, adding lectures dynamically, and printing timetables.
 */
public class HtmlExporter {

    private static final String[] COLOR_PALETTE = {
            "#4f46e5", "#0891b2", "#059669", "#d97706",
            "#dc2626", "#7c3aed", "#2563eb", "#db2777",
            "#0d9488", "#ea580c", "#65a30d", "#475569"
    };

    public static void exportToFile(Timetable timetable, TimetableData data, String filePath) throws IOException {
        String html = generateHtml(timetable, data);
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, html);
    }

    public static String generateHtml(Timetable timetable, TimetableData data) {
        // Assign deterministic color to each course
        Map<String, String> courseColors = new HashMap<>();
        int colorIdx = 0;
        for (Course c : data.getCourses().values()) {
            courseColors.put(c.getId(), COLOR_PALETTE[colorIdx % COLOR_PALETTE.length]);
            colorIdx++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Automated Academic Timetable Dashboard</title>\n");
        sb.append("<style>\n");
        sb.append(getCssStyles());
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<div class=\"app-container\">\n");

        // Header
        sb.append("<header class=\"header\">\n");
        sb.append("  <div class=\"brand\">\n");
        sb.append("    <div class=\"brand-icon\">📅</div>\n");
        sb.append("    <div>\n");
        sb.append("      <h1>Automated Timetable Generator</h1>\n");
        sb.append("      <p class=\"subtitle\">Interactive Scheduling Engine • Click any lecture card below to Edit / Move</p>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"actions no-print\">\n");
        sb.append("    <button class=\"btn btn-success\" onclick=\"openLectureModal()\">➕ Add Lecture / Subject</button>\n");
        sb.append("    <button class=\"btn btn-secondary\" onclick=\"regenerateTimetable()\">🔄 Re-generate</button>\n");
        sb.append("    <button class=\"btn btn-primary\" onclick=\"window.print()\">🖨️ Print / Save PDF</button>\n");
        sb.append("  </div>\n");
        sb.append("</header>\n");

        // Live notification banner
        sb.append("<div id=\"liveNotification\" class=\"notification-banner\" style=\"display:none;\"></div>\n");

        // KPI Metric Cards
        sb.append("<div class=\"stats-grid no-print\">\n");
        sb.append("  <div class=\"card stat-card\">\n");
        sb.append("    <div class=\"stat-label\">Status</div>\n");
        if (timetable.isValid()) {
            sb.append("    <div class=\"stat-val text-success\">OPTIMAL</div>\n");
            sb.append("    <div class=\"stat-desc\">0 Hard Conflicts (Feasible)</div>\n");
        } else {
            sb.append("    <div class=\"stat-val text-danger\">").append(timetable.getHardConflicts()).append(" CLASHES</div>\n");
            sb.append("    <div class=\"stat-desc\">Conflict Detected</div>\n");
        }
        sb.append("  </div>\n");

        sb.append("  <div class=\"card stat-card\">\n");
        sb.append("    <div class=\"stat-label\">Fitness Score</div>\n");
        sb.append(String.format("    <div class=\"stat-val\">%.4f</div>\n", timetable.getFitness()));
        sb.append("    <div class=\"stat-desc\">Soft penalty: ").append(timetable.getSoftConflicts()).append("</div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"card stat-card\">\n");
        sb.append("    <div class=\"stat-label\">Scheduled Sessions</div>\n");
        sb.append("    <div class=\"stat-val\">").append(timetable.getSlots().size()).append("</div>\n");
        sb.append("    <div class=\"stat-desc\">Across ").append(data.getTimeSlots().size()).append(" time slots</div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"card stat-card\">\n");
        sb.append("    <div class=\"stat-label\">Entities</div>\n");
        sb.append("    <div class=\"stat-val\">")
                .append(data.getStudentGroups().size()).append(" / ")
                .append(data.getInstructors().size()).append(" / ")
                .append(data.getRooms().size())
                .append("</div>\n");
        sb.append("    <div class=\"stat-desc\">Batches / Faculty / Rooms</div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        // Navigation Tabs
        sb.append("<div class=\"nav-tabs no-print\">\n");
        sb.append("  <button class=\"tab-btn active\" onclick=\"showTab('batches')\">👨‍🎓 Student Batches</button>\n");
        sb.append("  <button class=\"tab-btn\" onclick=\"showTab('faculty')\">👩‍🏫 Faculty Schedules</button>\n");
        sb.append("  <button class=\"tab-btn\" onclick=\"showTab('rooms')\">🏫 Rooms & Labs</button>\n");
        sb.append("  <button class=\"tab-btn\" onclick=\"showTab('diagnostics')\">🔍 Diagnostics & Rules</button>\n");
        sb.append("</div>\n");

        // --- TAB 1: BATCHES ---
        sb.append("<div id=\"tab-batches\" class=\"tab-content active\">\n");
        sb.append("  <div class=\"toolbar no-print\">\n");
        sb.append("    <label for=\"batchSelect\"><b>Select Batch / Section:</b></label>\n");
        sb.append("    <select id=\"batchSelect\" onchange=\"filterBatch()\" class=\"select-input\">\n");
        for (StudentGroup g : data.getStudentGroups().values()) {
            sb.append("      <option value=\"").append(g.getId()).append("\">").append(g.getName()).append(" (").append(g.getNumberOfStudents()).append(" students)</option>\n");
        }
        sb.append("    </select>\n");
        sb.append("    <span class=\"hint-text\">💡 Tip: Click on any class card to edit, move to another day/time, or change room/teacher.</span>\n");
        sb.append("  </div>\n");

        for (StudentGroup g : data.getStudentGroups().values()) {
            sb.append("  <div class=\"timetable-view batch-view\" id=\"view_batch_").append(g.getId()).append("\">\n");
            sb.append("    <h2 class=\"section-title\">Weekly Schedule: ").append(g.getName()).append("</h2>\n");
            sb.append(renderBatchGrid(timetable, data, g.getId(), courseColors));
            sb.append("  </div>\n");
        }
        sb.append("</div>\n");

        // --- TAB 2: FACULTY ---
        sb.append("<div id=\"tab-faculty\" class=\"tab-content\">\n");
        sb.append("  <div class=\"toolbar no-print\">\n");
        sb.append("    <label for=\"facultySelect\"><b>Select Instructor:</b></label>\n");
        sb.append("    <select id=\"facultySelect\" onchange=\"filterFaculty()\" class=\"select-input\">\n");
        for (Instructor inst : data.getInstructors().values()) {
            sb.append("      <option value=\"").append(inst.getId()).append("\">").append(inst.getName()).append(" [").append(inst.getDepartment()).append("]</option>\n");
        }
        sb.append("    </select>\n");
        sb.append("  </div>\n");

        for (Instructor inst : data.getInstructors().values()) {
            sb.append("  <div class=\"timetable-view faculty-view\" id=\"view_faculty_").append(inst.getId()).append("\">\n");
            sb.append("    <h2 class=\"section-title\">Faculty Schedule: ").append(inst.getName()).append(" (").append(inst.getEmail()).append(")</h2>\n");
            sb.append(renderFacultyGrid(timetable, data, inst.getId(), courseColors));
            sb.append("  </div>\n");
        }
        sb.append("</div>\n");

        // --- TAB 3: ROOMS ---
        sb.append("<div id=\"tab-rooms\" class=\"tab-content\">\n");
        sb.append("  <div class=\"toolbar no-print\">\n");
        sb.append("    <label for=\"roomSelect\"><b>Select Room:</b></label>\n");
        sb.append("    <select id=\"roomSelect\" onchange=\"filterRoom()\" class=\"select-input\">\n");
        for (Room r : data.getRooms().values()) {
            sb.append("      <option value=\"").append(r.getId()).append("\">").append(r.getName()).append(" (Cap: ").append(r.getCapacity()).append(")</option>\n");
        }
        sb.append("    </select>\n");
        sb.append("  </div>\n");

        for (Room r : data.getRooms().values()) {
            sb.append("  <div class=\"timetable-view room-view\" id=\"view_room_").append(r.getId()).append("\">\n");
            sb.append("    <h2 class=\"section-title\">Room Occupancy: ").append(r.getName()).append(" [").append(r.getType().getLabel()).append("]</h2>\n");
            sb.append(renderRoomGrid(timetable, data, r.getId(), courseColors));
            sb.append("  </div>\n");
        }
        sb.append("</div>\n");

        // --- TAB 4: DIAGNOSTICS ---
        sb.append("<div id=\"tab-diagnostics\" class=\"tab-content\">\n");
        sb.append("  <div class=\"card\">\n");
        sb.append("    <h2 class=\"section-title\">Engine Validation & Diagnostics</h2>\n");
        if (timetable.getDiagnosticViolations().isEmpty()) {
            sb.append("    <div class=\"alert-box success\">\n");
            sb.append("      <b>✨ Feasible & Valid Solution:</b> All hard constraints (no instructor clashes, no student group clashes, no room clashes, capacity limits, and lab type prerequisites) are fully satisfied (0 violations).\n");
            sb.append("    </div>\n");
        } else {
            sb.append("    <div class=\"alert-box warning\">\n");
            sb.append("      <b>⚠️ Detected Violations (").append(timetable.getDiagnosticViolations().size()).append("):</b>\n");
            sb.append("      <ul>\n");
            for (String v : timetable.getDiagnosticViolations()) {
                sb.append("        <li>").append(v).append("</li>\n");
            }
            sb.append("      </ul>\n");
            sb.append("    </div>\n");
        }

        sb.append("    <h3 style=\"margin-top: 24px;\">Hard & Soft Constraint Architecture</h3>\n");
        sb.append("    <table class=\"simple-table\">\n");
        sb.append("      <thead><tr><th>Constraint</th><th>Type</th><th>Penalty / Rule</th><th>Status</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        sb.append("        <tr><td>No Room Clashes</td><td>Hard</td><td>1000 pts per overlap</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>No Faculty Double-Booking</td><td>Hard</td><td>1000 pts per overlap</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>No Student Batch Double-Booking</td><td>Hard</td><td>1000 pts per overlap</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>Room Capacity ≥ Batch Size</td><td>Hard</td><td>1000 pts per overflow</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>Lab Course in Dedicated Lab Room</td><td>Hard</td><td>1000 pts per mismatch</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>Faculty Subject Qualification</td><td>Hard</td><td>1000 pts per uncertified class</td><td class=\"text-success\">Enforced</td></tr>\n");
        sb.append("        <tr><td>Even Course Distribution Across Days</td><td>Soft</td><td>15 pts per duplicate course/day</td><td class=\"text-success\">Minimized</td></tr>\n");
        sb.append("        <tr><td>Avoid Single-Hour Idle Gaps</td><td>Soft</td><td>10 pts per empty hour hole</td><td class=\"text-success\">Minimized</td></tr>\n");
        sb.append("        <tr><td>Faculty Daily Workload Balance</td><td>Soft</td><td>25 pts per hour over limit</td><td class=\"text-success\">Minimized</td></tr>\n");
        sb.append("      </tbody>\n");
        sb.append("    </table>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        sb.append("</div>\n"); // app-container

        // --- MODAL 1: ADD LECTURE FORM ---
        sb.append("<div id=\"lectureModal\" class=\"modal-backdrop\" style=\"display:none;\">\n");
        sb.append("  <div class=\"modal-content\">\n");
        sb.append("    <div class=\"modal-header\">\n");
        sb.append("      <h3>➕ Add Lecture / Course & Update Timetable</h3>\n");
        sb.append("      <button class=\"close-btn\" onclick=\"closeLectureModal()\">&times;</button>\n");
        sb.append("    </div>\n");
        sb.append("    <form id=\"addLectureForm\" onsubmit=\"submitNewLecture(event)\">\n");
        sb.append("      <div class=\"form-group\">\n");
        sb.append("        <label>Course Code *</label>\n");
        sb.append("        <input type=\"text\" id=\"mCourseCode\" required placeholder=\"e.g. CS401 or MATH202\" class=\"form-input\" />\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-group\">\n");
        sb.append("        <label>Course Title / Subject Name *</label>\n");
        sb.append("        <input type=\"text\" id=\"mCourseName\" required placeholder=\"e.g. Cloud Computing & DevOps\" class=\"form-input\" />\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-row\">\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Target Student Batch *</label>\n");
        sb.append("          <select id=\"mBatch\" class=\"form-input\" onchange=\"toggleNewBatch(this.value)\">\n");
        for (StudentGroup g : data.getStudentGroups().values()) {
            sb.append("            <option value=\"").append(g.getId()).append("\">").append(g.getName()).append("</option>\n");
        }
        sb.append("            <option value=\"__NEW__\">+ Create New Batch...</option>\n");
        sb.append("          </select>\n");
        sb.append("          <input type=\"text\" id=\"mNewBatchName\" placeholder=\"New Batch Name (e.g. CSE-Year-4)\" class=\"form-input\" style=\"display:none; margin-top:8px;\" />\n");
        sb.append("        </div>\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Assigned Faculty / Instructor *</label>\n");
        sb.append("          <select id=\"mInstructor\" class=\"form-input\" onchange=\"toggleNewInstructor(this.value)\">\n");
        for (Instructor inst : data.getInstructors().values()) {
            sb.append("            <option value=\"").append(inst.getId()).append("\">").append(inst.getName()).append("</option>\n");
        }
        sb.append("            <option value=\"__NEW__\">+ Create New Faculty...</option>\n");
        sb.append("          </select>\n");
        sb.append("          <input type=\"text\" id=\"mNewInstructorName\" placeholder=\"New Faculty Name (e.g. Dr. John)\" class=\"form-input\" style=\"display:none; margin-top:8px;\" />\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-row\">\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Weekly Lectures (Hours) *</label>\n");
        sb.append("          <input type=\"number\" id=\"mWeeklyLectures\" min=\"1\" max=\"6\" value=\"3\" class=\"form-input\" required />\n");
        sb.append("        </div>\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Room Requirement</label>\n");
        sb.append("          <select id=\"mRoomType\" class=\"form-input\">\n");
        sb.append("            <option value=\"CLASSROOM\">Standard Classroom</option>\n");
        sb.append("            <option value=\"LECTURE_HALL\">Lecture Hall / Auditorium</option>\n");
        sb.append("            <option value=\"COMPUTER_LAB\">Computer Lab</option>\n");
        sb.append("            <option value=\"SCIENCE_LAB\">Science / Electronics Lab</option>\n");
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-group checkbox-group\">\n");
        sb.append("        <input type=\"checkbox\" id=\"mRequiresLab\" onchange=\"toggleLabRequirement(this.checked)\" />\n");
        sb.append("        <label for=\"mRequiresLab\">This is a practical / laboratory session (requires Lab room)</label>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"modal-footer\">\n");
        sb.append("        <button type=\"button\" class=\"btn btn-secondary\" onclick=\"closeLectureModal()\">Cancel</button>\n");
        sb.append("        <button type=\"submit\" id=\"submitBtn\" class=\"btn btn-primary\">🚀 Add & Update Timetable</button>\n");
        sb.append("      </div>\n");
        sb.append("    </form>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        // --- MODAL 2: EDIT SCHEDULED SLOT MODAL ---
        sb.append("<div id=\"editSlotModal\" class=\"modal-backdrop\" style=\"display:none;\">\n");
        sb.append("  <div class=\"modal-content\">\n");
        sb.append("    <div class=\"modal-header\">\n");
        sb.append("      <h3 id=\"editModalTitle\">✏️ Edit Scheduled Class</h3>\n");
        sb.append("      <button class=\"close-btn\" onclick=\"closeEditSlotModal()\">&times;</button>\n");
        sb.append("    </div>\n");
        sb.append("    <form id=\"editSlotForm\" onsubmit=\"submitSlotEdit(event)\">\n");
        sb.append("      <input type=\"hidden\" id=\"eSessionId\" />\n");
        sb.append("      <div class=\"form-group\">\n");
        sb.append("        <label>Selected Course / Subject</label>\n");
        sb.append("        <div id=\"eCourseDisplay\" class=\"highlight-badge\"></div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-row\">\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Day of Week *</label>\n");
        sb.append("          <select id=\"eDay\" class=\"form-input\" required>\n");
        for (DayOfWeek d : DayOfWeek.values()) {
            sb.append("            <option value=\"").append(d.name()).append("\">").append(d.getDisplayName()).append("</option>\n");
        }
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Time Period *</label>\n");
        sb.append("          <select id=\"ePeriod\" class=\"form-input\" required>\n");
        sb.append("            <option value=\"0\">Period 1 (09:00 - 10:00)</option>\n");
        sb.append("            <option value=\"1\">Period 2 (10:00 - 11:00)</option>\n");
        sb.append("            <option value=\"2\">Period 3 (11:00 - 12:00)</option>\n");
        sb.append("            <option value=\"3\">Period 4 (13:00 - 14:00)</option>\n");
        sb.append("            <option value=\"4\">Period 5 (14:00 - 15:00)</option>\n");
        sb.append("            <option value=\"5\">Period 6 (15:00 - 16:00)</option>\n");
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"form-row\">\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Classroom / Lab *</label>\n");
        sb.append("          <select id=\"eRoom\" class=\"form-input\" required>\n");
        for (Room r : data.getRooms().values()) {
            sb.append("            <option value=\"").append(r.getId()).append("\">").append(r.getName()).append(" (Cap: ").append(r.getCapacity()).append(")</option>\n");
        }
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("        <div class=\"form-group flex-1\">\n");
        sb.append("          <label>Assigned Faculty / Instructor *</label>\n");
        sb.append("          <select id=\"eInstructor\" class=\"form-input\" required>\n");
        for (Instructor inst : data.getInstructors().values()) {
            sb.append("            <option value=\"").append(inst.getId()).append("\">").append(inst.getName()).append(" [").append(inst.getDepartment()).append("]</option>\n");
        }
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"modal-footer\" style=\"justify-content: space-between;\">\n");
        sb.append("        <button type=\"button\" class=\"btn btn-danger\" onclick=\"deleteCurrentSlot()\">🗑️ Remove Class</button>\n");
        sb.append("        <div style=\"display:flex; gap:10px;\">\n");
        sb.append("          <button type=\"button\" class=\"btn btn-secondary\" onclick=\"closeEditSlotModal()\">Cancel</button>\n");
        sb.append("          <button type=\"submit\" id=\"saveEditBtn\" class=\"btn btn-primary\">💾 Save Changes</button>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("    </form>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        // Interactive JavaScript
        sb.append("<script>\n");
        sb.append(getJavaScript());
        sb.append("</script>\n");

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private static String renderBatchGrid(Timetable tt, TimetableData data, String groupId, Map<String, String> colors) {
        List<ScheduleSlot> slots = tt.getSlotsForGroup(groupId);
        Map<DayOfWeek, Map<Integer, ScheduleSlot>> matrix = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            matrix.put(d, new HashMap<>());
        }
        for (ScheduleSlot s : slots) {
            matrix.get(s.getTimeSlot().getDay()).put(s.getTimeSlot().getSlotIndex(), s);
        }

        return renderHtmlTable(matrix, data, colors, "batch", groupId);
    }

    private static String renderFacultyGrid(Timetable tt, TimetableData data, String instId, Map<String, String> colors) {
        List<ScheduleSlot> slots = tt.getSlotsForInstructor(instId);
        Map<DayOfWeek, Map<Integer, ScheduleSlot>> matrix = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            matrix.put(d, new HashMap<>());
        }
        for (ScheduleSlot s : slots) {
            matrix.get(s.getTimeSlot().getDay()).put(s.getTimeSlot().getSlotIndex(), s);
        }

        return renderHtmlTable(matrix, data, colors, "faculty", instId);
    }

    private static String renderRoomGrid(Timetable tt, TimetableData data, String roomId, Map<String, String> colors) {
        List<ScheduleSlot> slots = tt.getSlotsForRoom(roomId);
        Map<DayOfWeek, Map<Integer, ScheduleSlot>> matrix = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            matrix.put(d, new HashMap<>());
        }
        for (ScheduleSlot s : slots) {
            matrix.get(s.getTimeSlot().getDay()).put(s.getTimeSlot().getSlotIndex(), s);
        }

        return renderHtmlTable(matrix, data, colors, "room", roomId);
    }

    private static String renderHtmlTable(Map<DayOfWeek, Map<Integer, ScheduleSlot>> matrix,
                                          TimetableData data, Map<String, String> colors,
                                          String mode, String contextId) {
        StringBuilder sb = new StringBuilder();
        String[] periodHeaders = {
                "Period 1<br><small>09:00 - 10:00</small>",
                "Period 2<br><small>10:00 - 11:00</small>",
                "Period 3<br><small>11:00 - 12:00</small>",
                "Lunch Break<br><small>12:00 - 13:00</small>",
                "Period 4<br><small>13:00 - 14:00</small>",
                "Period 5<br><small>14:00 - 15:00</small>",
                "Period 6<br><small>15:00 - 16:00</small>"
        };

        sb.append("<div class=\"table-responsive\"><table class=\"timetable-grid\">\n");
        sb.append("<thead><tr><th class=\"day-col\">Day</th>");
        for (String h : periodHeaders) {
            sb.append("<th>").append(h).append("</th>");
        }
        sb.append("</tr></thead>\n<tbody>\n");

        for (DayOfWeek day : DayOfWeek.values()) {
            sb.append("<tr>");
            sb.append("<td class=\"day-cell\"><b>").append(day.getDisplayName()).append("</b></td>");
            Map<Integer, ScheduleSlot> daySlots = matrix.get(day);

            // Periods 0, 1, 2
            for (int p = 0; p < 3; p++) {
                sb.append(renderCell(daySlots.get(p), day, p, data, colors, mode, contextId));
            }

            // Lunch break column
            sb.append("<td class=\"lunch-cell\">☕ Lunch</td>");

            // Periods 3, 4, 5
            for (int p = 3; p < 6; p++) {
                sb.append(renderCell(daySlots.get(p), day, p, data, colors, mode, contextId));
            }

            sb.append("</tr>\n");
        }

        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private static String renderCell(ScheduleSlot slot, DayOfWeek day, int slotIndex,
                                     TimetableData data, Map<String, String> colors,
                                     String mode, String contextId) {
        if (slot == null) {
            return "<td class=\"empty-cell\" onclick=\"handleEmptySlotClick('" + day.name() + "', " + slotIndex + ", '" + mode + "', '" + escapeJs(contextId) + "')\" title=\"Click to schedule a lecture here\">\n" +
                   "  <span class=\"no-class\">—</span>\n" +
                   "  <span class=\"add-hint no-print\">+ Add</span>\n" +
                   "</td>";
        }

        Course course = data.getCourse(slot.getSession().getCourseId());
        String color = colors.getOrDefault(course.getId(), "#3b82f6");

        String jsCall = String.format("openEditSlotModal('%s', '%s', '%s', '%s', %d, '%s', '%s')",
                escapeJs(slot.getSession().getId()),
                escapeJs(course.getCode()),
                escapeJs(course.getName()),
                day.name(),
                slotIndex,
                escapeJs(slot.getRoom().getId()),
                escapeJs(slot.getInstructor().getId())
        );

        StringBuilder sb = new StringBuilder();
        sb.append("<td class=\"cell-filled\">\n");
        sb.append("  <div class=\"course-card\" style=\"border-left-color: ").append(color).append(";\" onclick=\"").append(jsCall).append("\" title=\"Click to Edit or Move this class\">\n");
        sb.append("    <div class=\"course-header\">\n");
        sb.append("      <span class=\"course-badge\" style=\"background: ").append(color).append(";\">").append(course.getCode()).append("</span>\n");
        if (course.requiresLab()) {
            sb.append("      <span class=\"badge-lab\">LAB</span>\n");
        }
        sb.append("      <span class=\"edit-badge no-print\" title=\"Click to edit\">✏️</span>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"course-title\" title=\"").append(course.getName()).append("\">").append(course.getName()).append("</div>\n");

        if ("batch".equals(mode)) {
            sb.append("    <div class=\"cell-meta\">📍 ").append(slot.getRoom().getName()).append("</div>\n");
            sb.append("    <div class=\"cell-meta\">👤 ").append(slot.getInstructor().getName()).append("</div>\n");
        } else if ("faculty".equals(mode)) {
            StudentGroup g = data.getStudentGroup(slot.getSession().getStudentGroupId());
            sb.append("    <div class=\"cell-meta\">👥 ").append(g != null ? g.getName() : "Batch").append("</div>\n");
            sb.append("    <div class=\"cell-meta\">📍 ").append(slot.getRoom().getName()).append("</div>\n");
        } else { // room mode
            StudentGroup g = data.getStudentGroup(slot.getSession().getStudentGroupId());
            sb.append("    <div class=\"cell-meta\">👥 ").append(g != null ? g.getName() : "Batch").append("</div>\n");
            sb.append("    <div class=\"cell-meta\">👤 ").append(slot.getInstructor().getName()).append("</div>\n");
        }

        sb.append("    <div class=\"edit-hint no-print\">Click to edit ✏️</div>\n");
        sb.append("  </div>\n</td>");
        return sb.toString();
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("'", "\\'").replace("\"", "\\\"");
    }

    private static String getCssStyles() {
        return """
            :root {
                --primary: #4f46e5;
                --primary-hover: #4338ca;
                --bg: #f8fafc;
                --surface: #ffffff;
                --border: #e2e8f0;
                --text-main: #0f172a;
                --text-muted: #64748b;
                --success: #10b981;
                --success-hover: #059669;
                --danger: #ef4444;
                --danger-hover: #dc2626;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif; }
            body { background: var(--bg); color: var(--text-main); line-height: 1.5; padding: 24px; }
            .app-container { max-width: 1400px; margin: 0 auto; }
            .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); flex-wrap: wrap; gap: 16px; }
            .brand { display: flex; align-items: center; gap: 16px; }
            .brand-icon { font-size: 38px; }
            .header h1 { font-size: 24px; font-weight: 700; color: #1e293b; }
            .subtitle { font-size: 14px; color: var(--text-muted); }
            .actions { display: flex; gap: 10px; align-items: center; }
            .btn { padding: 9px 18px; font-size: 14px; font-weight: 600; border-radius: 8px; border: none; cursor: pointer; transition: 0.2s; display: inline-flex; align-items: center; gap: 6px; }
            .btn-primary { background: var(--primary); color: white; }
            .btn-primary:hover { background: var(--primary-hover); }
            .btn-success { background: var(--success); color: white; }
            .btn-success:hover { background: var(--success-hover); }
            .btn-danger { background: var(--danger); color: white; }
            .btn-danger:hover { background: var(--danger-hover); }
            .btn-secondary { background: #e2e8f0; color: #334155; }
            .btn-secondary:hover { background: #cbd5e1; }
            
            .notification-banner { padding: 12px 18px; border-radius: 8px; margin-bottom: 20px; font-weight: 600; font-size: 14px; animation: fadeIn 0.3s; }
            .notification-banner.success { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
            .notification-banner.info { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
            .notification-banner.warning { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
            .notification-banner.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }

            .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
            .card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            .stat-card { text-align: left; }
            .stat-label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); font-weight: 600; margin-bottom: 4px; }
            .stat-val { font-size: 26px; font-weight: 700; color: var(--text-main); }
            .stat-desc { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
            .text-success { color: var(--success) !important; }
            .text-danger { color: var(--danger) !important; }
            .nav-tabs { display: flex; gap: 8px; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 8px; }
            .tab-btn { background: none; border: none; font-size: 15px; font-weight: 600; padding: 10px 18px; border-radius: 8px; cursor: pointer; color: var(--text-muted); transition: all 0.2s; }
            .tab-btn:hover { background: #f1f5f9; color: var(--text-main); }
            .tab-btn.active { background: #ede9fe; color: var(--primary); }
            .tab-content { display: none; }
            .tab-content.active { display: block; }
            .toolbar { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; background: white; padding: 12px 18px; border-radius: 8px; border: 1px solid var(--border); flex-wrap: wrap; }
            .select-input { padding: 8px 12px; font-size: 14px; border: 1px solid var(--border); border-radius: 6px; outline: none; background: #f8fafc; font-weight: 500; min-width: 280px; }
            .hint-text { font-size: 13px; color: #6366f1; font-weight: 500; margin-left: auto; }
            .section-title { font-size: 18px; font-weight: 700; margin-bottom: 16px; color: #1e293b; }
            .table-responsive { overflow-x: auto; background: white; border-radius: 12px; border: 1px solid var(--border); box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
            .timetable-grid { width: 100%; border-collapse: collapse; min-width: 1000px; text-align: left; }
            .timetable-grid th { background: #f1f5f9; padding: 12px; font-size: 13px; font-weight: 600; color: #475569; border-bottom: 2px solid var(--border); text-align: center; }
            .day-col { width: 110px; }
            .day-cell { background: #f8fafc; text-align: center; font-size: 14px; border-right: 1px solid var(--border); border-bottom: 1px solid var(--border); }
            .timetable-grid td { padding: 8px; border-bottom: 1px solid var(--border); vertical-align: top; width: 14%; }
            
            .empty-cell { background: #fafafa; text-align: center; vertical-align: middle !important; color: #cbd5e1; cursor: pointer; transition: all 0.15s; }
            .empty-cell:hover { background: #ede9fe; color: var(--primary); }
            .add-hint { display: none; font-size: 11px; font-weight: 700; color: var(--primary); }
            .empty-cell:hover .no-class { display: none; }
            .empty-cell:hover .add-hint { display: inline-block; }

            .lunch-cell { background: #fef3c7; text-align: center; vertical-align: middle !important; font-weight: 600; color: #92400e; font-size: 13px; border-left: 1px dashed #fcd34d; border-right: 1px dashed #fcd34d; }
            
            /* Card styling with edit indicator */
            .course-card { background: #ffffff; border: 1px solid #e2e8f0; border-left-width: 4px; border-radius: 6px; padding: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease; cursor: pointer; position: relative; }
            .course-card:hover { transform: translateY(-3px); box-shadow: 0 6px 12px -2px rgba(0,0,0,0.1); border-color: #a5b4fc; }
            .course-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
            .course-badge { color: white; font-weight: 700; font-size: 11px; padding: 2px 6px; border-radius: 4px; }
            .badge-lab { background: #ec4899; color: white; font-size: 9px; font-weight: 800; padding: 2px 4px; border-radius: 3px; }
            .edit-badge { font-size: 11px; opacity: 0; transition: opacity 0.15s; }
            .course-card:hover .edit-badge { opacity: 1; }
            .course-title { font-size: 12px; font-weight: 600; color: #1e293b; margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .cell-meta { font-size: 11px; color: var(--text-muted); line-height: 1.3; }
            .edit-hint { font-size: 10px; color: var(--primary); font-weight: 700; margin-top: 5px; display: none; text-align: right; }
            .course-card:hover .edit-hint { display: block; }

            .alert-box { padding: 14px 18px; border-radius: 8px; font-size: 14px; margin-bottom: 16px; }
            .alert-box.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
            .alert-box.warning { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; }
            .simple-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
            .simple-table th, .simple-table td { padding: 10px 14px; border-bottom: 1px solid var(--border); font-size: 13px; text-align: left; }
            .simple-table th { background: #f8fafc; font-weight: 600; }
            .highlight-badge { background: #f1f5f9; padding: 8px 12px; border-radius: 6px; font-weight: 600; color: #1e293b; border-left: 4px solid var(--primary); }

            /* Modal Styling */
            .modal-backdrop { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(15, 23, 42, 0.6); display: flex; justify-content: center; align-items: center; z-index: 1000; backdrop-filter: blur(4px); }
            .modal-content { background: white; border-radius: 14px; width: 560px; max-width: 90%; padding: 24px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); animation: modalIn 0.2s ease-out; }
            .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 12px; }
            .modal-header h3 { font-size: 18px; font-weight: 700; color: #1e293b; }
            .close-btn { background: none; border: none; font-size: 24px; color: #94a3b8; cursor: pointer; }
            .close-btn:hover { color: #0f172a; }
            .form-group { margin-bottom: 16px; }
            .form-group label { display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; }
            .form-input { width: 100%; padding: 9px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px; outline: none; transition: border-color 0.15s; }
            .form-input:focus { border-color: var(--primary); }
            .form-row { display: flex; gap: 14px; }
            .flex-1 { flex: 1; }
            .checkbox-group { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
            .checkbox-group label { margin-bottom: 0; font-weight: normal; font-size: 13px; cursor: pointer; }
            .modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border); }

            @keyframes modalIn {
                from { transform: scale(0.95); opacity: 0; }
                to { transform: scale(1); opacity: 1; }
            }
            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(-5px); }
                to { opacity: 1; transform: translateY(0); }
            }

            @media print {
                body { padding: 0; background: white; }
                .no-print { display: none !important; }
                .tab-content { display: block !important; page-break-after: always; }
                .batch-view, .faculty-view, .room-view { display: block !important; page-break-after: always; }
                .table-responsive { box-shadow: none; border: 1px solid #000; }
                .course-card { box-shadow: none; border: 1px solid #999; }
            }
            """;
    }

    private static String getJavaScript() {
        return """
            function showTab(tabName) {
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
                
                const targetBtn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.innerText.toLowerCase().includes(tabName.substring(0, 4)));
                if (targetBtn) targetBtn.classList.add('active');
                
                const targetContent = document.getElementById('tab-' + tabName);
                if (targetContent) targetContent.classList.add('active');
            }

            function filterBatch() {
                const val = document.getElementById('batchSelect').value;
                document.querySelectorAll('.batch-view').forEach(v => {
                    v.style.display = (v.id === 'view_batch_' + val) ? 'block' : 'none';
                });
            }

            function filterFaculty() {
                const val = document.getElementById('facultySelect').value;
                document.querySelectorAll('.faculty-view').forEach(v => {
                    v.style.display = (v.id === 'view_faculty_' + val) ? 'block' : 'none';
                });
            }

            function filterRoom() {
                const val = document.getElementById('roomSelect').value;
                document.querySelectorAll('.room-view').forEach(v => {
                    v.style.display = (v.id === 'view_room_' + val) ? 'block' : 'none';
                });
            }

            // Modal Controls
            function openLectureModal() {
                document.getElementById('lectureModal').style.display = 'flex';
            }

            function closeLectureModal() {
                document.getElementById('lectureModal').style.display = 'none';
            }

            function openEditSlotModal(sessionId, courseCode, courseName, day, slotIndex, roomId, instructorId) {
                document.getElementById('eSessionId').value = sessionId;
                document.getElementById('eCourseDisplay').innerText = courseCode + ' - ' + courseName;
                document.getElementById('eDay').value = day;
                document.getElementById('ePeriod').value = slotIndex;
                document.getElementById('eRoom').value = roomId;
                document.getElementById('eInstructor').value = instructorId;
                document.getElementById('editSlotModal').style.display = 'flex';
            }

            function closeEditSlotModal() {
                document.getElementById('editSlotModal').style.display = 'none';
            }

            function handleEmptySlotClick(day, slotIndex, mode, contextId) {
                // If user clicks an empty slot, prefill add lecture form or open quick add
                openLectureModal();
                if (mode === 'batch') {
                    document.getElementById('mBatch').value = contextId;
                } else if (mode === 'faculty') {
                    document.getElementById('mInstructor').value = contextId;
                }
            }

            function toggleNewBatch(val) {
                const inp = document.getElementById('mNewBatchName');
                inp.style.display = (val === '__NEW__') ? 'block' : 'none';
                if (val === '__NEW__') inp.focus();
            }

            function toggleNewInstructor(val) {
                const inp = document.getElementById('mNewInstructorName');
                inp.style.display = (val === '__NEW__') ? 'block' : 'none';
                if (val === '__NEW__') inp.focus();
            }

            function toggleLabRequirement(checked) {
                const roomSelect = document.getElementById('mRoomType');
                if (checked && roomSelect.value !== 'COMPUTER_LAB' && roomSelect.value !== 'SCIENCE_LAB') {
                    roomSelect.value = 'COMPUTER_LAB';
                }
            }

            function showNotification(msg, type) {
                const banner = document.getElementById('liveNotification');
                banner.className = 'notification-banner ' + type;
                banner.innerHTML = msg;
                banner.style.display = 'block';
                if (type === 'success') {
                    setTimeout(() => { banner.style.display = 'none'; }, 6000);
                }
            }

            // Submit Edited Slot
            async function submitSlotEdit(e) {
                e.preventDefault();
                const saveBtn = document.getElementById('saveEditBtn');
                saveBtn.disabled = true;
                saveBtn.innerText = '💾 Saving...';

                const payload = {
                    sessionId: document.getElementById('eSessionId').value,
                    day: document.getElementById('eDay').value,
                    slotIndex: parseInt(document.getElementById('ePeriod').value),
                    roomId: document.getElementById('eRoom').value,
                    instructorId: document.getElementById('eInstructor').value
                };

                try {
                    const response = await fetch('/api/slot/edit', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });

                    if (!response.ok) {
                        const err = await response.json();
                        throw new Error(err.error || 'Server error occurred');
                    }

                    const res = await response.json();
                    closeEditSlotModal();
                    
                    if (res.hardConflicts > 0) {
                        showNotification('⚠️ Warning: Move created ' + res.hardConflicts + ' conflict(s)! Check Diagnostics tab.', 'warning');
                    } else {
                        showNotification('✨ Class updated successfully with 0 conflicts!', 'success');
                    }
                    
                    setTimeout(() => { window.location.reload(); }, 600);
                } catch (err) {
                    if (window.location.protocol === 'file:') {
                        alert("⚠️ Please run the web server: ./run.sh --web 8080 to edit interactively.");
                    } else {
                        alert("Error saving edit: " + err.message);
                    }
                } finally {
                    saveBtn.disabled = false;
                    saveBtn.innerText = '💾 Save Changes';
                }
            }

            // Delete Current Scheduled Slot
            async function deleteCurrentSlot() {
                if (!confirm("Are you sure you want to remove this class from the schedule?")) return;
                const sessionId = document.getElementById('eSessionId').value;
                try {
                    const response = await fetch('/api/slot/delete', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ sessionId: sessionId })
                    });
                    if (response.ok) {
                        closeEditSlotModal();
                        showNotification('🗑️ Class removed from schedule!', 'info');
                        setTimeout(() => { window.location.reload(); }, 600);
                    } else {
                        const err = await response.json();
                        alert("Error: " + err.error);
                    }
                } catch (err) {
                    alert("Error: " + err.message);
                }
            }

            // Submit new lecture to backend
            async function submitNewLecture(e) {
                e.preventDefault();
                const submitBtn = document.getElementById('submitBtn');
                submitBtn.disabled = true;
                submitBtn.innerText = '⏳ Optimizing Timetable...';

                const payload = {
                    courseCode: document.getElementById('mCourseCode').value.trim(),
                    courseName: document.getElementById('mCourseName').value.trim(),
                    groupId: document.getElementById('mBatch').value,
                    newGroupName: document.getElementById('mNewBatchName').value.trim(),
                    instructorId: document.getElementById('mInstructor').value,
                    newInstructorName: document.getElementById('mNewInstructorName').value.trim(),
                    weeklyLectures: parseInt(document.getElementById('mWeeklyLectures').value),
                    requiresLab: document.getElementById('mRequiresLab').checked,
                    roomType: document.getElementById('mRoomType').value
                };

                try {
                    const response = await fetch('/api/lecture/add', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });

                    if (!response.ok) {
                        const err = await response.json();
                        throw new Error(err.error || 'Server error occurred');
                    }

                    const res = await response.json();
                    closeLectureModal();
                    showNotification('✨ ' + res.message, 'success');
                    
                    setTimeout(() => {
                        window.location.reload();
                    }, 1200);

                } catch (err) {
                    if (window.location.protocol === 'file:') {
                        alert("⚠️ To add lectures interactively in the browser, please run the Web Server:\\n\\n./run.sh --web 8080\\n\\nThen open http://localhost:8080/ in your browser.");
                    } else {
                        alert("Error adding lecture: " + err.message);
                    }
                } finally {
                    submitBtn.disabled = false;
                    submitBtn.innerText = '🚀 Add & Update Timetable';
                }
            }

            // Trigger re-generation
            async function regenerateTimetable() {
                try {
                    showNotification('🔄 Re-running Genetic Algorithm optimizer...', 'info');
                    const res = await fetch('/api/regenerate', { method: 'POST' });
                    if (res.ok) {
                        showNotification('✅ Timetable re-optimized successfully!', 'success');
                        setTimeout(() => { window.location.reload(); }, 800);
                    }
                } catch (err) {
                    if (window.location.protocol === 'file:') {
                        alert("⚠️ To re-generate from the browser, launch the server:\\n./run.sh --web 8080");
                    } else {
                        alert("Error: " + err.message);
                    }
                }
            }

            // Init on load
            window.addEventListener('DOMContentLoaded', () => {
                filterBatch();
                filterFaculty();
                filterRoom();
            });
            """;
    }
}
