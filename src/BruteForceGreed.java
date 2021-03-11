import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

class BruteForceGreed extends GreedStrategy {
    public static final int maxDepth = 2; // Maximum recursion depth
    public static final int maxThreads = 11; // Maximum number of threads to spawn if singleThread == false
    public static final boolean singleThread = false; // If true, don't spawn any additional threads
    public static final boolean seededRandom = false; // If false, use the default seed generator
    public static final long seed = 3467091; // The seed to use if seededRandom == true
    public static final int numGames = 1000000; // The number of games to play
    public static final boolean verbose = false; // If true, print details about each game to the console
    public static final int cacheLength = 10000000; // The size to allocate to the caching arrays
    public static final String dataFile = "C:\\Users\\bobby\\IdeaProjects\\Greed\\greedcache"; // Path to the cache file
    public static final boolean readFile = true; // If false, regenerate the cache
    public static final boolean writeFile = true; // If false, delete the in-memory cache after the competition
    public static final int cachingDepth = 1; // The maximum depth where the cache will be checked

    // All possible scoring combinations a player can get
    // First element is the length of the combination, second element is the score
    public static int[][] allChoices = {{6, 5000, 6, 6, 6, 6, 6, 6}, {6, 5000, 5, 5, 5, 5, 5, 5}, {6, 5000, 4, 4, 4, 4,
            4, 4}, {6, 5000, 3, 3, 3, 3, 3, 3}, {6, 5000, 2, 2, 2, 2, 2, 2}, {6, 5000, 1, 1, 1, 1, 1, 1}, {6, 2000, 4,
            4, 5, 5, 6, 6}, {6, 2000, 3, 3, 4, 4, 5, 5}, {6, 2000, 2, 2, 3, 3, 4, 4}, {6, 2000, 1, 1, 2, 2, 3, 3}, {6,
            1000, 1, 2, 3, 4, 5, 6}, {4, 1000, 1, 1, 1, 1}, {3, 600, 6, 6, 6}, {3, 500, 5, 5, 5}, {3, 400, 4, 4, 4}, {3,
            300, 3, 3, 3}, {3, 200, 2, 2, 2}, {1, 100, 1}, {1, 50, 5}};
    public static AtomicInteger nextSavedIndex = new AtomicInteger();
    public static byte[] savedDiceLengths = new byte[cacheLength];
    public static byte[][] savedDice = new byte[cacheLength][];
    public static short[] savedBanks = new short[cacheLength];
    public static boolean[] savedRolled = new boolean[cacheLength];
    public static short[][] savedResults = new short[cacheLength][2];
    public static byte[] savedDepths = new byte[cacheLength];

    /**
     * Fills a matrix with all possible outcomes from a dice roll.
     * @param dice the number of dice to roll
     * @param result the matrix which holds the outcomes
     * @param start the row in the matrix to start filling at
     */
    public static void getAllCombinations(int dice, int[][] result, int start) {
        if (dice > 0) {
            int size = 1;
            for (int i = 0; i < dice - 1; i++) {
                size *= 6;
            }
            for (int i = 0; i < 6; i++) {
                getAllCombinations(dice - 1, result, start + i * size);
            }
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < size; j++) {
                    result[start + i * size + j][dice - 1] = i + 1;
                }
            }
        }
    }

    public static void quicksort(int[] array, int start, int end) {
        if (start < end) {
            int pivot = array[end];
            int i = start - 1;
            for (int j = start; j < end; j++) {
                if (array[j] <= pivot) {
                    i++;
                    int swapTemp = array[i];
                    array[i] = array[j];
                    array[j] = swapTemp;
                }
            }
            int swapTemp = array[i + 1];
            array[i + 1] = array[end];
            array[end] = swapTemp;
            quicksort(array, start, i);
            quicksort(array, i + 2, end);
        }
    }

    /**
     * Selects the option which results in the statistically highest score
     * @param dice an array containing the dice
     * @param numDice the number of dice in dice
     * @param bank the current bank
     * @param depth the recursion depth at this point
     * @param rollDepth the number of times the outcome of a re-roll has been analyzed (used for deciding whether to
     *                  spawn more threads)
     * @param rolled true if the last choice was to re-roll, false if the last choice was to score (the re-roll option
     *               will not be available)
     * @param result a 2-int array: result[0] will be set to the average score a player would get by picking the best
     *               option, result[1] will be set to the index of the best option
     */
    public static void choose(int[] dice, int numDice, int bank, int depth, int rollDepth, boolean rolled, int[] result) {
        if (depth > maxDepth) {
            // We have exceeded the maximum recursion depth: use 0 as the result
            result[0] = 0;
        } else {
            boolean found = false;
            if (depth <= cachingDepth) {
                // Check the cache to see if we already analyzed this combination
                for (int j = 0; j < nextSavedIndex.intValue(); j++) {
                    if (savedBanks[j] == bank && savedDiceLengths[j] == numDice && savedRolled[j] == rolled && savedDepths[j] == maxDepth - depth) {
                        boolean equal = true;
                        for (int k = 0; k < numDice; k++) {
                            if (savedDice[j][k] != dice[k]) {
                                equal = false;
                                break;
                            }
                        }
                        if (equal) {
                            result[0] = savedResults[j][0];
                            result[1] = savedResults[j][1];
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                // Find all the scoring combinations that apply to dice
                // This algorithm is faster but assumes that dice is sorted (which it always is)
                int numChoices = 0;
                boolean[] answers = new boolean[19];
                for (int i = 0; i < 19; i++) {
                    int first = 0;
                    for (int j = 0; j < numDice; j++) {
                        if (dice[j] == allChoices[i][first + 2]) {
                            first++;
                        } else if (first > 0) {
                            break;
                        }
                        if (first >= allChoices[i][0]) {
                            answers[i] = true;
                            numChoices++;
                            break;
                        }
                    }
                }
                int l = 0;
                int[][] choices = new int[numChoices][];
                for (int i = 0; i < 19; i++) {
                    if (answers[i]) {
                        choices[l] = allChoices[i];
                        l++;
                    }
                }

                // If we can't find any better options, end the turn
                result[0] = bank;
                result[1] = numChoices + 1;

                if (!rolled) {
                    // The option is available, so check if re-rolling is a better option
                    int length = numDice;
                    if (length == 0) {
                        length = 6;
                    }
                    int size = 1;
                    for (int i = 0; i < length; i++) {
                        size *= 6;
                    }
                    int[][] combos = new int[size][length];
                    getAllCombinations(length, combos, 0);

                    // Find only the unique combinations and record the probability that they will occur
                    int[][] analyzedCombos = new int[size][];
                    int[] analyzedComboScores = new int[size];
                    int nextIndex = 0;
                    for (int i = 0; i < size; i++) {
                        quicksort(combos[i], 0, length - 1);
                        int index = -1;
                        for (int j = 0; j < nextIndex; j++) {
                            boolean equal = true;
                            for (int k = 0; k < length; k++) {
                                if (analyzedCombos[j][k] != combos[i][k]) {
                                    equal = false;
                                    break;
                                }
                            }
                            if (equal) {
                                index = j;
                                break;
                            }
                        }
                        if (index > -1) {
                            analyzedComboScores[index]++;
                        } else {
                            analyzedCombos[nextIndex] = combos[i];
                            analyzedComboScores[nextIndex] = 1;
                            nextIndex++;
                        }
                    }

                    // Test every possible combination
                    int sum = 0;
                    if (!singleThread && rollDepth == 0) {
                        // If this is the first time analyzing a re-roll, spawn threads
                        int[][] score = new int[nextIndex][2];
                        Thread[] threads = new Thread[maxThreads];
                        AtomicInteger nextAnalyzedIndex = new AtomicInteger();
                        for (int i = 0; i < maxThreads; i++) {
                            int finalLength = length;
                            int finalNextIndex = nextIndex;
                            threads[i] = new Thread(() -> {
                                // Analyze a portion of the combinations sequentially on this thread
                                int j;
                                while ((j = nextAnalyzedIndex.getAndIncrement()) < finalNextIndex) {
                                    choose(analyzedCombos[j], finalLength, bank, depth + 1, rollDepth + 1, true,
                                            score[j]);
                                }
                            });
                            threads[i].start();
                        }
                        // Wait for all threads to finish
                        for (int i = 0; i < maxThreads; i++) {
                            try {
                                threads[i].join();
                            } catch (InterruptedException e) {
                                threads[i].interrupt();
                            }
                        }
                        for (int i = 0; i < nextIndex; i++) {
                            sum += score[i][0] * analyzedComboScores[i];
                        }
                    } else {
                        // Analyze all combinations sequentially
                        int[] score = new int[2];
                        for (int i = 0; i < nextIndex; i++) {
                            choose(analyzedCombos[i], length, bank, depth + 1, rollDepth + 1, true, score);
                            sum += score[0] * analyzedComboScores[i];
                        }
                    }

                    // Find the average of all the combinations
                    int avg = sum / size;

                    // If re-rolling is the best option, pick it
                    if (avg > result[0]) {
                        result[0] = avg;
                        result[1] = numChoices;
                    }
                }
                if (!rolled || numChoices > 0) {
                    // Analyze each possible scoring combination
                    for (int i = 0; i < numChoices; i++) {
                        // Find the remaining dice after selecting this combination
                        int length = numDice - choices[i][0];
                        int[] newCombo = new int[length];
                        int index = 0;
                        for (int j = 0; j < numDice; j++) {
                            if (index < choices[i][0] && dice[j] == choices[i][index + 2]) {
                                index++;
                            } else {
                                newCombo[j - index] = dice[j];
                            }
                        }

                        // Analyze this combination
                        int[] score = new int[2];
                        choose(newCombo, length, bank + choices[i][1], depth + 1, rollDepth, false, score);

                        // If this combination is better, use it
                        if (choices[i][1] + score[0] > result[0] || (rolled && numChoices == 1)) {
                            result[0] = choices[i][1] + score[0];
                            result[1] = i;
                        }
                    }
                } else {
                    // We don't have the option to re-roll and there are no scoring combinations, so we lose the bank
                    result[0] = 0;
                }
                if (depth <= cachingDepth) {
                    // Save this combination to the cache
                    int index = nextSavedIndex.getAndIncrement();
                    savedDice[index] = new byte[numDice];
                    for (int i = 0; i < numDice; i++) {
                        savedDice[index][i] = (byte) dice[i];
                    }
                    savedDiceLengths[index] = (byte) numDice;
                    savedBanks[index] = (short) bank;
                    savedRolled[index] = rolled;
                    savedDepths[index] = (byte) (maxDepth - depth);
                    savedResults[index][0] = (short) result[0];
                    savedResults[index][1] = (short) result[1];
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (readFile) {
            // Read the cache from disk
            FileInputStream fileIn = new FileInputStream(dataFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            nextSavedIndex.set(in.readInt());
            for (int i = 0; i < nextSavedIndex.intValue(); i++) {
                savedDiceLengths[i] = in.readByte();
                savedDice[i] = new byte[savedDiceLengths[i]];
                for (int j = 0; j < savedDiceLengths[i]; j++) {
                    savedDice[i][j] = in.readByte();
                }
                savedBanks[i] = in.readShort();
                savedRolled[i] = in.readBoolean();
                for (int j = 0; j < 2; j++) {
                    savedResults[i][j] = in.readShort();
                }
                savedDepths[i] = in.readByte();
            }
            in.close();
            fileIn.close();
        }

        GreedStrategy strat1 = new BruteForceGreed();
        GreedStrategy strat2 = new KwonSchirmerGeorgeFangGreedStrategy();
        if (seededRandom) {
            SeededGreedGame.ComputerGreedCompetition(strat1, strat2, numGames, verbose, seed);
        } else {
            SeededGreedGame.ComputerGreedCompetition(strat1, strat2, numGames, verbose);
        }

        if (writeFile) {
            // Save the cache to disk
            FileOutputStream fileOut = new FileOutputStream(dataFile);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeInt(nextSavedIndex.intValue());
            for (int i = 0; i < nextSavedIndex.intValue(); i++) {
                out.writeByte(savedDiceLengths[i]);
                for (int j = 0; j < savedDiceLengths[i]; j++) {
                    out.writeByte(savedDice[i][j]);
                }
                out.writeShort(savedBanks[i]);
                out.writeBoolean(savedRolled[i]);
                for (int j = 0; j < 2; j++) {
                    out.writeShort(savedResults[i][j]);
                }
                out.writeByte(savedDepths[i]);
            }
            out.close();
            fileOut.close();
        }
    }

    @Override
    public int choose(GreedOption[] options, int[] dice, int bank) {
        int length = dice.length;
        quicksort(dice, 0, length - 1);
        boolean rolled = options[options.length - 1].optionType() != GreedOption.ENDTURN;
        int[] result = new int[2];
        choose(dice, length, bank, 0, 0, rolled, result);
        return result[1];
    }
}
