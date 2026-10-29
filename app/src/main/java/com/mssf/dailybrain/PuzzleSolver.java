
package com.mssf.dailybrain;

public class PuzzleSolver {

    private static final int SIZE = 5;
    private static final int TOTAL_CELLS = SIZE * SIZE;
    private static final int MAX_SEARCH_NODES = 100_000;
    private static SearchState lastSearchState;
    private PuzzleSolver() {
    }

    /**
     * Counts solutions up to maxSolutions.
     * Stops searching after maxNodes recursive visits.
     *
     * The result is the number found within the search budget.
     * A result of 1 does not necessarily prove uniqueness if
     * the search budget was exhausted.
     */
    public static int countSolutions(
            Puzzle puzzle,
            int maxSolutions,
            int maxNodes) {

        if (puzzle == null ||
                maxSolutions < 1 ||
                maxNodes < 1) {
            return 0;
        }

        int[][] checkpoints = puzzle.getCheckpoints();
        boolean[][] horizontalWalls =
                puzzle.getHorizontalWalls();
        boolean[][] verticalWalls =
                puzzle.getVerticalWalls();

        if (!isValidInput(
                checkpoints,
                horizontalWalls,
                verticalWalls)) {
            return 0;
        }

        boolean[][] visited = new boolean[SIZE][SIZE];

        int startRow = checkpoints[0][0];
        int startCol = checkpoints[0][1];

        visited[startRow][startCol] = true;

        SearchState state = new SearchState(
                maxSolutions,
                maxNodes
        );

        lastSearchState = state;

        search(
                startRow,
                startCol,
                1,
                1,
                visited,
                checkpoints,
                horizontalWalls,
                verticalWalls,
                state
        );

        return state.solutions;
    }


    private static boolean isValidInput(
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        if (checkpoints == null ||
                checkpoints.length < 2 ||
                horizontalWalls == null ||
                horizontalWalls.length != SIZE - 1 ||
                verticalWalls == null ||
                verticalWalls.length != SIZE) {
            return false;
        }

        // Horizontal walls: (SIZE - 1) rows, SIZE columns.
        for (int row = 0; row < SIZE - 1; row++) {
            if (horizontalWalls[row] == null ||
                    horizontalWalls[row].length != SIZE) {
                return false;
            }
        }

        // Vertical walls: SIZE rows, (SIZE - 1) columns.
        for (int row = 0; row < SIZE; row++) {
            if (verticalWalls[row] == null ||
                    verticalWalls[row].length != SIZE - 1) {
                return false;
            }
        }

        // Check that checkpoints are inside the grid and unique.
        for (int i = 0; i < checkpoints.length; i++) {
            int[] checkpoint = checkpoints[i];

            if (checkpoint == null ||
                    checkpoint.length < 2 ||
                    checkpoint[0] < 0 ||
                    checkpoint[0] >= SIZE ||
                    checkpoint[1] < 0 ||
                    checkpoint[1] >= SIZE) {
                return false;
            }

            for (int j = 0; j < i; j++) {
                if (checkpoints[j][0] == checkpoint[0] &&
                        checkpoints[j][1] == checkpoint[1]) {
                    return false;
                }
            }
        }

        return true;
    }


    private static void search(
            int row,
            int col,
            int visitedCount,
            int nextCheckpoint,
            boolean[][] visited,
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls,
            SearchState state) {

        if (state.solutions >= state.maxSolutions ||
                state.nodes >= state.maxNodes) {
            return;
        }

        state.nodes++;

        if (visitedCount == TOTAL_CELLS) {
            if (nextCheckpoint == checkpoints.length) {
                state.solutions++;
            }
            return;
        }
// Prune paths if remaining cells are disconnected.
        if (!remainingCellsConnected(
                row,
                col,
                visited,
                horizontalWalls,
                verticalWalls)) {
            return;
        }
        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        for (int[] direction : directions) {

            if (state.solutions >= state.maxSolutions ||
                    state.nodes >= state.maxNodes) {
                return;
            }

            int nextRow = row + direction[0];
            int nextCol = col + direction[1];

            if (nextRow < 0 || nextRow >= SIZE ||
                    nextCol < 0 || nextCol >= SIZE) {
                continue;
            }

            if (visited[nextRow][nextCol]) {
                continue;
            }

            if (hasWall(
                    row, col,
                    nextRow, nextCol,
                    horizontalWalls,
                    verticalWalls)) {
                continue;
            }

            int checkpointIndex = findCheckpoint(
                    nextRow,
                    nextCol,
                    checkpoints
            );

            if (checkpointIndex != -1 &&
                    checkpointIndex != nextCheckpoint) {
                continue;
            }

            visited[nextRow][nextCol] = true;

            int updatedCheckpoint = nextCheckpoint;

            if (checkpointIndex == nextCheckpoint) {
                updatedCheckpoint++;
            }

            search(
                    nextRow,
                    nextCol,
                    visitedCount + 1,
                    updatedCheckpoint,
                    visited,
                    checkpoints,
                    horizontalWalls,
                    verticalWalls,
                    state
            );

            visited[nextRow][nextCol] = false;
        }
    }

    public static int getLastSearchNodeCount() {
        SearchState state = lastSearchState;
        return state == null ? 0 : state.nodes;
    }

    private static boolean remainingCellsConnected(
            int currentRow,
            int currentCol,
            boolean[][] visited,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        boolean[][] reached = new boolean[SIZE][SIZE];

        int[] queueRows = new int[TOTAL_CELLS];
        int[] queueCols = new int[TOTAL_CELLS];

        int head = 0;
        int tail = 0;

        queueRows[tail] = currentRow;
        queueCols[tail] = currentCol;
        tail++;

        reached[currentRow][currentCol] = true;

        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        while (head < tail) {
            int row = queueRows[head];
            int col = queueCols[head];
            head++;

            for (int[] direction : directions) {
                int nextRow = row + direction[0];
                int nextCol = col + direction[1];

                if (nextRow < 0 || nextRow >= SIZE ||
                        nextCol < 0 || nextCol >= SIZE) {
                    continue;
                }

                // Previously visited cells cannot be revisited,
                // except the current endpoint.
                if (visited[nextRow][nextCol] &&
                        !(nextRow == currentRow &&
                                nextCol == currentCol)) {
                    continue;
                }

                if (reached[nextRow][nextCol]) {
                    continue;
                }

                if (hasWall(
                        row, col,
                        nextRow, nextCol,
                        horizontalWalls,
                        verticalWalls)) {
                    continue;
                }

                reached[nextRow][nextCol] = true;
                queueRows[tail] = nextRow;
                queueCols[tail] = nextCol;
                tail++;
            }
        }

        // Every unvisited cell must remain reachable.
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!visited[row][col] &&
                        !reached[row][col]) {
                    return false;
                }
            }
        }

        return true;
    }
    private static int findCheckpoint(
            int row,
            int col,
            int[][] checkpoints) {

        for (int i = 0; i < checkpoints.length; i++) {
            if (checkpoints[i][0] == row &&
                    checkpoints[i][1] == col) {
                return i;
            }
        }

        return -1;
    }

    private static boolean hasWall(
            int row1,
            int col1,
            int row2,
            int col2,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        if (col1 == col2) {
            int wallRow = Math.min(row1, row2);
            return horizontalWalls[wallRow][col1];
        }

        int wallCol = Math.min(col1, col2);
        return verticalWalls[row1][wallCol];
    }

    private static class SearchState {
        final int maxSolutions;
        final int maxNodes;
        int solutions = 0;
        int nodes = 0;

        SearchState(int maxSolutions, int maxNodes) {
            this.maxSolutions = maxSolutions;
            this.maxNodes = maxNodes;
        }
    }
    public static boolean wasSearchExhausted(Puzzle puzzle) {
        SearchState state = lastSearchState;

        return state != null
                && state.nodes >= state.maxNodes;
    }
}
