package com.timetabler;

import com.timetabler.exporter.ConsoleExporter;
import com.timetabler.exporter.CsvExporter;
import com.timetabler.exporter.HtmlExporter;
import com.timetabler.exporter.JsonExporter;
import com.timetabler.ga.GAParameters;
import com.timetabler.ga.GeneticAlgorithm;
import com.timetabler.model.*;
import com.timetabler.util.SampleDataGenerator;
import com.timetabler.web.TimetableWebServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the Automated Timetable Generator.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   AUTOMATED TIMETABLE GENERATOR - AI GENETIC ALGORITHM ENGINE            ");
        System.out.println("==========================================================================");

        int generations = 500;
        int populationSize = 120;
        double mutationRate = 0.08;
        boolean startWeb = false;
        boolean interactive = false;
        int webPort = 8080;
        String htmlOutputPath = "output/timetable.html";
        String csvOutputPath = "output/timetable.csv";
        String jsonOutputPath = "output/timetable.json";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--generations":
                case "-g":
                    if (i + 1 < args.length) generations = Integer.parseInt(args[++i]);
                    break;
                case "--population":
                case "-p":
                    if (i + 1 < args.length) populationSize = Integer.parseInt(args[++i]);
                    break;
                case "--mutation":
                case "-m":
                    if (i + 1 < args.length) mutationRate = Double.parseDouble(args[++i]);
                    break;
                case "--web":
                case "-w":
                    startWeb = true;
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        webPort = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--interactive":
                case "-i":
                    interactive = true;
                    break;
                case "--html":
                    if (i + 1 < args.length) htmlOutputPath = args[++i];
                    break;
                case "--csv":
                    if (i + 1 < args.length) csvOutputPath = args[++i];
                    break;
                case "--json":
                    if (i + 1 < args.length) jsonOutputPath = args[++i];
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
            }
        }

        // 1. Load Dataset
        System.out.println("[*] Loading academic institutional dataset...");
        TimetableData data = SampleDataGenerator.createRealisticUniversityDataset();

        System.out.printf("    - Departments/Programs: 3%n");
        System.out.printf("    - Student Batches:      %d%n", data.getStudentGroups().size());
        System.out.printf("    - Total Courses:        %d%n", data.getCourses().size());
        System.out.printf("    - Faculty / Lecturers:  %d%n", data.getInstructors().size());
        System.out.printf("    - Classrooms & Labs:    %d%n", data.getRooms().size());
        System.out.printf("    - Weekly Time Slots:    %d%n", data.getTimeSlots().size());
        System.out.printf("    - Class Sessions to Fit:%d%n", data.getClassSessions().size());

        // 2. Initial Solve
        Timetable timetable = solveTimetable(data, generations, populationSize, mutationRate, true);

        // 3. Print Summary & Export Initial Files
        ConsoleExporter.printSummary(timetable, data);
        exportAll(timetable, data, htmlOutputPath, csvOutputPath, jsonOutputPath);

        // 4. Interactive Mode if requested
        if (interactive) {
            runInteractiveLoop(data, timetable, generations, populationSize, mutationRate, htmlOutputPath, csvOutputPath, jsonOutputPath, webPort);
            return;
        }

        // 5. Start Web Server if requested
        if (startWeb) {
            startWebServer(data, timetable, webPort);
        } else {
            System.out.println("\n==========================================================================");
            System.out.println("💡 HOW TO ADD LECTURES & UPDATE TIMETABLE:");
            System.out.println("  1. Live Web Interface: Run './run.sh --web 8080'");
            System.out.println("     Open http://localhost:8080/ -> Click '➕ Add Lecture / Subject'");
            System.out.println("     Add course code, batch, instructor & hours -> Timetable auto-updates!");
            System.out.println("  2. Interactive Terminal: Run './run.sh -i'");
            System.out.println("     Type lecture details in CLI and see the instant regenerated schedule!");
            System.out.println("==========================================================================");
        }
    }

    public static Timetable solveTimetable(TimetableData data, int generations, int populationSize,
                                           double mutationRate, boolean logProgress) {
        GAParameters params = new GAParameters();
        params.setMaxGenerations(generations);
        params.setPopulationSize(populationSize);
        params.setMutationRate(mutationRate);
        params.setCrossoverRate(0.85);
        params.setEliteCount(4);
        params.setTournamentSize(5);
        params.setUseLocalSearch(true);

        GeneticAlgorithm ga = new GeneticAlgorithm(data, params);

        if (logProgress) {
            System.out.println("\n[*] Optimizing timetable schedule (Searching for 0 hard conflicts)...");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-10s | %-12s | %-16s | %-14s | %-10s%n",
                    "GEN", "FITNESS", "HARD CONFLICTS", "SOFT PENALTY", "TIME");
            System.out.println("--------------------------------------------------------------------------");
        }

        long startClock = System.currentTimeMillis();
        Timetable timetable = ga.solve((gen, best, elapsedMs) -> {
            if (logProgress) {
                System.out.printf("Gen %-6d | %-12.4f | %-16d | %-14d | %d ms%n",
                        gen, best.getFitness(), best.getHardConflicts(), best.getSoftPenalty(), elapsedMs);
            }
        });
        long totalClock = System.currentTimeMillis() - startClock;

        if (logProgress) {
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("[✓] Optimization completed in %d ms!%n%n", totalClock);
        }
        return timetable;
    }

    public static void exportAll(Timetable timetable, TimetableData data, String htmlPath, String csvPath, String jsonPath) {
        try {
            new File("output").mkdirs();
            HtmlExporter.exportToFile(timetable, data, htmlPath);
            System.out.println("[✓] Interactive HTML Dashboard exported to: " + new File(htmlPath).getAbsolutePath());

            CsvExporter.exportToFile(timetable, data, csvPath);
            System.out.println("[✓] CSV Timetable exported to:               " + new File(csvPath).getAbsolutePath());

            JsonExporter.exportToFile(timetable, data, jsonPath);
            System.out.println("[✓] JSON Timetable exported to:              " + new File(jsonPath).getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error saving export files: " + e.getMessage());
        }
    }

    private static void runInteractiveLoop(TimetableData data, Timetable timetable, int generations,
                                           int populationSize, double mutationRate,
                                           String htmlPath, String csvPath, String jsonPath, int webPort) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n==========================================================================");
            System.out.println("                      INTERACTIVE TIMETABLE MENU                          ");
            System.out.println("==========================================================================");
            System.out.println(" 1. View Timetable for a Student Batch");
            System.out.println(" 2. View Timetable for a Faculty / Instructor");
            System.out.println(" 3. ➕ Add a New Lecture / Course & Auto-Update Timetable");
            System.out.println(" 4. 🔄 Re-optimize Current Timetable");
            System.out.println(" 5. 🌐 Launch Live Web Dashboard Server");
            System.out.println(" 6. Exit");
            System.out.print("\nEnter your choice (1-6): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": {
                    List<StudentGroup> groups = new ArrayList<>(data.getStudentGroups().values());
                    System.out.println("\nAvailable Batches:");
                    for (int i = 0; i < groups.size(); i++) {
                        System.out.printf("  %d. %s%n", i + 1, groups.get(i).getName());
                    }
                    System.out.print("Select batch number: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx >= 0 && idx < groups.size()) {
                            ConsoleExporter.printGroupTimetable(timetable, data, groups.get(idx).getId());
                        } else {
                            System.out.println("Invalid selection.");
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                    }
                    break;
                }
                case "2": {
                    List<Instructor> insts = new ArrayList<>(data.getInstructors().values());
                    System.out.println("\nAvailable Instructors:");
                    for (int i = 0; i < insts.size(); i++) {
                        System.out.printf("  %d. %s [%s]%n", i + 1, insts.get(i).getName(), insts.get(i).getDepartment());
                    }
                    System.out.print("Select instructor number: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx >= 0 && idx < insts.size()) {
                            ConsoleExporter.printInstructorTimetable(timetable, data, insts.get(idx).getId());
                        } else {
                            System.out.println("Invalid selection.");
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                    }
                    break;
                }
                case "3": {
                    System.out.println("\n--- ➕ Add New Lecture / Course ---");
                    System.out.print("Course Code (e.g. CS401, MATH205): ");
                    String code = scanner.nextLine().trim().toUpperCase();
                    if (code.isEmpty()) {
                        System.out.println("Course code cannot be empty.");
                        break;
                    }

                    System.out.print("Course Title / Subject Name (e.g. Cloud Computing): ");
                    String name = scanner.nextLine().trim();
                    if (name.isEmpty()) name = code;

                    // Choose batch
                    List<StudentGroup> groups = new ArrayList<>(data.getStudentGroups().values());
                    System.out.println("Select Target Batch:");
                    for (int i = 0; i < groups.size(); i++) {
                        System.out.printf("  %d. %s%n", i + 1, groups.get(i).getName());
                    }
                    System.out.printf("  %d. [+ Add New Batch]%n", groups.size() + 1);
                    System.out.print("Choice: ");
                    String groupId;
                    int bChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (bChoice == groups.size() + 1) {
                        System.out.print("Enter New Batch Name: ");
                        String newBName = scanner.nextLine().trim();
                        String newBId = "GRP_" + System.currentTimeMillis();
                        data.addStudentGroup(new StudentGroup(newBId, newBName, 40));
                        groupId = newBId;
                    } else {
                        groupId = groups.get(bChoice - 1).getId();
                    }

                    // Choose Instructor
                    List<Instructor> insts = new ArrayList<>(data.getInstructors().values());
                    System.out.println("Select Faculty / Instructor:");
                    for (int i = 0; i < insts.size(); i++) {
                        System.out.printf("  %d. %s%n", i + 1, insts.get(i).getName());
                    }
                    System.out.printf("  %d. [+ Add New Faculty]%n", insts.size() + 1);
                    System.out.print("Choice: ");
                    String instId;
                    int iChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (iChoice == insts.size() + 1) {
                        System.out.print("Enter New Faculty Name: ");
                        String newIName = scanner.nextLine().trim();
                        String newIId = "INS_" + System.currentTimeMillis();
                        data.addInstructor(new Instructor(newIId, newIName, "faculty@univ.edu", "General", 4));
                        instId = newIId;
                    } else {
                        instId = insts.get(iChoice - 1).getId();
                    }

                    System.out.print("Weekly Lectures count (e.g. 1 to 5): ");
                    int weekly = Integer.parseInt(scanner.nextLine().trim());

                    System.out.print("Is this a Lab course? (y/n): ");
                    boolean isLab = scanner.nextLine().trim().equalsIgnoreCase("y");

                    RoomType rType = isLab ? RoomType.COMPUTER_LAB : RoomType.CLASSROOM;

                    // Add course & update
                    data.addOrUpdateLectureCourse(code, name, groupId, instId, weekly, isLab, rType);
                    System.out.println("\n[✓] Lecture registered! Re-optimizing timetable schedule...");

                    timetable = solveTimetable(data, generations, populationSize, mutationRate, true);
                    ConsoleExporter.printSummary(timetable, data);
                    ConsoleExporter.printGroupTimetable(timetable, data, groupId);
                    exportAll(timetable, data, htmlPath, csvPath, jsonPath);
                    break;
                }
                case "4": {
                    System.out.println("\n[*] Re-optimizing timetable...");
                    timetable = solveTimetable(data, generations, populationSize, mutationRate, true);
                    ConsoleExporter.printSummary(timetable, data);
                    exportAll(timetable, data, htmlPath, csvPath, jsonPath);
                    break;
                }
                case "5": {
                    startWebServer(data, timetable, webPort);
                    return;
                }
                case "6":
                    System.out.println("Exiting Timetable Generator. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option, please choose 1-6.");
            }
        }
    }

    private static void startWebServer(TimetableData data, Timetable timetable, int port) {
        try {
            TimetableWebServer webServer = new TimetableWebServer(port, data, timetable);
            webServer.start();
            System.out.println("\n==========================================================================");
            System.out.println(" Web Server running at: http://localhost:" + port + "/");
            System.out.println(" Use the browser to click '➕ Add Lecture / Subject' anytime!");
            System.out.println(" Press Ctrl+C in terminal to stop server.");
            System.out.println("==========================================================================");
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java com.timetabler.Main [options]");
        System.out.println("Options:");
        System.out.println("  -i, --interactive        Interactive terminal menu to view & add lectures");
        System.out.println("  -w, --web [port]         Start live HTTP web dashboard server (default: 8080)");
        System.out.println("  -g, --generations <N>    Maximum GA generations (default: 500)");
        System.out.println("  -p, --population <N>     Population size (default: 120)");
        System.out.println("  -m, --mutation <rate>    Mutation probability (default: 0.08)");
        System.out.println("  --html <path>            Path for HTML output (default: output/timetable.html)");
        System.out.println("  --csv <path>             Path for CSV output (default: output/timetable.csv)");
        System.out.println("  --json <path>            Path for JSON output (default: output/timetable.json)");
        System.out.println("  -h, --help               Display this help message");
    }
}
