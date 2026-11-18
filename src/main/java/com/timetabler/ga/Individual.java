package com.timetabler.ga;

import com.timetabler.constraint.FitnessEvaluator;
import com.timetabler.model.*;

import java.util.*;

/**
 * An individual chromosome in the genetic algorithm representing a candidate timetable solution.
 */
public class Individual implements Comparable<Individual> {
    private final ScheduleSlot[] genes;
    private double fitness = -1;
    private int hardConflicts = -1;
    private int softPenalty = -1;
    private List<String> diagnostics = null;

    public Individual(int geneLength) {
        this.genes = new ScheduleSlot[geneLength];
    }

    public Individual(ScheduleSlot[] genes) {
        this.genes = Arrays.copyOf(genes, genes.length);
    }

    /**
     * Initializes random genes for all sessions using domain-guided selection.
     */
    public static Individual createRandom(TimetableData data, Random random) {
        List<ClassSession> sessions = data.getClassSessions();
        List<TimeSlot> slots = data.getTimeSlots();
        Individual ind = new Individual(sessions.size());

        for (int i = 0; i < sessions.size(); i++) {
            ClassSession session = sessions.get(i);
            Course course = data.getCourse(session.getCourseId());
            StudentGroup group = data.getStudentGroup(session.getStudentGroupId());

            // Pick random timeslot
            TimeSlot ts = slots.get(random.nextInt(slots.size()));

            // Pick random compatible room
            List<Room> compatibleRooms = data.getCompatibleRooms(course, group);
            Room room = compatibleRooms.get(random.nextInt(compatibleRooms.size()));

            // Pick qualified instructor
            List<Instructor> qualifiedInstructors = data.getQualifiedInstructors(course.getId());
            Instructor inst;
            if (!qualifiedInstructors.isEmpty()) {
                inst = qualifiedInstructors.get(random.nextInt(qualifiedInstructors.size()));
            } else {
                // fallback to any instructor if none specifically qualified
                List<Instructor> allInst = new ArrayList<>(data.getInstructors().values());
                inst = allInst.get(random.nextInt(allInst.size()));
            }

            ind.setGene(i, new ScheduleSlot(session, ts, room, inst));
        }

        return ind;
    }

    public ScheduleSlot getGene(int index) {
        return genes[index];
    }

    public void setGene(int index, ScheduleSlot slot) {
        genes[index] = slot;
        this.fitness = -1; // invalidate cache
    }

    public ScheduleSlot[] getGenes() {
        return genes;
    }

    public int size() {
        return genes.length;
    }

    public double getFitness() {
        return fitness;
    }

    public int getHardConflicts() {
        return hardConflicts;
    }

    public int getSoftPenalty() {
        return softPenalty;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Evaluates this individual using the given FitnessEvaluator.
     */
    public void evaluate(FitnessEvaluator evaluator, boolean collectDiagnostics) {
        FitnessEvaluator.EvaluationResult res = evaluator.evaluate(Arrays.asList(genes), collectDiagnostics);
        this.fitness = res.fitness;
        this.hardConflicts = res.hardConflicts;
        this.softPenalty = res.softPenalty;
        this.diagnostics = res.diagnosticViolations;
    }

    public boolean isEvaluated() {
        return fitness >= 0;
    }

    /**
     * Converts this individual into a Timetable model.
     */
    public Timetable toTimetable() {
        Timetable tt = new Timetable(Arrays.asList(genes));
        tt.setFitness(this.fitness);
        tt.setHardConflicts(this.hardConflicts);
        tt.setSoftConflicts(this.softPenalty);
        tt.setDiagnosticViolations(this.diagnostics);
        return tt;
    }

    public Individual copy() {
        Individual copy = new Individual(this.genes);
        copy.fitness = this.fitness;
        copy.hardConflicts = this.hardConflicts;
        copy.softPenalty = this.softPenalty;
        if (this.diagnostics != null) {
            copy.diagnostics = new ArrayList<>(this.diagnostics);
        }
        return copy;
    }

    @Override
    public int compareTo(Individual o) {
        // Higher fitness is better
        return Double.compare(o.fitness, this.fitness);
    }
}
