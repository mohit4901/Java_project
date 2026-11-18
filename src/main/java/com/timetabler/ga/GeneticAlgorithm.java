package com.timetabler.ga;

import com.timetabler.constraint.FitnessEvaluator;
import com.timetabler.model.*;

import java.util.*;

/**
 * Genetic Algorithm solver that evolves timetables toward 0 hard conflicts and minimal soft penalties.
 */
public class GeneticAlgorithm {

    public interface GenerationListener {
        void onGeneration(int generation, Individual best, long elapsedMs);
    }

    private final TimetableData data;
    private final GAParameters params;
    private final FitnessEvaluator evaluator;
    private final Random random;

    public GeneticAlgorithm(TimetableData data, GAParameters params) {
        this.data = data;
        this.params = params;
        this.evaluator = new FitnessEvaluator(data);
        this.random = new Random();
    }

    public Timetable solve(GenerationListener listener) {
        long startTime = System.currentTimeMillis();
        int popSize = params.getPopulationSize();
        List<Individual> population = new ArrayList<>(popSize);

        // 1. Initialize Population
        for (int i = 0; i < popSize; i++) {
            Individual ind = Individual.createRandom(data, random);
            ind.evaluate(evaluator, false);
            population.add(ind);
        }

        Collections.sort(population);
        Individual bestEver = population.get(0).copy();

        if (listener != null) {
            listener.onGeneration(0, bestEver, System.currentTimeMillis() - startTime);
        }

        // Check if initial generation already solved it
        if (bestEver.getHardConflicts() == 0 && bestEver.getSoftPenalty() == 0) {
            bestEver.evaluate(evaluator, true);
            return bestEver.toTimetable();
        }

        // 2. Evolution Loop
        for (int gen = 1; gen <= params.getMaxGenerations(); gen++) {
            List<Individual> nextGen = new ArrayList<>(popSize);

            // Elitism: Preserve the best individuals
            int eliteCount = Math.min(params.getEliteCount(), popSize);
            for (int e = 0; e < eliteCount; e++) {
                nextGen.add(population.get(e).copy());
            }

            // Breed the rest
            while (nextGen.size() < popSize) {
                Individual parent1 = tournamentSelect(population);
                Individual parent2 = tournamentSelect(population);

                Individual child;
                if (random.nextDouble() < params.getCrossoverRate()) {
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.copy();
                }

                mutate(child);

                // Optional local repair heuristic to quickly unstick conflicts
                if (params.isUseLocalSearch() && random.nextDouble() < 0.25) {
                    localRepair(child);
                }

                child.evaluate(evaluator, false);
                nextGen.add(child);
            }

            population = nextGen;
            Collections.sort(population);

            Individual currentBest = population.get(0);
            if (currentBest.getFitness() > bestEver.getFitness()) {
                bestEver = currentBest.copy();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (listener != null && (gen % 5 == 0 || gen == params.getMaxGenerations() || bestEver.getHardConflicts() == 0)) {
                listener.onGeneration(gen, bestEver, elapsed);
            }

            // Early stopping condition: 0 hard conflicts and low soft penalty
            if (bestEver.getHardConflicts() == 0 && (bestEver.getSoftPenalty() <= 10 || gen >= 200)) {
                if (bestEver.getHardConflicts() == 0) {
                    // We found a feasible timetable! Continue slightly if soft penalty can improve, or exit.
                    if (bestEver.getSoftPenalty() == 0 || gen >= 300) {
                        break;
                    }
                }
            }
        }

        // Evaluate best solution with full diagnostics
        bestEver.evaluate(evaluator, true);
        return bestEver.toTimetable();
    }

    /**
     * Tournament selection: chooses k random individuals and returns the best among them.
     */
    private Individual tournamentSelect(List<Individual> population) {
        int k = params.getTournamentSize();
        Individual best = null;
        for (int i = 0; i < k; i++) {
            Individual contender = population.get(random.nextInt(population.size()));
            if (best == null || contender.getFitness() > best.getFitness()) {
                best = contender;
            }
        }
        return best;
    }

    /**
     * Uniform crossover combining genes from both parents.
     */
    private Individual crossover(Individual parent1, Individual parent2) {
        int length = parent1.size();
        Individual child = new Individual(length);

        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                child.setGene(i, parent1.getGene(i));
            } else {
                child.setGene(i, parent2.getGene(i));
            }
        }
        return child;
    }

    /**
     * Mutates individual genes according to mutation rate.
     */
    private void mutate(Individual ind) {
        List<TimeSlot> allSlots = data.getTimeSlots();
        int geneCount = ind.size();

        for (int i = 0; i < geneCount; i++) {
            if (random.nextDouble() < params.getMutationRate()) {
                ScheduleSlot current = ind.getGene(i);
                ClassSession session = current.getSession();
                Course course = data.getCourse(session.getCourseId());
                StudentGroup group = data.getStudentGroup(session.getStudentGroupId());

                // Randomly decide which attribute of the gene to mutate:
                int choice = random.nextInt(3);
                TimeSlot newSlot = current.getTimeSlot();
                Room newRoom = current.getRoom();
                Instructor newInst = current.getInstructor();

                if (choice == 0) {
                    // Mutate TimeSlot
                    newSlot = allSlots.get(random.nextInt(allSlots.size()));
                } else if (choice == 1) {
                    // Mutate Room (to compatible room)
                    List<Room> compatible = data.getCompatibleRooms(course, group);
                    if (!compatible.isEmpty()) {
                        newRoom = compatible.get(random.nextInt(compatible.size()));
                    }
                } else {
                    // Mutate Instructor (to qualified instructor)
                    List<Instructor> qualified = data.getQualifiedInstructors(course.getId());
                    if (!qualified.isEmpty()) {
                        newInst = qualified.get(random.nextInt(qualified.size()));
                    }
                }

                ind.setGene(i, new ScheduleSlot(session, newSlot, newRoom, newInst));
            }
        }
    }

    /**
     * Local repair heuristic: Identifies colliding sessions and attempts to move them
     * to less crowded time slots.
     */
    private void localRepair(Individual ind) {
        List<TimeSlot> slots = data.getTimeSlots();
        // Pick 2 random genes and try moving to a random open slot
        for (int step = 0; step < 2; step++) {
            int idx = random.nextInt(ind.size());
            ScheduleSlot current = ind.getGene(idx);
            TimeSlot targetSlot = slots.get(random.nextInt(slots.size()));
            ind.setGene(idx, new ScheduleSlot(current.getSession(), targetSlot, current.getRoom(), current.getInstructor()));
        }
    }
}
