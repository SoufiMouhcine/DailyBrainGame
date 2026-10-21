
package com.example.dailybrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private android.view.View completionPanel;
    private TextView completionTitle;
    private TextView completionTime;
    private TextView cellCounter;
    private TextView timerText;
    private android.view.View gameControls;
    private Button nextStageButton;
    private Button replayButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAuth firebaseAuth;
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

        firebaseAuth = FirebaseAuth.getInstance();
        signInPlayer();
        gameControls = findViewById(R.id.gameControls);
        nextStageButton = findViewById(R.id.nextStageButton);
        replayButton = findViewById(R.id.replayButton);
        completionPanel = findViewById(R.id.completionPanel);
        completionTitle = findViewById(R.id.completionTitle);
        completionTime = findViewById(R.id.completionTime);
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


            private void openLeaderboard(int completedStage) {
                android.content.Intent intent =
                        new android.content.Intent(
                                MainActivity.this,
                                LeaderboardActivity.class
                        );

                intent.putExtra("stage_number", completedStage);
                startActivity(intent);
            }

            @Override
            public void onGameCompleted() {

// Calculate the precise completion time
                long completionTimeMs = timerRunning
                        ? System.currentTimeMillis() - startTime
                        : 0;

// Stop the timer
                stopTimer();

// Get the completed stage
                int completedStage =
                        getIntent().getIntExtra("stage_number", 1);
// Save the player's best time online
                PlayerManager.createOrLoadPlayer(
                        new PlayerManager.PlayerCallback() {

                            @Override
                            public void onReady(
                                    String playerId,
                                    String displayName) {

                                LeaderboardManager.saveBestTime(
                                        completedStage,
                                        completionTimeMs,
                                        displayName,
                                        new LeaderboardManager.LeaderboardCallback() {


                                            @Override
                                            public void onSaved(boolean newBest, long bestTimeMs) {
                                                android.util.Log.d(
                                                        "DailyBrain",
                                                        newBest
                                                                ? "New personal best saved!"
                                                                : "Existing personal best retained."
                                                );

                                                runOnUiThread(() -> openLeaderboard(completedStage));
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                android.util.Log.e(
                                                        "DailyBrain",
                                                        "Failed to save leaderboard score",
                                                        exception
                                                );

                                                runOnUiThread(() -> openLeaderboard(completedStage));
                                            }
                                        }
                                );
                            }

                            @Override
                            public void onError(Exception exception) {
                                android.util.Log.e(
                                        "DailyBrain",
                                        "Could not load player for leaderboard",
                                        exception
                                );
                            }
                        }
                );
                // Save unlocked stage
                android.content.SharedPreferences preferences =
                        getSharedPreferences("game_progress", MODE_PRIVATE);

                int unlockedStage =
                        preferences.getInt("unlocked_stage", 1);

                if (completedStage >= unlockedStage) {
                    preferences.edit()
                            .putInt("unlocked_stage", completedStage + 1)
                            .apply();
                }

                // Show completion message instead of closing immediately
                String elapsedTime = timerText.getText().toString()
                        .replace("Time: ", "")
                        .replace("TIME: ", "");

                completionTitle.setText(
                        "STAGE " + completedStage + " COMPLETE!"
                );

                completionTime.setText("TIME: " + elapsedTime);

                completionPanel.setVisibility(android.view.View.VISIBLE);
                gameControls.setVisibility(View.GONE);
                gameView.setEnabled(false);

                // Hide the game controls after winning
                gameView.setEnabled(false);
            }

        });

        undoButton.setOnClickListener(v -> {
            gameView.undoMove();
        });

        hintButton.setOnClickListener(v -> {
            gameView.showHint();
        });

        nextStageButton.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            MainActivity.class
                    );

            intent.putExtra("stage_number", stageNumber + 1);
            startActivity(intent);
            finish();
        });

        replayButton.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            MainActivity.class
                    );

            intent.putExtra("stage_number", stageNumber);
            startActivity(intent);
            finish();
        });
    }

    private void signInPlayer() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            loadPlayerProfile();
            return;
        }

        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        loadPlayerProfile();
                    } else {
                        android.util.Log.e(
                                "DailyBrain",
                                "Anonymous sign-in failed",
                                task.getException()
                        );
                    }
                });
    }

    private void loadPlayerProfile() {
        PlayerManager.createOrLoadPlayer(new PlayerManager.PlayerCallback() {
            @Override
            public void onReady(String playerId, String displayName) {
                android.util.Log.d(
                        "DailyBrain",
                        "Player ready: " + displayName + ", ID: " + playerId
                );
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.e(
                        "DailyBrain",
                        "Failed to load player profile",
                        exception
                );
            }
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