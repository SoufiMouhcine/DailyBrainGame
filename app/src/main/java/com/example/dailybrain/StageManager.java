
package com.example.dailybrain;

public class StageManager {

    public static Puzzle getStage(int stageNumber) {

        // Keep stage numbers valid.
        stageNumber = Math.max(1, stageNumber);

        int difficulty = getDifficulty(stageNumber);
        int numberOfCheckpoints =
                getNumberOfCheckpoints(stageNumber);

        return PuzzleGenerator.generate(
                stageNumber,
                numberOfCheckpoints,
                difficulty
        );
    }

    private static int getDifficulty(int stageNumber) {

        if (stageNumber <= 3) {
            return 1;
        } else if (stageNumber <= 6) {
            return 2;
        } else {
            return 3;
        }
    }

    private static int getNumberOfCheckpoints(
            int stageNumber) {

        if (stageNumber <= 2) {
            return 3;
        } else if (stageNumber <= 4) {
            return 4;
        } else if (stageNumber <= 6) {
            return 5;
        } else if (stageNumber <= 8) {
            return 6;
        } else {
            return 7;
        }
    }
}
