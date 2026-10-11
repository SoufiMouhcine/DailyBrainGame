package com.example.dailybrain;

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
        } else {
            numberOfCheckpoints = 7;
            difficulty = 3;
        }

        return PuzzleGenerator.generate(
                puzzleNumber * 1000 + difficulty * 37,
                numberOfCheckpoints,
                difficulty
        );
    }
}