package com.mssf.dailybrain;

public class PuzzleManager {

    public static Puzzle getPuzzle(int puzzleNumber) {

        int numberOfCheckpoints;
        int difficulty;

        if (puzzleNumber <= 3) {
            numberOfCheckpoints = 3;
            difficulty = 1;
        } else if (puzzleNumber <= 6) {
            numberOfCheckpoints = 5;
            difficulty = 2;
        } else if (puzzleNumber <= 10) {
            numberOfCheckpoints = 7;
            difficulty = 3;
        } else if (puzzleNumber <= 20) {
            numberOfCheckpoints = 8;
            difficulty = 4;
        } else {
            numberOfCheckpoints = 9;
            difficulty = 5;
        }

        int seed = puzzleNumber * 1000 + difficulty * 37;

        PuzzleGenerator generator = new PuzzleGenerator();

        return generator.generate(
                seed,
                numberOfCheckpoints,
                difficulty
        );
    }
}