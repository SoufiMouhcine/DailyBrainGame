
package com.example.dailybrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    private TextView cellCounter;
    private TextView resultText;
    private TextView timerText;

    private Handler handler = new Handler(Looper.getMainLooper());

    private long startTime = 0;
    private boolean timerRunning = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            if (!timerRunning) {
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;

            int seconds = (int) (elapsed / 1000);

            int minutes = seconds / 60;
            seconds = seconds % 60;

            String time = String.format(
                    java.util.Locale.getDefault(),
                    "Time: %02d:%02d",
                    minutes,
                    seconds
            );

            timerText.setText(time);

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        int stageNumber =
                getIntent().getIntExtra("stage_number", 1);

        Puzzle puzzle =
                StageManager.getStage(stageNumber);

        gameView.setPuzzle(puzzle);

        TextView puzzleNumberText =
                findViewById(R.id.puzzleNumberText);

        puzzleNumberText.setText(
                "STAGE #" + stageNumber
        );

        cellCounter = findViewById(R.id.cellCounter);
        resultText = findViewById(R.id.resultText);
        timerText = findViewById(R.id.timerText);

        Button hintButton = findViewById(R.id.hintButton);
        Button undoButton = findViewById(R.id.undoButton);

        gameView.setGameListener(new GameView.GameListener() {

            @Override
            public void onProgressChanged(int count) {

                cellCounter.setText("Cells: " + count + " / 25");

                if (count > 0 && !timerRunning) {
                    startTimer();
                }
            }

            @Override
            public void onGameCompleted() {

                resultText.setText("YOU WIN!");

                int completedStage =
                        getIntent().getIntExtra("stage_number", 1);

                android.content.SharedPreferences preferences =
                        getSharedPreferences("game_progress", MODE_PRIVATE);

                int unlockedStage =
                        preferences.getInt("unlocked_stage", 1);

                if (completedStage >= unlockedStage) {

                    preferences.edit()
                            .putInt(
                                    "unlocked_stage",
                                    completedStage + 1
                            )
                            .apply();
                }

                // Return to Stage Selection
                finish();
            }
        });

        undoButton.setOnClickListener(v -> {
            gameView.undoMove();
        });

        hintButton.setOnClickListener(v -> {
            gameView.showHint();
        });
    }

    private void startTimer() {

        if (timerRunning) {
            return;
        }

        timerRunning = true;

        startTime = System.currentTimeMillis();

        handler.post(timerRunnable);
    }

    private void stopTimer() {

        timerRunning = false;

        handler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {

        stopTimer();

        super.onDestroy();
    }
}