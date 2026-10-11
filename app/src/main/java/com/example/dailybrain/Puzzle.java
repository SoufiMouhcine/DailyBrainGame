package com.example.dailybrain;

public class Puzzle {

    private final int[][] checkpoints;

    private final boolean[][] horizontalWalls;
    private final boolean[][] verticalWalls;

    private final int[][] solutionPath;

    public Puzzle(
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls,
            int[][] solutionPath) {

        this.checkpoints = checkpoints;
        this.horizontalWalls = horizontalWalls;
        this.verticalWalls = verticalWalls;
        this.solutionPath = solutionPath;
    }

    public int[][] getCheckpoints() {
        return checkpoints;
    }

    public boolean[][] getHorizontalWalls() {
        return horizontalWalls;
    }

    public boolean[][] getVerticalWalls() {
        return verticalWalls;
    }

    public int[][] getSolutionPath() {
        return solutionPath;
    }
}