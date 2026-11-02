
package com.mssf.dailybrain;

public class Puzzle {

    private final int size;

    private final int[][] checkpoints;
    private final boolean[][] horizontalWalls;
    private final boolean[][] verticalWalls;
    private final int[][] solutionPath;

    // Existing constructor: keeps normal 5x5 stages compatible
    public Puzzle(
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls,
            int[][] solutionPath) {

        this(5, checkpoints, horizontalWalls, verticalWalls, solutionPath);
    }

    // New constructor: allows 6x6 Daily Challenge
    public Puzzle(
            int size,
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls,
            int[][] solutionPath) {

        this.size = size;
        this.checkpoints = checkpoints;
        this.horizontalWalls = horizontalWalls;
        this.verticalWalls = verticalWalls;
        this.solutionPath = solutionPath;
    }

    public int getSize() {
        return size;
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
