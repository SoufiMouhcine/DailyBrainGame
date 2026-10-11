package com.example.dailybrain;

public class StageManager {

    public static Puzzle getStage(int stageNumber) {

        int numberOfCheckpoints;

        if (stageNumber <= 3) {
            numberOfCheckpoints = 3;
        } else if (stageNumber <= 6) {
            numberOfCheckpoints = 5;
        } else if (stageNumber <= 10) {
            numberOfCheckpoints = 7;
        } else {
            numberOfCheckpoints = 7;
        }

        return PuzzleGenerator.generate(
                stageNumber,
                numberOfCheckpoints,
                getDifficulty(stageNumber)
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
}