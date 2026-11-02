
package com.mssf.dailybrain;

public class PuzzleSolver {

    private static SearchState lastSearchState;

    private static final int[][] DIRECTIONS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    private PuzzleSolver() {
    }

    public static int countSolutions(
            Puzzle puzzle,
            int maxSolutions,
            int maxNodes) {

        lastSearchState = null;

        if (puzzle == null ||
                maxSolutions < 1 ||
                maxNodes < 1) {
            return 0;
        }

        int size = puzzle.getSize();
        int totalCells = size * size;

        int[][] checkpoints = puzzle.getCheckpoints();
        boolean[][] horizontalWalls = puzzle.getHorizontalWalls();
        boolean[][] verticalWalls = puzzle.getVerticalWalls();

        if (!isValidInput(
                size,
                checkpoints,
                horizontalWalls,
                verticalWalls)) {
            return 0;
        }

        boolean[][] visited = new boolean[size][size];

        int startRow = checkpoints[0][0];
        int startCol = checkpoints[0][1];

        visited[startRow][startCol] = true;

        SearchState state = new SearchState(
                maxSolutions,
                maxNodes
        );

        lastSearchState = state;

        search(
                size,
                totalCells,
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
            int size,
            int[][] checkpoints,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        if (size < 2 ||
                checkpoints == null ||
                checkpoints.length < 2 ||
                horizontalWalls == null ||
                horizontalWalls.length != size - 1 ||
                verticalWalls == null ||
                verticalWalls.length != size) {
            return false;
        }

        // Horizontal walls: (size - 1) rows, size columns.
        for (int row = 0; row < size - 1; row++) {
            if (horizontalWalls[row] == null ||
                    horizontalWalls[row].length != size) {
                return false;
            }
        }

        // Vertical walls: size rows, (size - 1) columns.
        for (int row = 0; row < size; row++) {
            if (verticalWalls[row] == null ||
                    verticalWalls[row].length != size - 1) {
                return false;
            }
        }

        // Check that checkpoints are inside the grid and unique.
        for (int i = 0; i < checkpoints.length; i++) {

            int[] checkpoint = checkpoints[i];

            if (checkpoint == null ||
                    checkpoint.length < 2 ||
                    checkpoint[0] < 0 ||
                    checkpoint[0] >= size ||
                    checkpoint[1] < 0 ||
                    checkpoint[1] >= size) {
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
            int size,
            int totalCells,
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

        // All cells must be visited, and the final checkpoint
        // must be the endpoint of the path.
        if (visitedCount == totalCells) {

            int[] finalCheckpoint = checkpoints[checkpoints.length - 1];

            if (nextCheckpoint == checkpoints.length &&
                    row == finalCheckpoint[0] &&
                    col == finalCheckpoint[1]) {
                state.solutions++;
            }

            return;
        }

        // Once the final checkpoint has been reached,
        // the path cannot continue to another cell.
        if (nextCheckpoint == checkpoints.length) {
            return;
        }

        // Prune paths if remaining cells are disconnected.
        if (!remainingCellsConnected(
                size,
                row,
                col,
                visited,
                horizontalWalls,
                verticalWalls)) {
            return;
        }

        for (int[] direction : DIRECTIONS) {

            if (state.solutions >= state.maxSolutions ||
                    state.nodes >= state.maxNodes) {
                return;
            }

            int nextRow = row + direction[0];
            int nextCol = col + direction[1];

            if (nextRow < 0 || nextRow >= size ||
                    nextCol < 0 || nextCol >= size) {
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

            // Checkpoints must be visited in order.
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
                    size,
                    totalCells,
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

    public static boolean wasSearchExhausted(Puzzle puzzle) {
        SearchState state = lastSearchState;

        return state != null &&
                state.nodes >= state.maxNodes;
    }

    private static boolean remainingCellsConnected(
            int size,
            int currentRow,
            int currentCol,
            boolean[][] visited,
            boolean[][] horizontalWalls,
            boolean[][] verticalWalls) {

        boolean[][] reached = new boolean[size][size];

        int totalCells = size * size;

        int[] queueRows = new int[totalCells];
        int[] queueCols = new int[totalCells];

        int head = 0;
        int tail = 0;

        queueRows[tail] = currentRow;
        queueCols[tail] = currentCol;
        tail++;

        reached[currentRow][currentCol] = true;

        while (head < tail) {

            int row = queueRows[head];
            int col = queueCols[head];
            head++;

            for (int[] direction : DIRECTIONS) {

                int nextRow = row + direction[0];
                int nextCol = col + direction[1];

                if (nextRow < 0 || nextRow >= size ||
                        nextCol < 0 || nextCol >= size) {
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
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {

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
}
