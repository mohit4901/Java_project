package com.timetabler.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.timetabler.exporter.CsvExporter;
import com.timetabler.exporter.HtmlExporter;
import com.timetabler.exporter.JsonExporter;
import com.timetabler.ga.GAParameters;
import com.timetabler.ga.GeneticAlgorithm;
import com.timetabler.model.*;
import com.timetabler.util.JsonHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Built-in HTTP server supporting live timetable dashboard, REST APIs,
 * and real-time lecture / course addition with automatic re-scheduling.
 */
public class TimetableWebServer {

    private final int port;
    private final TimetableData data;
    private Timetable currentTimetable;
    private HttpServer server;

    public TimetableWebServer(int port, TimetableData data, Timetable initialTimetable) {
        this.port = port;
        this.data = data;
        this.currentTimetable = initialTimetable;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 1. Web Dashboard HTML Page
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                    return;
                }
                String html = HtmlExporter.generateHtml(currentTimetable, data);
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        // 2. GET /api/timetable: Returns current timetable JSON
        server.createContext("/api/timetable", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                String json = JsonExporter.toJson(currentTimetable, data);
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
            }
        });

        // 3. GET /api/options: Returns list of batches, instructors, and rooms for form dropdowns
        server.createContext("/api/options", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                StringBuilder sb = new StringBuilder("{");

                // Batches
                sb.append("\"batches\": [");
                int idx = 0;
                for (StudentGroup g : data.getStudentGroups().values()) {
                    sb.append("{\"id\":\"").append(g.getId()).append("\",\"name\":\"").append(escape(g.getName()))
                      .append("\",\"size\":").append(g.getNumberOfStudents()).append("}");
                    if (++idx < data.getStudentGroups().size()) sb.append(",");
                }
                sb.append("],");

                // Instructors
                sb.append("\"instructors\": [");
                idx = 0;
                for (Instructor inst : data.getInstructors().values()) {
                    sb.append("{\"id\":\"").append(inst.getId()).append("\",\"name\":\"").append(escape(inst.getName()))
                      .append("\",\"dept\":\"").append(escape(inst.getDepartment())).append("\"}");
                    if (++idx < data.getInstructors().size()) sb.append(",");
                }
                sb.append("],");

                // Rooms
                sb.append("\"rooms\": [");
                idx = 0;
                for (Room r : data.getRooms().values()) {
                    sb.append("{\"id\":\"").append(r.getId()).append("\",\"name\":\"").append(escape(r.getName()))
                      .append("\",\"cap\":").append(r.getCapacity()).append(",\"type\":\"").append(r.getType().name()).append("\"}");
                    if (++idx < data.getRooms().size()) sb.append(",");
                }
                sb.append("]");

                sb.append("}");
                sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
            }
        });

        // 4. POST /api/lecture/add: Adds new lecture/course and automatically re-generates timetable
        server.createContext("/api/lecture/add", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "application/json", "{\"error\":\"POST required\"}");
                    return;
                }

                String body = readBody(exchange);
                Map<String, String> params = JsonHelper.parseSimpleJson(body);

                String courseCode = JsonHelper.getString(params, "courseCode", "");
                String courseName = JsonHelper.getString(params, "courseName", "");
                String groupId = JsonHelper.getString(params, "groupId", "");
                String newGroupName = JsonHelper.getString(params, "newGroupName", "");
                String instructorId = JsonHelper.getString(params, "instructorId", "");
                String newInstructorName = JsonHelper.getString(params, "newInstructorName", "");
                int weeklyLectures = JsonHelper.getInt(params, "weeklyLectures", 3);
                boolean requiresLab = JsonHelper.getBoolean(params, "requiresLab", false);
                String roomTypeStr = JsonHelper.getString(params, "roomType", requiresLab ? "COMPUTER_LAB" : "CLASSROOM");

                if (courseCode.isEmpty()) {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"courseCode is required\"}");
                    return;
                }
                if (courseName.isEmpty()) {
                    courseName = courseCode;
                }

                // Handle dynamic new student group
                if ("__NEW__".equals(groupId) || (groupId.isEmpty() && !newGroupName.isEmpty())) {
                    String genGroupId = "GRP_" + System.currentTimeMillis();
                    StudentGroup newGroup = new StudentGroup(genGroupId, newGroupName.isEmpty() ? "Batch-" + courseCode : newGroupName, 40);
                    data.addStudentGroup(newGroup);
                    groupId = genGroupId;
                }

                // Handle dynamic new instructor
                if ("__NEW__".equals(instructorId) || (instructorId.isEmpty() && !newInstructorName.isEmpty())) {
                    String genInstId = "INS_" + System.currentTimeMillis();
                    Instructor newInst = new Instructor(genInstId, newInstructorName.isEmpty() ? "Faculty-" + courseCode : newInstructorName,
                            "faculty@univ.edu", "General", 4);
                    data.addInstructor(newInst);
                    instructorId = genInstId;
                }

                RoomType roomType;
                try {
                    roomType = RoomType.valueOf(roomTypeStr.toUpperCase());
                } catch (Exception e) {
                    roomType = requiresLab ? RoomType.COMPUTER_LAB : RoomType.CLASSROOM;
                }

                // Add or update the lecture course & enroll the batch
                data.addOrUpdateLectureCourse(courseCode, courseName, groupId, instructorId, weeklyLectures, requiresLab, roomType);

                // Run Genetic Algorithm to re-generate the schedule
                long t0 = System.currentTimeMillis();
                GAParameters gaParams = new GAParameters();
                gaParams.setMaxGenerations(500);
                gaParams.setPopulationSize(120);
                GeneticAlgorithm ga = new GeneticAlgorithm(data, gaParams);
                currentTimetable = ga.solve(null);
                long elapsed = System.currentTimeMillis() - t0;

                // Sync files on disk
                try {
                    new File("output").mkdirs();
                    HtmlExporter.exportToFile(currentTimetable, data, "output/timetable.html");
                    CsvExporter.exportToFile(currentTimetable, data, "output/timetable.csv");
                    JsonExporter.exportToFile(currentTimetable, data, "output/timetable.json");
                } catch (Exception ignored) {}

                String response = String.format(
                        "{\"status\":\"success\",\"message\":\"Lecture '%s' added successfully! Timetable updated in %d ms.\"," +
                        "\"fitness\":%.4f,\"hardConflicts\":%d,\"softPenalty\":%d,\"totalSessions\":%d}",
                        escape(courseCode), elapsed, currentTimetable.getFitness(),
                        currentTimetable.getHardConflicts(), currentTimetable.getSoftConflicts(),
                        currentTimetable.getSlots().size()
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
            }
        });

        // 5. POST /api/regenerate: Re-runs optimizer with existing courses
        server.createContext("/api/regenerate", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                GeneticAlgorithm ga = new GeneticAlgorithm(data, new GAParameters());
                currentTimetable = ga.solve(null);

                syncFiles();

                String json = "{\"status\": \"success\", \"fitness\": " + currentTimetable.getFitness()
                        + ", \"hardConflicts\": " + currentTimetable.getHardConflicts() + "}";
                sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
            }
        });

        // 6. POST /api/slot/edit: Edit a specific scheduled slot (change day, period, room, instructor)
        server.createContext("/api/slot/edit", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "application/json", "{\"error\":\"POST required\"}");
                    return;
                }

                String body = readBody(exchange);
                Map<String, String> params = JsonHelper.parseSimpleJson(body);

                String sessionId = JsonHelper.getString(params, "sessionId", "");
                String dayStr = JsonHelper.getString(params, "day", "MONDAY");
                int slotIndex = JsonHelper.getInt(params, "slotIndex", 0);
                String roomId = JsonHelper.getString(params, "roomId", "");
                String instructorId = JsonHelper.getString(params, "instructorId", "");

                DayOfWeek day;
                try {
                    day = DayOfWeek.valueOf(dayStr.toUpperCase());
                } catch (Exception e) {
                    day = DayOfWeek.MONDAY;
                }

                TimeSlot newSlot = data.getTimeSlot(day, slotIndex);
                Room newRoom = data.getRoom(roomId);
                Instructor newInst = data.getInstructor(instructorId);

                if (newSlot == null || newRoom == null || newInst == null) {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid slot, room, or instructor ID\"}");
                    return;
                }

                boolean updated = currentTimetable.updateSlot(sessionId, newSlot, newRoom, newInst);
                if (!updated) {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Session not found in schedule\"}");
                    return;
                }

                // Recalculate evaluation metrics
                recalculateTimetable();
                syncFiles();

                String response = String.format(
                        "{\"status\":\"success\",\"message\":\"Lecture slot updated successfully!\"," +
                        "\"hardConflicts\":%d,\"softPenalty\":%d,\"fitness\":%.4f,\"violations\":%d}",
                        currentTimetable.getHardConflicts(), currentTimetable.getSoftConflicts(),
                        currentTimetable.getFitness(), currentTimetable.getDiagnosticViolations().size()
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
            }
        });

        // 7. POST /api/slot/delete: Remove a scheduled lecture slot
        server.createContext("/api/slot/delete", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                setCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "application/json", "{\"error\":\"POST required\"}");
                    return;
                }

                String body = readBody(exchange);
                Map<String, String> params = JsonHelper.parseSimpleJson(body);
                String sessionId = JsonHelper.getString(params, "sessionId", "");

                boolean removed = currentTimetable.removeSlot(sessionId);
                if (!removed) {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Session not found\"}");
                    return;
                }

                recalculateTimetable();
                syncFiles();

                String response = String.format(
                        "{\"status\":\"success\",\"message\":\"Lecture slot removed!\"," +
                        "\"hardConflicts\":%d,\"totalSessions\":%d}",
                        currentTimetable.getHardConflicts(), currentTimetable.getSlots().size()
                );
                sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println(">>> Web Dashboard live at: http://localhost:" + port + "/");
    }

    private void recalculateTimetable() {
        com.timetabler.constraint.FitnessEvaluator evaluator = new com.timetabler.constraint.FitnessEvaluator(data);
        com.timetabler.constraint.FitnessEvaluator.EvaluationResult eval = evaluator.evaluate(currentTimetable.getSlots(), true);
        currentTimetable.setFitness(eval.fitness);
        currentTimetable.setHardConflicts(eval.hardConflicts);
        currentTimetable.setSoftConflicts(eval.softPenalty);
        currentTimetable.setDiagnosticViolations(eval.diagnosticViolations);
    }

    private void syncFiles() {
        try {
            new File("output").mkdirs();
            HtmlExporter.exportToFile(currentTimetable, data, "output/timetable.html");
            CsvExporter.exportToFile(currentTimetable, data, "output/timetable.csv");
            JsonExporter.exportToFile(currentTimetable, data, "output/timetable.json");
        } catch (Exception ignored) {}
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
