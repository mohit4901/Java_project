package com.timetabler.ga;

/**
 * Hyperparameters configuring the Genetic Algorithm solver.
 */
public class GAParameters {
    private int populationSize = 120;
    private double mutationRate = 0.08;
    private double crossoverRate = 0.85;
    private int tournamentSize = 5;
    private int eliteCount = 4;
    private int maxGenerations = 600;
    private boolean useLocalSearch = true;

    public GAParameters() {}

    public GAParameters(int populationSize, double mutationRate, double crossoverRate,
                        int tournamentSize, int eliteCount, int maxGenerations) {
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.tournamentSize = tournamentSize;
        this.eliteCount = eliteCount;
        this.maxGenerations = maxGenerations;
    }

    public int getPopulationSize() { return populationSize; }
    public void setPopulationSize(int populationSize) { this.populationSize = populationSize; }

    public double getMutationRate() { return mutationRate; }
    public void setMutationRate(double mutationRate) { this.mutationRate = mutationRate; }

    public double getCrossoverRate() { return crossoverRate; }
    public void setCrossoverRate(double crossoverRate) { this.crossoverRate = crossoverRate; }

    public int getTournamentSize() { return tournamentSize; }
    public void setTournamentSize(int tournamentSize) { this.tournamentSize = tournamentSize; }

    public int getEliteCount() { return eliteCount; }
    public void setEliteCount(int eliteCount) { this.eliteCount = eliteCount; }

    public int getMaxGenerations() { return maxGenerations; }
    public void setMaxGenerations(int maxGenerations) { this.maxGenerations = maxGenerations; }

    public boolean isUseLocalSearch() { return useLocalSearch; }
    public void setUseLocalSearch(boolean useLocalSearch) { this.useLocalSearch = useLocalSearch; }
}
