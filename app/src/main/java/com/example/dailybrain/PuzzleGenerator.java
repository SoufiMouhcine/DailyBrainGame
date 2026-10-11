package com.example.dailybrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleGenerator {

    private static final int SIZE = 5;
    private static final int TOTAL_CELLS = 25;
    private static int difficulty;
    private static final int MAX_PATH_ATTEMPTS = 5000;
    private static int searchAttempts;
    private int[][] solutionPath;
    public static Puzzle generate(
            int seed,
            int numberOfCheckpoints,
            int difficulty) {

        Random random = new Random(seed);

        PuzzleGenerator.difficulty = difficulty;

        List<int[]> path =
                generateDifficultyPath(
                        random,
                        difficulty
                );

        int[][] checkpoints =
                generateCheckpoints(
                        path,
                        numberOfCheckpoints,
                        difficulty
                );

        boolean[][] horizontalWalls =
                generateHorizontalWalls(
                        path,
                        difficulty,
                        random
                );

        boolean[][] verticalWalls =
                generateVerticalWalls(
                        path,
                        difficulty,
                        random
                );

        int[][] solutionPath =
                convertPathToArray(path);

        return new Puzzle(
                checkpoints,
                horizontalWalls,
                verticalWalls,
                solutionPath
        );
    }

    private static int[][] convertPathToArray(
            List<int[]> path) {

        int[][] result =
                new int[path.size()][2];

        for (int i = 0; i < path.size(); i++) {

            result[i][0] =
                    path.get(i)[0];

            result[i][1] =
                    path.get(i)[1];
        }

        return result;
    }
    private static boolean[][] generateHorizontalWalls(
            List<int[]> path,
            int difficulty,
            Random random) {

        boolean[][] walls =
                new boolean[SIZE - 1][SIZE];

        List<int[]> availableWalls =
                new ArrayList<>();

        // Find all horizontal borders
        // that are NOT used by the solution path
        for (int row = 0; row < SIZE - 1; row++) {

            for (int col = 0; col < SIZE; col++) {

                if (!isPathConnection(
                        path,
                        row,
                        col,
                        row + 1,
                        col)) {

                    availableWalls.add(
                            new int[]{row, col}
                    );
                }
            }
        }

        Collections.shuffle(
                availableWalls,
                random
        );

        int numberOfWalls;

        if (difficulty == 1) {
            numberOfWalls = 3;
        } else if (difficulty == 2) {
            numberOfWalls = 5;
        } else {
            numberOfWalls = 7;
        }

        int wallsToPlace =
                Math.min(
                        numberOfWalls,
                        availableWalls.size()
                );

        for (int i = 0; i < wallsToPlace; i++) {

            int[] wall =
                    availableWalls.get(i);

            walls[wall[0]][wall[1]] = true;
        }

        return walls;
    }
    private static boolean[][] generateVerticalWalls(
            List<int[]> path,
            int difficulty,
            Random random) {

        boolean[][] walls =
                new boolean[SIZE][SIZE - 1];

        List<int[]> availableWalls =
                new ArrayList<>();

        // Find all vertical borders
        // that are NOT used by the solution path
        for (int row = 0; row < SIZE; row++) {

            for (int col = 0; col < SIZE - 1; col++) {

                if (!isPathConnection(
                        path,
                        row,
                        col,
                        row,
                        col + 1)) {

                    availableWalls.add(
                            new int[]{row, col}
                    );
                }
            }
        }

        Collections.shuffle(
                availableWalls,
                random
        );

        int numberOfWalls;

        if (difficulty == 1) {
            numberOfWalls = 2;
        } else if (difficulty == 2) {
            numberOfWalls = 4;
        } else {
            numberOfWalls = 6;
        }

        int wallsToPlace =
                Math.min(
                        numberOfWalls,
                        availableWalls.size()
                );

        for (int i = 0; i < wallsToPlace; i++) {

            int[] wall =
                    availableWalls.get(i);

            walls[wall[0]][wall[1]] = true;
        }

        return walls;
    }
    private static boolean isPathConnection(
            List<int[]> path,
            int row1,
            int col1,
            int row2,
            int col2) {

        for (int i = 0; i < path.size() - 1; i++) {

            int[] current = path.get(i);
            int[] next = path.get(i + 1);

            boolean sameDirection =
                    current[0] == row1 &&
                            current[1] == col1 &&
                            next[0] == row2 &&
                            next[1] == col2;

            boolean oppositeDirection =
                    current[0] == row2 &&
                            current[1] == col2 &&
                            next[0] == row1 &&
                            next[1] == col1;

            if (sameDirection || oppositeDirection) {
                return true;
            }
        }

        return false;
    }

    private static List<int[]> generateRandomPath(
            Random random,
            int difficulty) {
        searchAttempts = 0;
        List<int[]> path = new ArrayList<>();

        boolean[][] visited = new boolean[SIZE][SIZE];

        // Start from a random cell
        int startRow = random.nextInt(SIZE);
        int startCol = random.nextInt(SIZE);

        boolean success = findPath(
                startRow,
                startCol,
                visited,
                path,
                random,
                difficulty
        );

        if (!success) {
            // Fallback to a guaranteed snake path
            return generateSnakePath();
        }

        return path;
    }

    private static boolean findPath(
            int row,
            int col,
            boolean[][] visited,
            List<int[]> path,
            Random random,
            int difficulty) {
        searchAttempts++;

        if (searchAttempts > MAX_PATH_ATTEMPTS) {
            return false;
        }
        if (row < 0 || row >= SIZE ||
                col < 0 || col >= SIZE ||
                visited[row][col]) {

            return false;
        }

        visited[row][col] = true;

        path.add(new int[]{row, col});
        boolean preferStraight = difficulty == 1 && path.size() > 1;

        // We visited all 25 cells
        if (path.size() == TOTAL_CELLS) {
            return true;
        }

        List<int[]> directions = new ArrayList<>();

        directions.add(new int[]{-1, 0}); // up
        directions.add(new int[]{1, 0});  // down
        directions.add(new int[]{0, -1}); // left
        directions.add(new int[]{0, 1});  // right

        if (difficulty == 1) {

            // Easy: random but relatively predictable
            Collections.shuffle(directions, random);

        } else if (difficulty == 2) {

            // Medium: more random decisions
            Collections.shuffle(directions, random);
            Collections.shuffle(directions, random);

        } else {

            // Hard: highly unpredictable
            Collections.shuffle(directions, random);
            Collections.shuffle(directions, random);
            Collections.shuffle(directions, random);
        }

        for (int[] direction : directions) {

            int newRow = row + direction[0];
            int newCol = col + direction[1];

            if (findPath(
                    newRow,
                    newCol,
                    visited,
                    path,
                    random,
                    difficulty)) {

                return true;
            }
        }

        // Backtrack
        visited[row][col] = false;
        path.remove(path.size() - 1);

        return false;
    }

    private static List<int[]> generateSnakePath() {

        List<int[]> path = new ArrayList<>();

        for (int row = 0; row < SIZE; row++) {

            if (row % 2 == 0) {

                for (int col = 0; col < SIZE; col++) {
                    path.add(new int[]{row, col});
                }

            } else {

                for (int col = SIZE - 1; col >= 0; col--) {
                    path.add(new int[]{row, col});
                }
            }
        }

        return path;
    }
    private static int countTurns(List<int[]> path) {

        int turns = 0;

        for (int i = 2; i < path.size(); i++) {

            int[] previous = path.get(i - 2);
            int[] current = path.get(i - 1);
            int[] next = path.get(i);

            int direction1Row = current[0] - previous[0];
            int direction1Col = current[1] - previous[1];

            int direction2Row = next[0] - current[0];
            int direction2Col = next[1] - current[1];

            if (direction1Row != direction2Row ||
                    direction1Col != direction2Col) {

                turns++;
            }
        }

        return turns;
    }
    private static List<int[]> generateDifficultyPath(
            Random random,
            int difficulty) {

        List<int[]> bestPath = null;
        int bestScore = Integer.MAX_VALUE;

        int targetTurns;

        if (difficulty == 1) {
            targetTurns = 6 + (Math.abs(random.nextInt()) % 4);
        } else if (difficulty == 2) {
            targetTurns = 11 + (Math.abs(random.nextInt()) % 5);
        } else {
            targetTurns = 17 + (Math.abs(random.nextInt()) % 5);
        }

        for (int attempt = 0; attempt < 5; attempt++) {

            List<int[]> candidate =
                    generateRandomPath(random, difficulty);

            int turns = countTurns(candidate);

            int difference =
                    Math.abs(turns - targetTurns);

            if (difference < bestScore) {

                bestScore = difference;
                bestPath = candidate;
            }

            if (turns == targetTurns) {
                break;
            }
        }

        return bestPath;
    }
    private static int[][] generateCheckpoints(
            List<int[]> path,
            int numberOfCheckpoints,
            int difficulty) {

        int[][] checkpoints =
                new int[numberOfCheckpoints][2];

        int previousIndex = -1;

        for (int i = 0; i < numberOfCheckpoints; i++) {

            int minIndex =
                    i * (TOTAL_CELLS - 1)
                            / (numberOfCheckpoints - 1);

            int maxIndex =
                    (i + 1) * (TOTAL_CELLS - 1)
                            / (numberOfCheckpoints - 1);

            int index;

            if (i == 0) {
                index = 0;
            } else if (i == numberOfCheckpoints - 1) {
                index = TOTAL_CELLS - 1;
            } else {

                int range = Math.max(1, maxIndex - minIndex);

                if (difficulty == 1) {
                    index = minIndex;
                } else {
                    index = minIndex + range / 2;
                }
            }

            if (index <= previousIndex) {
                index = previousIndex + 1;
            }

            checkpoints[i][0] = path.get(index)[0];
            checkpoints[i][1] = path.get(index)[1];

            previousIndex = index;
        }

        return checkpoints;
    }
}