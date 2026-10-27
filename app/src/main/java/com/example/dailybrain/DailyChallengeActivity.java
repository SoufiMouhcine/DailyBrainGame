package com.example.dailybrain;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DailyChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView message = new TextView(this);
        message.setText("Checking today's challenge...");
        message.setTextSize(18);
        message.setPadding(40, 60, 40, 40);

        setContentView(message);

        PlayerManager.createOrLoadPlayer(
                new PlayerManager.PlayerCallback() {

                    @Override
                    public void onReady(
                            String playerId,
                            String displayName) {

                        checkTodayResult();
                    }

                    @Override
                    public void onError(Exception exception) {
                        showError(
                                "Could not load your player profile."
                        );
                    }
                }
        );
    }

    private void checkTodayResult() {

        String dateKey =
                DailyChallengeManager.getTodayDateKey();

        DailyLeaderboardManager.getDailyRank(
                dateKey,
                new DailyLeaderboardManager.DailyRankCallback() {

                    @Override
                    public void onRankFound(
                            int rank,
                            long officialTimeMs) {

                        Intent intent = new Intent(
                                DailyChallengeActivity.this,
                                DailyLeaderboardActivity.class
                        );

                        intent.putExtra("daily_rank", rank);
                        intent.putExtra(
                                "official_time_ms",
                                officialTimeMs
                        );
                        intent.putExtra("date_key", dateKey);
                        intent.putExtra("show_daily_interstitial", true);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onNoResult() {
                        Intent intent = new Intent(
                                DailyChallengeActivity.this,
                                MainActivity.class
                        );

                        intent.putExtra("daily_challenge", true);

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(Exception exception) {
                        android.util.Log.e(
                                "DailyBrain",
                                "Failed to check daily result",
                                exception
                        );

                        showError(
                                "Could not check today's result. "
                                        + "Please try again."
                        );
                    }
                }
        );
    }

    private void showError(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}