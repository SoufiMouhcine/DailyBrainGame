
package com.example.dailybrain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class GameView extends View {

    private final int GRID_SIZE = 5;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cellSize;
    private float gridStartX;
    private float gridStartY;

    private final ArrayList<int[]> path = new ArrayList<>();

    // Checkpoints: row, column
    private final int[][] checkpoints = {
            {0, 0}, // 1
            {2, 2}, // 2
            {4, 4}  // 3
    };

    private final int BACKGROUND_COLOR = Color.rgb(245, 247, 250);
    private final int CELL_COLOR = Color.WHITE;
    private final int PATH_COLOR = Color.rgb(100, 149, 237);
    private final int BORDER_COLOR = Color.rgb(210, 215, 225);

    public GameView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        cellSize = Math.min(width, height) / (GRID_SIZE + 0.5f);

        gridStartX = (width - cellSize * GRID_SIZE) / 2;
        gridStartY = (height - cellSize * GRID_SIZE) / 2;

        canvas.drawColor(BACKGROUND_COLOR);

        // Draw cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {

                float left = gridStartX + col * cellSize;
                float top = gridStartY + row * cellSize;

                RectF rect = new RectF(
                        left + 5,
                        top + 5,
                        left + cellSize - 5,
                        top + cellSize - 5
                );

                paint.setStyle(Paint.Style.FILL);

                if (isCellInPath(row, col)) {
                    paint.setColor(PATH_COLOR);
                } else {
                    paint.setColor(CELL_COLOR);
                }

                canvas.drawRoundRect(rect, 15, 15, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(BORDER_COLOR);

                canvas.drawRoundRect(rect, 15, 15, paint);
            }
        }

        // Draw path
        if (path.size() > 1) {

            paint.setColor(PATH_COLOR);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(cellSize * 0.12f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            for (int i = 0; i < path.size() - 1; i++) {

                int[] current = path.get(i);
                int[] next = path.get(i + 1);

                canvas.drawLine(
                        getCellCenterX(current[1]),
                        getCellCenterY(current[0]),
                        getCellCenterX(next[1]),
                        getCellCenterY(next[0]),
                        paint
                );
            }
        }

        // Draw numbered checkpoints
        drawCheckpoints(canvas);
    }

    private void drawCheckpoints(Canvas canvas) {

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(35, 45, 65));
        paint.setTextSize(cellSize * 0.42f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        for (int i = 0; i < checkpoints.length; i++) {

            int row = checkpoints[i][0];
            int col = checkpoints[i][1];

            float x = getCellCenterX(col);
            float y = getCellCenterY(row);

            // Vertically center the number
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float textY = y - (metrics.ascent + metrics.descent) / 2;

            canvas.drawText(String.valueOf(i + 1), x, textY, paint);
        }

        paint.setFakeBoldText(false);
    }

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (cellSize <= 0) {
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
                    )) {

                        // Do not revisit cells
                        if (!isCellInPath(row, col)) {

                            // Check checkpoint order
                            if (canVisitCell(row, col)) {

                                path.add(new int[]{row, col});
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