package com.timetabler;

import com.timetabler.constraint.FitnessEvaluator;
import com.timetabler.exporter.CsvExporter;
import com.timetabler.exporter.HtmlExporter;
import com.timetabler.exporter.JsonExporter;
import com.timetabler.ga.GAParameters;
import com.timetabler.ga.GeneticAlgorithm;
import com.timetabler.model.*;
import com.timetabler.util.SampleDataGenerator;

import java.io.File;
import java.util.List;

/**
 * Automated test suite to verify timetabling constraints, GA convergence, dynamic lecture addition, and exports.
 */
public class TimetableTest {

    public static void main(String[] args) {
        System.out.println(">>> Running Timetable Generator Automated Tests...");
        int passed = 0;
        int failed = 0;

        try {
            testDatasetGeneration();
            passed++;
            System.out.println("  [PASS] testDatasetGeneration");
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] testDatasetGeneration: " + t.getMessage());
        }

        try {
            testHardConstraintsDetection();
            passed++;
            System.out.println("  [PASS] testHardConstraintsDetection");
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] testHardConstraintsDetection: " + t.getMessage());
        }

        try {
            testGeneticAlgorithmSolveFeasibility();
            passed++;
            System.out.println("  [PASS] testGeneticAlgorithmSolveFeasibility");
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] testGeneticAlgorithmSolveFeasibility: " + t.getMessage());
        }

        try {
            testDynamicAddLectureAndRegenerate();
            passed++;
            System.out.println("  [PASS] testDynamicAddLectureAndRegenerate");
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] testDynamicAddLectureAndRegenerate: " + t.getMessage());
        }

        try {
            testExporters();
            passed++;
            System.out.println("  [PASS] testExporters");
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] testExporters: " + t.getMessage());
        }

        System.out.printf("%nTest Results: %d Passed, %d Failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testDatasetGeneration() {
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();
        if (data.getCourses().isEmpty()) throw new AssertionError("Courses should not be empty");
        if (data.getRooms().isEmpty()) throw new AssertionError("Rooms should not be empty");
        if (data.getInstructors().isEmpty()) throw new AssertionError("Instructors should not be empty");
        if (data.getStudentGroups().isEmpty()) throw new AssertionError("StudentGroups should not be empty");
        if (data.getTimeSlots().size() != 30) throw new AssertionError("Expected 30 time slots (5 days x 6 slots)");
        if (data.getClassSessions().isEmpty()) throw new AssertionError("ClassSessions should be generated");
    }

    private static void testHardConstraintsDetection() {
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();
        FitnessEvaluator evaluator = new FitnessEvaluator(data);

        TimeSlot slot1 = data.getTimeSlots().get(0);
        Room room1 = data.getRooms().values().iterator().next();
        Instructor inst1 = data.getInstructors().values().iterator().next();

        ClassSession sess1 = data.getClassSessions().get(0);
        ClassSession sess2 = data.getClassSessions().get(1);

        // Intentionally create a room double booking clash
        ScheduleSlot s1 = new ScheduleSlot(sess1, slot1, room1, inst1);
        ScheduleSlot s2 = new ScheduleSlot(sess2, slot1, room1, inst1);

        FitnessEvaluator.EvaluationResult res = evaluator.evaluate(List.of(s1, s2), true);
        if (res.hardConflicts == 0) {
            throw new AssertionError("Evaluator should detect double-booking conflict!");
        }
    }

    private static void testGeneticAlgorithmSolveFeasibility() {
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();
        GAParameters params = new GAParameters();
        params.setMaxGenerations(400);
        params.setPopulationSize(100);
        params.setMutationRate(0.08);

        GeneticAlgorithm ga = new GeneticAlgorithm(data, params);
        Timetable solution = ga.solve(null);

        if (solution.getHardConflicts() != 0) {
            throw new AssertionError("GA should find a feasible solution with 0 hard conflicts, found: " + solution.getHardConflicts());
        }
        if (solution.getSlots().size() != data.getClassSessions().size()) {
            throw new AssertionError("All sessions must be scheduled!");
        }
    }

    private static void testDynamicAddLectureAndRegenerate() {
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();
        int initialSessions = data.getClassSessions().size();

        // User adds a new lecture: CS405 (Distributed Systems, 3 lectures/week for GRP_CSE_Y1, taught by INS_TURING)
        data.addOrUpdateLectureCourse("CS405", "Distributed Systems", "GRP_CSE_Y1", "INS_TURING", 3, false, RoomType.CLASSROOM);

        if (data.getClassSessions().size() != initialSessions + 3) {
            throw new AssertionError("Expected sessions count to increase by 3, but was: " + data.getClassSessions().size());
        }

        GAParameters params = new GAParameters();
        params.setMaxGenerations(300);
        params.setPopulationSize(100);
        GeneticAlgorithm ga = new GeneticAlgorithm(data, params);
        Timetable updatedTimetable = ga.solve(null);

        if (updatedTimetable.getHardConflicts() != 0) {
            throw new AssertionError("Updated timetable should have 0 hard conflicts");
        }

        // Verify the newly added course CS405 is present in the schedule
        boolean foundNewCourse = false;
        for (ScheduleSlot s : updatedTimetable.getSlots()) {
            if ("CS405".equals(s.getSession().getCourseId())) {
                foundNewCourse = true;
                break;
            }
        }

        if (!foundNewCourse) {
            throw new AssertionError("Newly added lecture CS405 should appear in the scheduled timetable slots!");
        }
    }

    private static void testExporters() throws Exception {
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();
        GAParameters params = new GAParameters();
        params.setMaxGenerations(50);
        params.setPopulationSize(20);

        GeneticAlgorithm ga = new GeneticAlgorithm(data, params);
        Timetable solution = ga.solve(null);

        File tmpDir = new File("build/test_out");
        tmpDir.mkdirs();

        String htmlPath = "build/test_out/test.html";
        String csvPath = "build/test_out/test.csv";
        String jsonPath = "build/test_out/test.json";

        HtmlExporter.exportToFile(solution, data, htmlPath);
        CsvExporter.exportToFile(solution, data, csvPath);
        JsonExporter.exportToFile(solution, data, jsonPath);

        if (!new File(htmlPath).exists() || new File(htmlPath).length() == 0) {
            throw new AssertionError("HTML export failed");
        }
        if (!new File(csvPath).exists() || new File(csvPath).length() == 0) {
            throw new AssertionError("CSV export failed");
        }
        if (!new File(jsonPath).exists() || new File(jsonPath).length() == 0) {
            throw new AssertionError("JSON export failed");
        }
    }
}
