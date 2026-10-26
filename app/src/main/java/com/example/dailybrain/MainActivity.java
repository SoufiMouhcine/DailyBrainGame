
package com.example.dailybrain;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.HashSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;

public class MainActivity extends AppCompatActivity {
    private boolean practiceMode = false;
    private boolean dailyChallengeMode = false;
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
        initializeAdMob();
        setContentView(R.layout.activity_main);
        practiceMode = getIntent().getBooleanExtra(
                "practice_mode",
                false
        );

        dailyChallengeMode = getIntent().getBooleanExtra(
                "daily_challenge",
                false
        );
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

        Puzzle puzzle;

        if (dailyChallengeMode) {
            puzzle = DailyChallengeManager.generateTodayPuzzle();
        } else {
            puzzle = StageManager.getStage(stageNumber);
        }

        gameView.setPuzzle(puzzle);

        TextView puzzleNumberText =
                findViewById(R.id.puzzleNumberText);

        if (dailyChallengeMode) {
            puzzleNumberText.setText(
                    "DAILY CHALLENGE · "
                            + DailyChallengeManager.getTodayDateKey()
            );
        } else {
            puzzleNumberText.setText(
                    "STAGE #" + stageNumber
            );
        }

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


            private String formatDailyTime(long timeMs) {
                long minutes = timeMs / 60000;
                long seconds = (timeMs % 60000) / 1000;
                long hundredths = (timeMs % 1000) / 10;

                return String.format(
                        java.util.Locale.US,
                        "%02d:%02d.%02d",
                        minutes,
                        seconds,
                        hundredths
                );
            }

            @Override
            public void onGameCompleted() {

                // Calculate the completion time
                long completionTimeMs = timerRunning
                        ? System.currentTimeMillis() - startTime
                        : 0;

                // Stop the timer
                stopTimer();

                // Get the completed stage
                int completedStage =
                        getIntent().getIntExtra("stage_number", 1);

                // Prepare the displayed completion time
                String elapsedTime = timerText.getText().toString()
                        .replace("Time: ", "")
                        .replace("TIME: ", "");

                // Update the completion screen
                completionTitle.setText(
                        practiceMode
                                ? "PRACTICE COMPLETE!"
                                : "STAGE " + completedStage + " COMPLETE!"
                );

                completionTime.setText("TIME: " + elapsedTime);

                completionPanel.setVisibility(View.VISIBLE);
                gameControls.setVisibility(View.GONE);
                gameView.setEnabled(false);

                // Practice runs must not affect official results or progression.
                if (practiceMode) {
                    return;
                }
                if (dailyChallengeMode) {
                    completionTitle.setText("DAILY CHALLENGE COMPLETE!");

                    final String displayedTime =
                            completionTime.getText().toString();

                    String dateKey = DailyChallengeManager.getTodayDateKey();

                    PlayerManager.createOrLoadPlayer(
                            new PlayerManager.PlayerCallback() {
                                @Override
                                public void onReady(
                                        String playerId,
                                        String displayName) {

                                    DailyLeaderboardManager.saveDailyTime(
                                            dateKey,
                                            completionTimeMs,
                                            displayName,
                                            new DailyLeaderboardManager.DailyLeaderboardCallback() {


                                                @Override
                                                public void onSaved(
                                                        boolean firstCompletion,
                                                        long officialTimeMs
                                                ) {
                                                    runOnUiThread(() -> {
                                                        completionTime.setText(
                                                                "Official result saved!\n"
                                                                        + "TIME: "
                                                                        + formatDailyTime(officialTimeMs)
                                                        );

                                                        Intent intent = new Intent(
                                                                MainActivity.this,
                                                                DailyLeaderboardActivity.class
                                                        );

                                                        intent.putExtra(
                                                                "date_key",
                                                                dateKey
                                                        );

                                                        intent.putExtra(
                                                                "official_time_ms",
                                                                officialTimeMs
                                                        );

                                                        startActivity(intent);
                                                    });
                                                }


                                                @Override
                                                public void onError(Exception exception) {
                                                    completionTime.setText(
                                                            "Challenge completed!\n"
                                                                    + "Could not save your result."
                                                    );

                                                    android.util.Log.e(
                                                            "DailyBrain",
                                                            "Failed to save daily result",
                                                            exception
                                                    );
                                                }
                                            }
                                    );
                                }

                                @Override
                                public void onError(Exception exception) {
                                    completionTime.setText(
                                            "Challenge completed!\n"
                                                    + "Could not load your profile."
                                    );

                                    android.util.Log.e(
                                            "DailyBrain",
                                            "Failed to load player profile",
                                            exception
                                    );
                                }
                            }
                    );

                    return;
                }

// Keep your existing stage completion and leaderboard logic below unchanged.
                // Unlock the next stage for an official completion

                // Save official stage completion and unlock the next stage.
                android.content.SharedPreferences preferences =
                        getSharedPreferences("game_progress", MODE_PRIVATE);

                int unlockedStage =
                        preferences.getInt("unlocked_stage", 1);

                java.util.Set<String> completedStages = new HashSet<>(
                        preferences.getStringSet("completed_stages", new HashSet<>())
                );

                completedStages.add(String.valueOf(completedStage));

                android.content.SharedPreferences.Editor editor =
                        preferences.edit();

                editor.putStringSet("completed_stages", completedStages);

                if (completedStage >= unlockedStage) {
                    editor.putInt("unlocked_stage", completedStage + 1);
                }

                editor.apply();


                // Save the first official score and open the leaderboard
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
                                            public void onSaved(
                                                    boolean newBest,
                                                    long bestTimeMs) {

                                                android.util.Log.d(
                                                        "DailyBrain",
                                                        newBest
                                                                ? "First official score saved!"
                                                                : "Official score preserved; replay did not change it."
                                                );

                                                runOnUiThread(
                                                        () -> openLeaderboard(completedStage)
                                                );
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                android.util.Log.e(
                                                        "DailyBrain",
                                                        "Failed to save leaderboard score",
                                                        exception
                                                );

                                                runOnUiThread(
                                                        () -> openLeaderboard(completedStage)
                                                );
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

                                // Keep the completion screen available if Firebase fails.
                            }
                        }
                );
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
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Practice replay?")
                    .setMessage(
                            "You've already completed this stage. " +
                                    "Your original time and official leaderboard rank " +
                                    "will remain unchanged. This replay is for practice only."
                    )
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton("Practice", (dialog, which) -> {
                        Intent intent = new Intent(
                                MainActivity.this,
                                MainActivity.class
                        );

                        if (dailyChallengeMode) {

                            intent.putExtra("daily_challenge", true);
                            intent.putExtra("practice_mode", true);

                        } else {
                            intent.putExtra("stage_number", stageNumber);
                            intent.putExtra("practice_mode", true);
                        }

                        startActivity(intent);
                        finish();
                    })
                    .show();
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

    private void initializeAdMob() {
        new Thread(() -> {
            MobileAds.initialize(
                    getApplicationContext(),
                    new InitializationConfig.Builder(
                            "ca-app-pub-3940256099942544~3347511713"
                    ).build(),
                    initializationStatus -> {
                        android.util.Log.d(
                                "DailyBrainAds",
                                "AdMob initialization completed"
                        );
                    }
            );
        }).start();
    }

}