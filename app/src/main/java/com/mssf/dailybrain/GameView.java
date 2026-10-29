
package com.mssf.dailybrain;

import android.util.AttributeSet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Path;
import java.util.ArrayList;

public class GameView extends View {

    private final int GRID_SIZE = 5;
    private boolean gameCompleted = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cellSize;
    private float gridStartX;
    private float gridStartY;

    private final ArrayList<int[]> path = new ArrayList<>();

    // Checkpoints: row, column
    private int[][] checkpoints;
    private boolean[][] horizontalWalls;
    private boolean[][] verticalWalls;
    private final int BACKGROUND_COLOR = Color.rgb(245, 247, 250);
    private final int CELL_COLOR = Color.WHITE;
    private final int PATH_COLOR = Color.rgb(38, 180, 70);
    private final int GRID_LINE_COLOR = Color.rgb(220, 223, 228);
    private final int CHECKPOINT_COLOR = Color.BLACK;
    private final int CHECKPOINT_START_COLOR = Color.rgb(76, 200, 70);
    private final int BORDER_COLOR = Color.rgb(210, 215, 225);
    private int[][] solutionPath;

    public interface GameListener {

        void onProgressChanged(int count);

        void onGameCompleted();
    }

    public void setPuzzle(Puzzle puzzle) {

        checkpoints = puzzle.getCheckpoints();

        horizontalWalls =
                puzzle.getHorizontalWalls();

        verticalWalls =
                puzzle.getVerticalWalls();
        solutionPath =
                puzzle.getSolutionPath();

        path.clear();
        gameCompleted = false;

        invalidate();
    }

    private GameListener gameListener;

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void resetGame() {

        path.clear();
        gameCompleted = false;

        if (gameListener != null) {
            gameListener.onProgressChanged(0);
        }

        invalidate();
    }

    private void notifyProgress() {

        if (gameListener != null) {
            gameListener.onProgressChanged(path.size());
        }
    }

    private boolean isGameCompleted() {

        if (path.size() != GRID_SIZE * GRID_SIZE) {
            return false;
        }

        int[] lastCell = path.get(path.size() - 1);

        int lastCheckpoint = getCheckpointIndex(
                lastCell[0],
                lastCell[1]
        );

        return lastCheckpoint == checkpoints.length - 1;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        // Keep the board square and centered
        cellSize = Math.min(width, height) / (GRID_SIZE + 0.15f);

        gridStartX = (width - cellSize * GRID_SIZE) / 2f;
        gridStartY = (height - cellSize * GRID_SIZE) / 2f;

        canvas.drawColor(BACKGROUND_COLOR);

        // 1. Draw white cells and thin grid lines
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {

                float left = gridStartX + col * cellSize;
                float top = gridStartY + row * cellSize;

                RectF rect = new RectF(
                        left,
                        top,
                        left + cellSize,
                        top + cellSize
                );

                paint.setColor(CELL_COLOR);
                canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                paint.setColor(GRID_LINE_COLOR);

                canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);
            }
        }

        // 2. Draw the outer border ONCE
        RectF boardRect = new RectF(
                gridStartX + 1.5f,
                gridStartY + 1.5f,
                gridStartX + GRID_SIZE * cellSize - 1.5f,
                gridStartY + GRID_SIZE * cellSize - 1.5f
        );

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(165, 170, 178));

        canvas.drawRoundRect(
                boardRect,
                18f,
                18f,
                paint
        );

        // 3. Draw walls between cells
        drawWalls(canvas);

        // 4. Draw the player's green path

        // Draw player path
        if (path.size() > 1) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(PATH_COLOR);
            paint.setStrokeWidth(cellSize * 0.17f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            Path linePath = new Path();

            int[] first = path.get(0);
            linePath.moveTo(
                    getCellCenterX(first[1]),
                    getCellCenterY(first[0])
            );

            for (int i = 1; i < path.size(); i++) {
                int[] cell = path.get(i);

                linePath.lineTo(
                        getCellCenterX(cell[1]),
                        getCellCenterY(cell[0])
                );
            }

            canvas.drawPath(linePath, paint);

            paint.setStyle(Paint.Style.FILL);
        }


        // 5. Draw the Hint highlight
        drawHint(canvas);

        // 6. Draw checkpoint circles last so they stay visible
        if (checkpoints != null) {
            drawCheckpoints(canvas);
        }
    }
    private void drawHint(Canvas canvas) {

        if (hintCell == null) {
            return;
        }

        float x =
                getCellCenterX(hintCell[1]);

        float y =
                getCellCenterY(hintCell[0]);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.07f);
        paint.setColor(
                Color.rgb(76, 200, 70)
        );

        canvas.drawCircle(
                x,
                y,
                cellSize * 0.32f,
                paint
        );
    }
    private void drawWalls(Canvas canvas) {

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cellSize * 0.14f);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        // Horizontal walls
        for (int row = 0; row < GRID_SIZE - 1; row++) {

            for (int col = 0; col < GRID_SIZE; col++) {

                if (horizontalWalls != null &&
                        horizontalWalls[row][col]) {

                    float x1 =
                            gridStartX +
                                    col * cellSize;

                    float x2 =
                            x1 + cellSize;

                    float y =
                            gridStartY +
                                    (row + 1) * cellSize;

                    canvas.drawLine(
                            x1,
                            y,
                            x2,
                            y,
                            paint
                    );
                }
            }
        }

        // Vertical walls
        for (int row = 0; row < GRID_SIZE; row++) {

            for (int col = 0; col < GRID_SIZE - 1; col++) {

                if (verticalWalls != null &&
                        verticalWalls[row][col]) {

                    float x =
                            gridStartX +
                                    (col + 1) * cellSize;

                    float y1 =
                            gridStartY +
                                    row * cellSize;

                    float y2 =
                            y1 + cellSize;

                    canvas.drawLine(
                            x,
                            y1,
                            x,
                            y2,
                            paint
                    );
                }
            }
        }
    }

    private void drawCheckpoints(Canvas canvas) {
        if (checkpoints == null) {
            return;
        }

        for (int i = 0; i < checkpoints.length; i++) {
            int row = checkpoints[i][0];
            int col = checkpoints[i][1];

            float x = getCellCenterX(col);
            float y = getCellCenterY(row);

            float radius = cellSize * 0.25f;

            // Green outer ring for the starting checkpoint
            if (i == 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(CHECKPOINT_START_COLOR);
                canvas.drawCircle(
                        x,
                        y,
                        radius + cellSize * 0.065f,
                        paint
                );
            }

            // Black checkpoint circle
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CHECKPOINT_COLOR);
            canvas.drawCircle(x, y, radius, paint);

            // White checkpoint number
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cellSize * 0.28f);
            paint.setTypeface(
                    android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.BOLD
                    )
            );

            Paint.FontMetrics metrics = paint.getFontMetrics();

            float textY = y
                    - (metrics.ascent + metrics.descent) / 2f;

            canvas.drawText(
                    String.valueOf(i + 1),
                    x,
                    textY,
                    paint
            );
        }

        // Reset paint settings for other drawing operations
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(
                android.graphics.Typeface.DEFAULT
        );
    }

    public void undoMove() {

        if (gameCompleted) {
            return;
        }

        if (path.size() <= 1) {
            return;
        }

        path.remove(path.size() - 1);

        notifyProgress();
        hintCell = null;
        invalidate();
    }

    public void showHint() {

        if (gameCompleted || solutionPath == null
                || solutionPath.length == 0) {
            return;
        }

        int[] lastCell;

        // If the player hasn't moved, start from
        // the first cell in the solution.
        if (path.isEmpty()) {
            lastCell = solutionPath[0];
        } else {
            lastCell = path.get(path.size() - 1);
        }

        int solutionIndex = findSolutionIndex(
                lastCell[0],
                lastCell[1]
        );

        if (solutionIndex == -1
                || solutionIndex >= solutionPath.length - 1) {
            return;
        }

        // Find the next valid cell in the solution.
        for (int i = solutionIndex + 1;
             i < solutionPath.length;
             i++) {

            int row = solutionPath[i][0];
            int col = solutionPath[i][1];

            if (!isCellInPath(row, col)
                    && isAdjacent(
                    lastCell[0],
                    lastCell[1],
                    row,
                    col
            )
                    && !isWallBetween(
                    lastCell[0],
                    lastCell[1],
                    row,
                    col
            )) {

                hintCell = new int[]{row, col};
                invalidate();
                return;
            }
        }
    }

    private int findSolutionIndex(
            int row,
            int col) {

        for (int i = 0;
             i < solutionPath.length;
             i++) {

            if (solutionPath[i][0] == row &&
                    solutionPath[i][1] == col) {

                return i;
            }
        }

        return -1;
    }
    private int[] hintCell = null;
    private float getCellCenterX(int col) {
        return gridStartX + col * cellSize + cellSize / 2;
    }

    private float getCellCenterY(int row) {
        return gridStartY + row * cellSize + cellSize / 2;
    }

    private boolean isCellInPath(int row, int col) {

        for (int[] cell : path) {

            if (cell[0] == row && cell[1] == col) {
                return true;
            }
        }

        return false;
    }

    private boolean isAdjacent(
            int row1, int col1,
            int row2, int col2) {

        return Math.abs(row1 - row2) +
                Math.abs(col1 - col2) == 1;
    }

    // Returns checkpoint index, or -1 if not a checkpoint
    private int getCheckpointIndex(int row, int col) {

        for (int i = 0; i < checkpoints.length; i++) {

            if (checkpoints[i][0] == row &&
                    checkpoints[i][1] == col) {

                return i;
            }
        }

        return -1;
    }

    // Determines which checkpoint must be reached next
    private int getNextCheckpointIndex() {

        int reached = 0;

        for (int[] cell : path) {

            int index = getCheckpointIndex(
                    cell[0],
                    cell[1]
            );

            if (index >= reached) {
                reached = index + 1;
            }
        }

        return reached;
    }

    private boolean canVisitCell(int row, int col) {

        int checkpointIndex = getCheckpointIndex(row, col);

        // Ordinary cell
        if (checkpointIndex == -1) {
            return true;
        }

        // Must visit checkpoints in order
        return checkpointIndex == getNextCheckpointIndex();
    }
    private boolean isWallBetween(
            int row1,
            int col1,
            int row2,
            int col2) {

        // Moving vertically
        if (col1 == col2) {

            int wallRow =
                    Math.min(row1, row2);

            return horizontalWalls != null &&
                    horizontalWalls[wallRow][col1];
        }

        // Moving horizontally
        if (row1 == row2) {

            int wallCol =
                    Math.min(col1, col2);

            return verticalWalls != null &&
                    verticalWalls[row1][wallCol];
        }

        return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (cellSize <= 0) {
            return true;
        }
        if (gameCompleted) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        int col = (int) ((x - gridStartX) / cellSize);
        int row = (int) ((y - gridStartY) / cellSize);

        boolean insideGrid =
                row >= 0 && row < GRID_SIZE &&
                        col >= 0 && col < GRID_SIZE;

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                // Path must start at checkpoint 1
                if (insideGrid &&
                        row == checkpoints[0][0] &&
                        col == checkpoints[0][1]) {

                    path.clear();
                    path.add(new int[]{row, col});
                    hintCell = null;
                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                if (insideGrid && !path.isEmpty()) {

                    int[] lastCell = path.get(path.size() - 1);

                    // Undo last movement
                    if (path.size() > 1) {

                        int[] previousCell =
                                path.get(path.size() - 2);

                        if (previousCell[0] == row &&
                                previousCell[1] == col) {

                            path.remove(path.size() - 1);
                            notifyProgress(); // Update counter after removing a cell

                            invalidate();

                            return true;
                        }
                    }

                    // Add adjacent cells only
                    if (isAdjacent(
                            lastCell[0],
                            lastCell[1],
                            row,
                            col
                    ) &&
                            !isWallBetween(
                                    lastCell[0],
                                    lastCell[1],
                                    row,
                                    col
                            )) {

                        // Do not revisit cells
                        if (!isCellInPath(row, col)) {

                            // Check checkpoint order
                            if (canVisitCell(row, col)) {

                                path.add(new int[]{row, col});
                                hintCell = null;
                                notifyProgress();

                                if (isGameCompleted()) {

                                    gameCompleted = true;

                                    if (gameListener != null) {
                                        gameListener.onGameCompleted();
                                    }
                                }

                                invalidate();
                            }
                        }
                    }
                }

                return true;

            case MotionEvent.ACTION_UP:

                return true;

            case MotionEvent.ACTION_CANCEL:

                path.clear();
                invalidate();

                return true;
        }

        return true;
    }
}