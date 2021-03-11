import java.util.Random;

public class SeededGreedGame {
    private final boolean verbose;
    private final Random random;

    private static long seedUniquifier = 8682522807148012L;

    SeededGreedGame(boolean verboseMode, long seed) {
        verbose = verboseMode;
        random = new Random(seed);
    }

    public static void ComputerGreedCompetition(GreedStrategy strat1, GreedStrategy strat2, int games, boolean verbose) {
        ComputerGreedCompetition(strat1, strat2, games, verbose, (seedUniquifier *= 1181783497276652981L) ^ System.nanoTime());
    }

    public static void ComputerGreedCompetition(GreedStrategy strat1, GreedStrategy strat2, int games, boolean verbose, long seed) {
        GreedPlayer player1 = new ComputerGreedStrategyPlayer(strat1);
        GreedPlayer player2 = new ComputerGreedStrategyPlayer(strat2);
        long startTime = System.currentTimeMillis();
        int avg1 = 0;
        int avg2 = 0;
        SeededGreedGame game1 = new SeededGreedGame(verbose, seed);
        SeededGreedGame game2 = new SeededGreedGame(verbose, seed);
        for (int i = 0; i < games; i++) {
            int[] scores = new int[2];
            game1.Turn(player1, scores, 0);
            game2.Turn(player2, scores, 1);
            avg1 += scores[0];
            avg2 += scores[1];
            System.out.println("Strategy 1 got " + scores[0] + "    \tStrategy 2 got " + scores[1]);
            System.out.print("Strategy 1 average: " + avg1 / (i + 1) + "    \tStrategy 2 average: " + avg2 / (i + 1) + "    \t[");
            int percent = (i + 1) * 100 / games;
            for (int j = 0; j < percent; j++) {
                System.out.print("#");
            }
            for (int j = percent; j < 100; j++) {
                System.out.print(".");
            }
            System.out.print("]    \t" + percent + "%\r");
        }
        System.out.println("Strategy 1 averaged " + (avg1 / games));
        System.out.println("Strategy 2 averaged " + (avg2 / games));
        long finishTime = System.currentTimeMillis();
        System.out.println("Competition finished in " + (finishTime - startTime) / 1000 + "s");
    }

    private void Turn(GreedPlayer player, int[] scores, int playerNum) {
        int remainingDice = 6;
        int bank = 0;
        boolean turnOver = false;
        boolean reRoll;
        do {
            int[] dice = new int[remainingDice];
            for (int index = 0; index < remainingDice; ++index) {
                dice[index] = random.nextInt(6) + 1;
            }
            reRoll = false;
            if (verbose && !player.human()) {
                System.out.print("Dice rolls: ");
                for (int die : dice) {
                    System.out.print(" " + die);
                }
                System.out.println();
            }
            ScoringCombination[] combos = ScoringCombination.allContainedIn(dice);
            GreedOption[] choices = combos;
            GreedOption choice = null;
            if (choices.length == 0) {
                bank = 0;
                player.noScoringCombinations(dice);
                if (verbose && !player.human()) {
                    System.out.print("Turn over because there are no combinations:");
                    for (int die : dice) {
                        System.out.print(" " + die);
                    }
                    System.out.println();
                }
                turnOver = true;
            } else {
                if (choices.length == 1) {
                    choice = choices[0];
                    player.oneScoringCombination(dice, (ScoringCombination) choice);
                }
                boolean hasBeenRemoved = false;
                do {
                    if (choice == null) {
                        if (hasBeenRemoved) {
                            choices = new GreedOption[combos.length + 2];
                            System.arraycopy(combos, 0, choices, 0, combos.length);
                            choices[choices.length - 2] = new GreedOption(GreedOption.ROLLAGAIN);
                            choices[choices.length - 1] = new GreedOption(GreedOption.ENDTURN);
                        }
                        int playerChoiceNum = player.choose(choices, dice, bank, scores, 1, 1, playerNum);
                        if (playerChoiceNum >= 0 && playerChoiceNum < choices.length) {
                            choice = choices[playerChoiceNum];
                        } else {
                            choice = new GreedOption(GreedOption.ENDTURN);
                        }
                    }
                    if (verbose && !player.human()) {
                        System.out.println("Choice: " + choice.toString());
                    }
                    if (choice.optionType() == GreedOption.SCORE) {
                        hasBeenRemoved = true;
                        dice = ((ScoringCombination) choice).remove(dice);
                        remainingDice = dice.length;
                        if (remainingDice == 0) {
                            remainingDice = 6;
                        }
                        bank += ((ScoringCombination) choice).getValue();
                        combos = ScoringCombination.allContainedIn(dice);
                        choices = new GreedOption[combos.length + 2];
                        System.arraycopy(combos, 0, choices, 0, combos.length);
                        choices[choices.length - 2] = new GreedOption(GreedOption.ROLLAGAIN);
                        choices[choices.length - 1] = new GreedOption(GreedOption.ENDTURN);
                        choice = null;
                    } else if (choice.optionType() == GreedOption.ENDTURN) {
                        turnOver = true;
                    } else if (choice.optionType() == GreedOption.ROLLAGAIN) {
                        reRoll = true;
                    }
                } while (!turnOver && !reRoll);
            }
        } while (!turnOver);
        scores[playerNum] += bank;
    }
}
