package algorthims;

import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by 40056761 on 20/05/2016.
 */
public class AlgorithmFitness extends HyperHeuristic {

    private int numberOfHeuristics;
    private ArrayList<int[]> algorithms;
    private String filePath;

    public AlgorithmFitness(long seed, String filePath) {
        super(seed);
        this.filePath = filePath;
    }

    private void generateAlgorithms() {
        this.algorithms = new ArrayList<>();

        for (int i = 0; i < numberOfHeuristics; i++) {
            for (int j = 0; j < numberOfHeuristics; j++) {
                for (int k = 0; k < numberOfHeuristics; k++) {

                    int[] algorithm = new int[3];
                    algorithm[0] = i;
                    algorithm[1] = j;
                    algorithm[2] = k;
                    this.algorithms.add(algorithm);
                }
            }
        }

    }

    private void writeToFile(String data) throws IOException {
        System.out.println("writing to file");
        FileWriter fw = new FileWriter(this.filePath, true);
        fw.append(data);
        fw.flush();
        fw.close();
    }

    @Override
    protected void solve(ProblemDomain problemDomain) {

        StringBuilder sb = new StringBuilder();

        int noImprovementCounter = 0;
        int algorithmIndex = 0;
        int iterations = 0;
        int totalIterations = 0;
        double delta;
        double newFitness;

        double currentFitness = Double.POSITIVE_INFINITY;
        this.numberOfHeuristics = problemDomain.getNumberOfHeuristics() - 1;

        generateAlgorithms();

        int[] currentAlgorithm = algorithms.get(algorithmIndex);

        problemDomain.setMemorySize(3);
        problemDomain.initialiseSolution(0);
        problemDomain.copySolution(0, 1);

        double startingFitness = problemDomain.getBestSolutionValue();

        while (!hasTimeExpired()) {

            for (int i = 0; i < currentAlgorithm.length; i++) {
                System.out.println(totalIterations);
                System.out.println("Alg: " + algorithmIndex);
                System.out.println("It: " + iterations);
                System.out.println("Heuristic: " + i);
                System.out.println("NoImprovements: " + noImprovementCounter + "\n");

                iterations++;
                totalIterations++;

                newFitness = problemDomain.applyHeuristic(currentAlgorithm[i], 1, 2);
                System.out.println("cur: " + currentFitness);
                System.out.println("new: " + newFitness);
                delta = currentFitness - newFitness;

                if (delta > 0) {
                    problemDomain.copySolution(2, 1);
                    currentFitness = newFitness;
                    noImprovementCounter = 0;

                } else {
                    noImprovementCounter++;
                }
            }

            if (noImprovementCounter < 3) {
                noImprovementCounter = 0;
            } else {
                algorithmIndex++;
                if (algorithmIndex <= algorithms.size() - 1) {
                    sb.append(startingFitness + "," + algorithmIndex + "," + currentFitness + "," + iterations + "\n");

                    currentAlgorithm = algorithms.get(algorithmIndex);
                    iterations = 0;
                    noImprovementCounter = 0;
                    problemDomain.copySolution(0, 1);
                    currentFitness = Double.POSITIVE_INFINITY;
                } else {

                    try {
                        this.writeToFile("starting fitness,algorithm number,fitness,number of iterations\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        this.writeToFile(sb.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println("we done");
                    return;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AlgorithmFitness";
    }
}
