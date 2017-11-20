package solver;

import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import BinPacking.BinPacking;
import FlowShop.FlowShop;
import PersonnelScheduling.PersonnelScheduling;
import SAT.SAT;
import factories.AlgorithmFactory;
import factories.EnsembleFactory;
import heuristics.Algorithm;
import heuristics.Ensemble;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

class RunDiverseHeuristics {

    private static int iterations;
    private static int ensembleNumber;
    private static int problemInstance;
    private static String type;
    private static String flag;
    private static FileWriter fw;
    private static Ensemble ensemble;
    private static ProblemDomain problem;
    private static String problemType;
    private static int iteratorStart;
    private static int iteratorEnd;


    /* writes data to output file */
    static synchronized void writeData(String string) throws IOException {
        fw.write(string);
        fw.flush();
    }

    public static void main(String args[]) throws IOException {
        type = "";
        flag = "";
        problemType = "";
        ensemble = null;
        iterations = 50;
        ensembleNumber = 0;
        problemInstance = 0;
        iteratorStart = -1;
        iteratorEnd = -1;
        String headerToken = "heuristics";

        parseArguments(args);
        AlgorithmFactory.setProblemHeuristics(problem);

        /* if not testing algorithms */
        if (!type.equals("-a")) {
            headerToken = "algorithms";

            /* generate appropriate ensemble */
            for (int i = 0; i < ensembleNumber + 1; i++) {
                switch (flag) {
                    case "--elite":
                        ensemble = EnsembleFactory.generateEliteEnsemble(problemType);
                        break;
                    case "--random":
                        ensemble = EnsembleFactory.generateRandomEnsemble();
                        break;
                    default:
                        ensemble = EnsembleFactory.generateEnsemble();
                        break;
                }
            }
        }

        createDataLocation();

        /* inform user that data collection is starting */
        String fullName = type.equals("-e") ? "ensemble" : "algorithm";
        System.out.printf("Starting %s data collection.\r\n", fullName);
        System.out.println(problem.toString());

        /* write data headers to output file */
        String header = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                "iteration", "problem instance", "problem seed", "algorithm seed",
                "starting fitness", "ensemble number", "fitness", "number of runs", headerToken);

        writeData(header);

        /* begin testing */
        if (type.equals("-a")) {
            collectAlgorithmData();
        } else {
            collectEnsembleData(ensemble);
        }

        /* close file writer */
        fw.close();
    }

    /* data collection for ensembles */
    private static void collectEnsembleData(Ensemble ensemble) {
        int problemSeed;
        int algorithmSeed;
        long timeLimit;
        HyperHeuristic hh;
        problemInstance = 0;

        /* for every problem instance */
        for (int i = 0; i < problem.getNumberOfInstances(); i++) {
            problemSeed = 1000;
            algorithmSeed = 1000;
            timeLimit = 1000 * 60 * 15;

            /* for number of iterations (50) */
            for (int j = 0; j < iterations; j++) {
                /* initialize new problem and solver and run */
                switch (problemType) {
                    case "--bin":
                        problem = new BinPacking(problemSeed);
                        break;
                    case "--sat":
                        problem = new SAT(problemSeed);
                        break;
                    case "--flo":
                        problem = new FlowShop(problemSeed);
                        break;
                    case "--per":
                        problem = new PersonnelScheduling(problemSeed);
                        break;
                }
                hh = new ExecuteHyperHeuristic(ensemble, algorithmSeed, problemSeed, problemInstance, j, type, flag);

                problem.loadInstance(problemInstance);

                hh.setTimeLimit(timeLimit);
                hh.loadProblemDomain(problem);
                hh.run();

                /* increment seeds */
                problemSeed++;
                algorithmSeed++;

                if (Objects.equals(flag, "--random")) {
                    ensemble = EnsembleFactory.generateRandomEnsemble(1);
                }
            }
            problemInstance++;
        }
    }

    /* algorithm data collection */
    private static void collectAlgorithmData() {
        ArrayList<Algorithm> algorithms = AlgorithmFactory.getAlgorithms();
        int iteratorBegin = (iteratorStart > -1) ? iteratorStart : 0;
        int iteratorFinish = (iteratorEnd > -1) ? iteratorEnd : algorithms.size();
        /* generate and test ensembles of single algorithms */
        for (int i = iteratorBegin; i < iteratorFinish; i++) {
            ensemble = new Ensemble(i);
            ensemble.appendAlgorithm(algorithms.get(i));

            collectEnsembleData(ensemble);
        }
    }

    /* creates data directory for output files */
    private static void createDataLocation() throws IOException {
        boolean result = false;
        String testType = problemType;
        String ensDirName = "Data/Ensembles";
        String algDirName = "Data/Algorithms";
        String eliDirName = "Data/EliteEnsembles";
        String ranDirName = "Data/RandomEnsembles";
        File dataDir = null;

        switch (type) {
            case "-e":
                switch (flag) {
                    case "--elite":
                        testType += "EliteEnsemble";
                        dataDir = new File(eliDirName);
                        break;
                    case "--random":
                        testType += "RandomEnsemble";
                        dataDir = new File(ranDirName);
                        break;
                    default:
                        testType += "Ensemble";
                        dataDir = new File(ensDirName);
                        break;
                }
                break;
            case "-a":
                testType += "Algorithm";
                dataDir = new File(algDirName);
                break;
        }

        assert dataDir != null;
        if (!dataDir.exists()) {
            result = dataDir.mkdirs();
        }
        if (result) {
            System.out.printf("%s data directory created.\n", dataDir.getName());
        }
        int fileNum = (!type.equals("-a") ? ensemble.getID() : iterations);
        String FILE_PATH = String.format("%s/%s%dData%d.csv",
                dataDir.getPath(), testType, fileNum, System.nanoTime());
        fw = new FileWriter(FILE_PATH, true);
    }

    /* determine test from commandline arguments */
    private static void parseArguments(String[] args) throws IOException {
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-h":
                    case "--help":
                        printUsage(false);
                        System.exit(0);
                    case "-a":
                        type = arg;
                        if (args.length < 2) {
                            printUsage(true);
                            System.exit(1);
                        } else {
                            try {
                                iterations = Integer.parseInt(args[i + 1]);
                            } catch (NumberFormatException e) {
                                printUsage(false);
                                System.exit(1);
                            }
                            if (iterations < 1) {
                                System.out.println("<iterations> must be greater than one.");
                                printUsage(false);
                                System.exit(1);
                            }
                        }
                        break;
                    case "-e":
                        type = arg;
                        if (args.length < 2) {
                            printUsage(true);
                            System.exit(1);
                        } else {
                            try {
                                ensembleNumber = Integer.parseInt(args[i + 1]);
                            } catch (NumberFormatException e) {
                                printUsage(false);
                                System.exit(1);
                            }
                            if (ensembleNumber < 0) {
                                System.out.println("ensembleNo must be positive.");
                                printUsage(false);
                                System.exit(1);
                            }
                        }
                        break;

                    case "--start":
                        try {
                            iteratorStart = Integer.parseInt(args[i + 1]);
                        } catch (NumberFormatException e) {
                            printUsage(false);
                            System.exit(1);
                        }
                        if (iteratorStart < 0) {
                            System.out.println("ensembleNo must be positive.");
                            printUsage(false);
                            System.exit(1);
                        }
                        break;

                    case "--end":
                        try {
                            iteratorEnd = Integer.parseInt(args[i + 1]);
                        } catch (NumberFormatException e) {
                            printUsage(false);
                            System.exit(1);
                        }
                        if (iteratorEnd < 0) {
                            System.out.println("ensembleNo must be positive.");
                            printUsage(false);
                            System.exit(1);
                        }
                        break;
                    case "--elite":
                        flag = arg;
                        break;
                    case "--random":
                        flag = arg;
                        break;
                    case "--bin":
                        problemType = arg;
                        problem = new BinPacking(0);
                        break;
                    case "--sat":
                        problemType = arg;
                        problem = new SAT(0);
                        break;
                    case "--flo":
                        problemType = arg;
                        problem = new FlowShop(0);
                        break;
                    case "--per":
                        problemType = arg;
                        problem = new PersonnelScheduling(0);
                        break;
                }
            }
        } else {
            printUsage(true);
            System.exit(1);
        }
        if (type.equals("")) {
            printUsage(true);
            System.exit(1);
        }
        if (problemType.equals("")) {
            printUsage(true);
            System.exit(1);
        }
    }

    /* outputs program usage to command line */
    private static void printUsage(boolean number) {
        if (number) {
            System.out.println("Unexpected number of arguments.");
        }
        System.out.println("Usage:\r\n" +
                "\tDiverseHeuristics <--bin|--sat|--flo|--per> -e <ensembleID>[--elite|--random]\r\n" +
                "\tDiverseHeuristics <--bin|--sat|--flo|--per> -a <iterations>\r\n" +
                "\t--bin:           Bin Packing problem\r\n" +
                "\t--sat:           SAT Boolean problem\r\n" +
                "\t--flo:           Flow Shop problem\r\n" +
                "\t--per:           Personal Scheduling problem\r\n" +
                "\t-e:              Test a single ensemble on all problems\r\n" +
                "\t<ensembleID>:    The ID of the ensemble to be tested.\r\n" +
                "\t--elite:         Test elite version of the ensemble.\r\n" +
                "\t--random:        Test a randomly generated ensemble.\r\n\r\n" +
                "\t-a:              Test all algorithms on all problems.\r\n" +
                "\t<iterations>:    The number of times to test each algorithm.");

        // Bin SAT Flo Per
    }

}