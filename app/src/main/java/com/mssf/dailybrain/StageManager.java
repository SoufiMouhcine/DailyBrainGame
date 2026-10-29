
package com.mssf.dailybrain;

public class StageManager {

    public static Puzzle getStage(int stageNumber) {

        // Keep stage numbers valid.
        stageNumber = Math.max(1, stageNumber);

        int difficulty = getDifficulty(stageNumber);
        int numberOfCheckpoints =
                getNumberOfCheckpoints(stageNumber);
        PuzzleGenerator generator = new PuzzleGenerator();
        return generator.generate(
                stageNumber,
                numberOfCheckpoints,
                difficulty
        );
    }


    private static int getDifficulty(int stageNumber) {

        if (stageNumber <= 5) {
            return 1; // Easy
        } else if (stageNumber <= 10) {
            return 2; // Normal
        } else if (stageNumber <= 20) {
            return 3; // Hard
        } else if (stageNumber <= 35) {
            return 4; // Expert
        } else {
            return 5; // Master
        }
    }



    private static int getNumberOfCheckpoints(int stageNumber) {

        if (stageNumber <= 2) {
            return 3;
        } else if (stageNumber <= 5) {
            return 4;
        } else if (stageNumber <= 10) {
            return 5;
        } else if (stageNumber <= 15) {
            return 6;
        } else if (stageNumber <= 20) {
            return 7;
        } else if (stageNumber <= 35) {
            return 8;
        } else {
            return 9;
        }
    }

}
