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



    public Puzzle generate(int seed, int numberOfCheckpoints, int difficulty) {
        Random random = new Random(seed);
        PuzzleGenerator.difficulty = difficulty;

        int attempts;

        if (difficulty <= 1) {
            attempts = 10;
        } else if (difficulty == 2) {
            attempts = 20;
        } else if (difficulty == 3) {
            attempts = 35;
        } else if (difficulty == 4) {
            attempts = 50;
        } else {
            attempts = 60;
        }

        Puzzle bestValidPuzzle = null;
        Puzzle bestUniquePuzzle = null;

        int bestValidScore = -1;
        int bestUniqueScore = -1;

        for (int i = 0; i < attempts; i++) {

            // IMPORTANT: generate a NEW route for every candidate.
            List<int[]> path = generateDifficultyPath(random, difficulty);

            if (path == null || path.size() != TOTAL_CELLS) {
                continue;
            }

            int[][] candidateSolution = convertPathToArray(path);

            // Checkpoints must match this candidate's route.
            int[][] candidateCheckpoints = generateCheckpoints(
                    path,
                    numberOfCheckpoints,
                    difficulty,
                    random
            );

            boolean[][] horizontalWalls =
                    generateHorizontalWalls(path, difficulty, random);

            boolean[][] verticalWalls =
                    generateVerticalWalls(path, difficulty, random);

            if (!isValidSolution(
                    candidateSolution,
                    horizontalWalls,
                    verticalWalls)) {
                continue;
            }

            Puzzle candidate = new Puzzle(
                    candidateCheckpoints,
                    horizontalWalls,
                    verticalWalls,
                    candidateSolution
            );

            // Count complete solutions and measure search complexity.
            int solutionCount = PuzzleSolver.countSolutions(
                    candidate, 2, 20_000
            );

            boolean searchExhausted =
                    PuzzleSolver.wasSearchExhausted(candidate);

            int complexityScore = PuzzleSolver.getLastSearchNodeCount();

            // Keep the most challenging candidate that has a solution
            // and whose search completed without hitting the node limit.
            if (solutionCount > 0
                    && !searchExhausted
                    && complexityScore > bestValidScore) {

                bestValidScore = complexityScore;
                bestValidPuzzle = candidate;
            }

            // Prefer a puzzle with exactly ONE complete solution.
            if (solutionCount == 1
                    && !searchExhausted
                    && complexityScore > bestUniqueScore) {

                bestUniqueScore = complexityScore;
                bestUniquePuzzle = candidate;
            }
        }

        // Prefer a proven unique solution.
        if (bestUniquePuzzle != null) {
            return bestUniquePuzzle;
        }

        // Keep Easy/Normal playable if no unique candidate was found.
        if (difficulty <= 2 && bestValidPuzzle != null) {
            return bestValidPuzzle;
        }

        throw new IllegalStateException(
                "Could not generate a valid unique puzzle. Try another seed."
        );
    }




    public static Puzzle generateDailyPuzzle(String dateKey) {

        // The same date produces the same puzzle.
        int seed = dateKey.hashCode();

        // Daily Challenge uses a harder configuration.
        int numberOfCheckpoints = 8;
        int difficulty = 5;
        PuzzleGenerator generator = new PuzzleGenerator();
        return generator.generate(
                seed,
                numberOfCheckpoints,
                difficulty
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

        boolean[][] walls = new boolean[SIZE - 1][SIZE];
        List<int[]> availableWalls = new ArrayList<>();

        for (int row = 0; row < SIZE - 1; row++) {
            for (int col = 0; col < SIZE; col++) {

                if (!isPathConnection(
                        path, row, col, row + 1, col)) {

                    availableWalls.add(new int[]{row, col});
                }
            }
        }

        int numberOfWalls;

        if (difficulty <= 1) {
            numberOfWalls = 1;
        } else if (difficulty == 2) {
            numberOfWalls = 1;
        } else if (difficulty == 3) {
            numberOfWalls = 2;
        } else if (difficulty == 4) {
            numberOfWalls = 3;
        } else {
            numberOfWalls = 4;
        }

        // Prefer walls near turns in the intended path.
        Collections.shuffle(availableWalls, random);

        List<int[]> selectedWalls = new ArrayList<>();

        for (int[] wall : availableWalls) {
            int wallRow = wall[0];
            int wallCol = wall[1];

            int score = getWallStrategicScore(
                    path, wallRow, wallCol, true);

            // Keep some randomness so layouts don't feel identical.
            score += random.nextInt(3);

            wall[0] = wallRow;
            wall[1] = wallCol;

            // Store the score separately in a three-value entry.
            selectedWalls.add(new int[]{
                    wallRow, wallCol, score
            });
        }

        Collections.sort(selectedWalls, (a, b) ->
                Integer.compare(b[2], a[2]));

        int wallsToPlace = Math.min(
                numberOfWalls, selectedWalls.size());

        for (int i = 0; i < wallsToPlace; i++) {
            int[] wall = selectedWalls.get(i);
            walls[wall[0]][wall[1]] = true;
        }

        return walls;
    }


    private static boolean[][] generateVerticalWalls(
            List<int[]> path,
            int difficulty,
            Random random) {

        boolean[][] walls = new boolean[SIZE][SIZE - 1];
        List<int[]> availableWalls = new ArrayList<>();

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE - 1; col++) {

                if (!isPathConnection(
                        path, row, col, row, col + 1)) {

                    availableWalls.add(new int[]{row, col});
                }
            }
        }

        int numberOfWalls;

        if (difficulty <= 1) {
            numberOfWalls = 0;
        } else if (difficulty == 2) {
            numberOfWalls = 1;
        } else if (difficulty == 3) {
            numberOfWalls = 1;
        } else if (difficulty == 4) {
            numberOfWalls = 2;
        } else {
            numberOfWalls = 3;
        }

        Collections.shuffle(availableWalls, random);

        List<int[]> selectedWalls = new ArrayList<>();

        for (int[] wall : availableWalls) {
            int wallRow = wall[0];
            int wallCol = wall[1];

            int score = getWallStrategicScore(
                    path, wallRow, wallCol, false);

            score += random.nextInt(3);

            selectedWalls.add(new int[]{
                    wallRow, wallCol, score
            });
        }

        Collections.sort(selectedWalls, (a, b) ->
                Integer.compare(b[2], a[2]));

        int wallsToPlace = Math.min(
                numberOfWalls, selectedWalls.size());

        for (int i = 0; i < wallsToPlace; i++) {
            int[] wall = selectedWalls.get(i);
            walls[wall[0]][wall[1]] = true;
        }

        return walls;
    }

    private static int getWallStrategicScore(
            List<int[]> path,
            int row,
            int col,
            boolean horizontalWall) {

        int score = 0;

        for (int i = 1; i < path.size() - 1; i++) {

            int[] previous = path.get(i - 1);
            int[] current = path.get(i);
            int[] next = path.get(i + 1);

            int previousRow = current[0] - previous[0];
            int previousCol = current[1] - previous[1];

            int nextRow = next[0] - current[0];
            int nextCol = next[1] - current[1];

            boolean isTurn =
                    previousRow != nextRow ||
                            previousCol != nextCol;

            if (!isTurn) {
                continue;
            }

            int distance = Math.abs(current[0] - row)
                    + Math.abs(current[1] - col);

            if (distance == 1) {
                score += 4;
            } else if (distance == 2) {
                score += 2;
            }
        }

        return score;
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
            // Retry with a different random starting point.
            // Do not return the same snake path for every failed attempt.
            return generateRandomPathFallback(random, difficulty);
        }

        return path;
    }

    private static List<int[]> generateRandomPathFallback(
            Random random,
            int difficulty) {

        for (int attempt = 0; attempt < 10; attempt++) {

            List<int[]> path = new ArrayList<>();
            boolean[][] visited = new boolean[SIZE][SIZE];

            int startRow = random.nextInt(SIZE);
            int startCol = random.nextInt(SIZE);

            searchAttempts = 0;

            if (findPath(
                    startRow,
                    startCol,
                    visited,
                    path,
                    random,
                    difficulty)) {

                return path;
            }
        }

        // Guaranteed fallback if randomized attempts fail.
        return generateSnakePath();
    }

    private static int calculatePathVarietyScore(List<int[]> path) {
        int turns = countTurns(path);
        int longestRun = getLongestStraightRun(path);

        int directionChanges = 0;

        for (int i = 2; i < path.size(); i++) {
            int[] a = path.get(i - 2);
            int[] b = path.get(i - 1);
            int[] c = path.get(i);

            int rowDirection1 = b[0] - a[0];
            int colDirection1 = b[1] - a[1];

            int rowDirection2 = c[0] - b[0];
            int colDirection2 = c[1] - b[1];

            if (rowDirection1 != rowDirection2
                    || colDirection1 != colDirection2) {
                directionChanges++;
            }
        }

        // Lower score favors a more winding path.
        // The random target in generateDifficultyPath()
        // will provide variety between stages.
        return Math.abs(turns - 12) * 10
                + longestRun * 3
                - directionChanges;
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



        if (difficulty == 1 && path.size() >= 2) {

            // EASY: prefer continuing straight.
            int[] previous = path.get(path.size() - 2);
            int[] current = path.get(path.size() - 1);

            int directionRow = current[0] - previous[0];
            int directionCol = current[1] - previous[1];

            int[] straight = new int[]{
                    directionRow,
                    directionCol
            };

            directions.removeIf(direction ->
                    direction[0] == directionRow
                            && direction[1] == directionCol
            );

            Collections.shuffle(directions, random);
            directions.add(0, straight);

        } else if (path.size() >= 2 && difficulty >= 3) {

            // HARD, EXPERT, MASTER:
            // Put turning directions before straight movement.
            int[] previous = path.get(path.size() - 2);
            int[] current = path.get(path.size() - 1);

            int directionRow = current[0] - previous[0];
            int directionCol = current[1] - previous[1];

            List<int[]> turningDirections = new ArrayList<>();
            List<int[]> straightDirections = new ArrayList<>();

            for (int[] direction : directions) {

                if (direction[0] == directionRow
                        && direction[1] == directionCol) {
                    straightDirections.add(direction);
                } else {
                    turningDirections.add(direction);
                }
            }

            Collections.shuffle(turningDirections, random);
            Collections.shuffle(straightDirections, random);

            directions.clear();

            // Higher difficulty prioritizes turns.
            directions.addAll(turningDirections);
            directions.addAll(straightDirections);

        } else {

            // NORMAL: randomize movement directions.
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

    private static int countAvailableMovesAlongPath(
            int[][] solutionPath,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        int totalAvailableMoves = 0;

        for (int i = 0; i < solutionPath.length; i++) {

            int row = solutionPath[i][0];
            int col = solutionPath[i][1];

            // Check the cell above.
            if (row > 0 && !horizontalWalls[row - 1][col]) {
                totalAvailableMoves++;
            }

            // Check the cell below.
            if (row < SIZE - 1 && !horizontalWalls[row][col]) {
                totalAvailableMoves++;
            }

            // Check the cell to the left.
            if (col > 0 && !verticalWalls[row][col - 1]) {
                totalAvailableMoves++;
            }

            // Check the cell to the right.
            if (col < SIZE - 1 && !verticalWalls[row][col]) {
                totalAvailableMoves++;
            }
        }

        return totalAvailableMoves;
    }

    private static int scoreWallLayout(
            int[][] solutionPath,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls,
            int difficulty) {

        int openConnections = 0;

        // Count open connections between vertically adjacent cells.
        for (int row = 0; row < SIZE - 1; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!horizontalWalls[row][col]) {
                    openConnections++;
                }
            }
        }

        // Count open connections between horizontally adjacent cells.
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE - 1; col++) {
                if (!verticalWalls[row][col]) {
                    openConnections++;
                }
            }
        }

        // Lower scores are preferred.
        // The grid has 40 possible connections.
        int targetOpenConnections;

        if (difficulty <= 1) {
            targetOpenConnections = 35;
        } else if (difficulty == 2) {
            targetOpenConnections = 32;
        } else if (difficulty == 3) {
            targetOpenConnections = 29;
        } else if (difficulty == 4) {
            targetOpenConnections = 26;
        } else {
            targetOpenConnections = 23;
        }

        return Math.abs(openConnections - targetOpenConnections);
    }

    private static List<int[]> generateDifficultyPath(
            Random random,
            int difficulty) {

        List<int[]> bestPath = null;
        int bestScore = Integer.MAX_VALUE;

        int targetTurns;

        if (difficulty <= 1) {
            targetTurns = 6 + random.nextInt(4);
        } else if (difficulty == 2) {
            targetTurns = 11 + random.nextInt(5);
        } else if (difficulty == 3) {
            targetTurns = 15 + random.nextInt(5);
        } else if (difficulty == 4) {
            targetTurns = 17 + random.nextInt(5);
        } else {
            targetTurns = 19 + random.nextInt(5);
        }

        int attempts;

        if (difficulty <= 1) {
            attempts = 5;
        } else if (difficulty == 2) {
            attempts = 10;
        } else if (difficulty == 3) {
            attempts = 20;
        } else {
            attempts = 30;
        }

        for (int attempt = 0; attempt < attempts; attempt++) {

            List<int[]> candidate =
                    generateRandomPath(random, difficulty);

            int turns = countTurns(candidate);

            int longestStraightRun =
                    getLongestStraightRun(candidate);

            int score =
                    Math.abs(turns - targetTurns) * 10
                            + longestStraightRun * 3
                            + random.nextInt(8);

            if (score < bestScore) {

                bestScore = score;
                bestPath = candidate;
            }
        }

        return bestPath;
    }

    private static String getPathSignature(List<int[]> path) {
        StringBuilder signature = new StringBuilder();

        for (int[] cell : path) {
            signature.append(cell[0])
                    .append(',')
                    .append(cell[1])
                    .append(';');
        }

        return signature.toString();
    }

    private static int getLongestStraightRun(
            List<int[]> path) {

        if (path.size() < 2) {
            return path.size();
        }

        int longestRun = 1;
        int currentRun = 1;

        int previousDirectionRow =
                path.get(1)[0] - path.get(0)[0];

        int previousDirectionCol =
                path.get(1)[1] - path.get(0)[1];

        for (int i = 2; i < path.size(); i++) {

            int directionRow =
                    path.get(i)[0] - path.get(i - 1)[0];

            int directionCol =
                    path.get(i)[1] - path.get(i - 1)[1];

            if (directionRow == previousDirectionRow
                    && directionCol == previousDirectionCol) {

                currentRun++;

            } else {

                currentRun = 1;
            }

            longestRun = Math.max(longestRun, currentRun);

            previousDirectionRow = directionRow;
            previousDirectionCol = directionCol;
        }

        return longestRun;
    }
    private static int[][] generateCheckpoints(
            List<int[]> path,
            int numberOfCheckpoints,
            int difficulty,
            Random random) {

        if (numberOfCheckpoints < 2 ||
                numberOfCheckpoints > path.size()) {
            throw new IllegalArgumentException(
                    "Checkpoint count must be between 2 and "
                            + path.size()
            );
        }

        int[][] checkpoints =
                new int[numberOfCheckpoints][2];

        // First checkpoint: beginning of the solution.
        checkpoints[0][0] = path.get(0)[0];
        checkpoints[0][1] = path.get(0)[1];

        // Last checkpoint: end of the solution.
        int lastCheckpoint = numberOfCheckpoints - 1;
        int lastPathIndex = path.size() - 1;

        checkpoints[lastCheckpoint][0] =
                path.get(lastPathIndex)[0];
        checkpoints[lastCheckpoint][1] =
                path.get(lastPathIndex)[1];

        int previousIndex = 0;

        int segmentSize =
                Math.max(1, lastPathIndex / lastCheckpoint);

        int variation;

        if (difficulty == 1) {
            variation = Math.max(1, segmentSize / 4);
        } else if (difficulty == 2) {
            variation = Math.max(1, segmentSize / 3);
        } else {
            variation = Math.max(1, segmentSize / 2);
        }

        // Place intermediate checkpoints at varied positions.
        for (int i = 1; i < lastCheckpoint; i++) {

            int idealIndex =
                    i * lastPathIndex / lastCheckpoint;

            int minIndex = Math.max(
                    previousIndex + 1,
                    idealIndex - variation
            );

            // Reserve enough cells for remaining checkpoints.
            int maxIndex = Math.min(
                    idealIndex + variation,
                    lastPathIndex
                            - (lastCheckpoint - i)
            );

            int index = minIndex;

            if (maxIndex > minIndex) {
                index = minIndex
                        + random.nextInt(
                        maxIndex - minIndex + 1
                );
            }

            checkpoints[i][0] = path.get(index)[0];
            checkpoints[i][1] = path.get(index)[1];

            previousIndex = index;
        }

        return checkpoints;
    }

    private static boolean isValidSolution(
            int[][] solutionPath,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        if (solutionPath == null ||
                solutionPath.length != TOTAL_CELLS) {
            return false;
        }

        boolean[][] visited =
                new boolean[SIZE][SIZE];

        for (int i = 0; i < solutionPath.length; i++) {

            int row = solutionPath[i][0];
            int col = solutionPath[i][1];

            // Check grid boundaries.
            if (row < 0 || row >= SIZE ||
                    col < 0 || col >= SIZE) {
                return false;
            }

            // Every cell must be visited only once.
            if (visited[row][col]) {
                return false;
            }

            visited[row][col] = true;

            if (i == 0) {
                continue;
            }

            int previousRow = solutionPath[i - 1][0];
            int previousCol = solutionPath[i - 1][1];

            int distance =
                    Math.abs(row - previousRow)
                            + Math.abs(col - previousCol);

            // Moves must be to adjacent cells.
            if (distance != 1) {
                return false;
            }

            // Check walls between the previous and current cells.
            if (row != previousRow) {

                int wallRow =
                        Math.min(row, previousRow);

                if (horizontalWalls[wallRow][col]) {
                    return false;
                }

            } else {

                int wallCol =
                        Math.min(col, previousCol);

                if (verticalWalls[row][wallCol]) {
                    return false;
                }
            }
        }

        return true;
    }


}