package pdp.project.parallel.ga.ttg;


import pdp.project.parallel.ga.ttg.ttg.spark.rdd_datasets.PopulationRDD;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GeneticAlgorithm implements Serializable{

	private int populationSize;
	private double mutationRate;
	private double crossoverRate;
	private int elitismCount;
	protected int tournamentSize;

	public GeneticAlgorithm(int populationSize, double mutationRate, double crossoverRate, int elitismCount,
			int tournamentSize) {

		this.populationSize = populationSize;
		this.mutationRate = mutationRate;
		this.crossoverRate = crossoverRate;
		this.elitismCount = elitismCount;
		this.tournamentSize = tournamentSize;
	}

	/**
	 * Initialize population
	 * 
	 * @param
	 *            timetable length of the individuals chromosome
	 * @return population The initial population generated
	 */
	public Population initPopulation(Timetable timetable) {
		// Initialize population
		Population population = new Population(this.populationSize, timetable);
		return population;
	}

	/**
	 * Check if population has met termination condition
	 * 
	 * @param generationsCount
	 *            Number of generations passed
	 * @param maxGenerations
	 *            Number of generations to terminate after
	 * @return boolean True if termination condition met, otherwise, false
	 */
	public boolean isTerminationConditionMet(int generationsCount, int maxGenerations) {
		return (generationsCount > maxGenerations);
	}

	/**
	 * Check if population has met termination condition
	 *
	 * @param population
	 * @return boolean True if termination condition met, otherwise, false
	 */
	public boolean isTerminationConditionMet(Population population) {
		return population.getFittest(0).getFitness() == 1.0;
	}

	/**
	 * Calculate individual's fitness value
	 * 
	 * @param individual
	 * @param timetable
	 * @return fitness
	 */
	public double calcFitness(Individual individual, Timetable timetable) {


        // Create new timetable object to use -- cloned from an existing timetable
		Timetable threadTimetable = new Timetable(timetable);
		threadTimetable.createClasses(individual);

        // todo Define RDD here
        //ClassRDD.setParallelRDD(threadTimetable.getClassRooms());



        // Calculate fitness
		int clashes = threadTimetable.calcClashes();
		double fitness = 1 / (double) (clashes + 1);

		individual.setFitness(fitness);

		return fitness;
	}

    public double calcFitness2(Individual individual, Timetable timetable) {



        return 2.2;
    }

	/**
	 * Evaluate population
	 * 
	 * @param population
	 * @param timetable
	 */

	public Population evalPopulation(Population population, Timetable timetable) {
		double populationFitness = 0;


        List<Individual> processedPopulation = PopulationRDD.getPopulationRDD().map(
                iChromesome -> {
            Timetable tt = new Timetable(timetable);
            tt.createClasses(iChromesome);
            double fitness = 1 / (double) (tt.calcClashes() + 1);
            iChromesome.setFitness(fitness);
            return iChromesome;
        }).collect();


        population.setPopulation(new ArrayList(processedPopulation));
        // todo paralze
        for (Individual individual : population.getIndividuals()) {
            populationFitness += individual.getFitness();
        }
        population.setPopulationFitness(populationFitness);

        return population;

	}

	/**
	 * Selects parent for crossover using tournament selection
	 * 
	 * Tournament selection works by choosing N random individuals, and then
	 * choosing the best of those.
	 * 
	 * @param population
	 * @return The individual selected as a parent
	 */
	public Individual selectParent(Population population) {
		// Create tournament
		Population tournament = new Population(this.tournamentSize);

		// Add random individuals to the tournament
		  population.shuffle();
         for (int i = 0; i < this.tournamentSize; i++) {
			Individual tournamentIndividual = population.getIndividual(i);
			tournament.setIndividual(i, tournamentIndividual);
		}

		// Return the best
		return tournament.getFittest(0);
	}


	/**
     * Apply mutation to population
     * 
     * @param population
     * @param timetable
     * @return The mutated population
     */
	public Population mutatePopulation(Population population, Timetable timetable) {

		/*List<Individual> processedPopulation = PopulationRDD.getPopulationRDD().map(iChromesome -> {
			Timetable randomIndividual = new Timetable(timetable);
			randomIndividual.createClasses(iChromesome);
			double fitness = 1 / (double) (tt.calcClashes() + 1);
			iChromesome.setFitness(fitness);
			return iChromesome;
		}).collect(); */




		// Initialize new population
		Population newPopulation = new Population(this.populationSize);

		// Loop over current population by fitness
		for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {

            // sequential code
		    // Individual individual = population.getFittest(populationIndex);

            // Parallized code
            Individual individual = PopulationRDD.getSortedData().get(populationIndex);

			// Create random individual to swap genes with
			Individual randomIndividual = new Individual(timetable);

			// Loop over individual's genes
			for (int geneIndex = 0; geneIndex < individual.getChromosomeLength(); geneIndex++) {
				// Skip mutation if this is an elite individual
				if (populationIndex > this.elitismCount) {
					// Does this gene need mutation?
					if (this.mutationRate > Math.random()) {
						// Swap for new gene
						individual.setGene(geneIndex, randomIndividual.getGene(geneIndex));
					}
				}
			}

			// Add individual to population
			newPopulation.setIndividual(populationIndex, individual);
		}

		// Return mutated population
		return newPopulation;
	}
    public void crossoverPopulationParallel(int populationSize) {

    }
    /**
     * Apply crossover to population
     * 
     * @param population The population to apply crossover to
     * @return The new population
     */
	public Population crossoverPopulation(Population population) {
		// Create new population
		Population newPopulation = new Population(population.size());

		// Loop over current population by fitness
		for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {

		    // Sequential code
		    //Individual parent1 = population.getFittest(populationIndex);

            // Parallized code
            Individual parent1 = PopulationRDD.getSortedData().get(populationIndex);

			//System.out.print("SP"+parent1.getFitness()+"\nPP"+PopulationRDD.getSortedData().get(populationIndex).getFitness());
			// Apply crossover to this individual?
			if (this.crossoverRate > Math.random() && populationIndex >= this.elitismCount) {
				// Initialize offspring

				Individual offspring = new Individual(parent1.getChromosomeLength());
				
				// Find second parent
				Individual parent2 = selectParent(population);

				// Loop over genome
				for (int geneIndex = 0; geneIndex < parent1.getChromosomeLength(); geneIndex++) {
					// Use half of parent1's genes and half of parent2's genes
					if (0.5 > Math.random()) {
						offspring.setGene(geneIndex, parent1.getGene(geneIndex));
					} else {
						offspring.setGene(geneIndex, parent2.getGene(geneIndex));
					}
				}

				// Add offspring to new population
				newPopulation.setIndividual(populationIndex, offspring);
			} else {
				// Add individual to new population without applying crossover
				newPopulation.setIndividual(populationIndex, parent1);
			}
		}

		return newPopulation;
	}



}
