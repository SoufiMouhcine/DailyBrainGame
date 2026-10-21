
package com.example.dailybrain;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.AggregateSource;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LeaderboardActivity extends AppCompatActivity {

    private LinearLayout scoresContainer;
    private TextView playerRankView;
    private String currentPlayerId;
    private int stageNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stageNumber = getIntent().getIntExtra("stage_number", 1);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentPlayerId =
                    FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(Color.rgb(16, 24, 39));

        TextView title = new TextView(this);
        title.setText("STAGE " + stageNumber + " LEADERBOARD");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("FASTEST TIMES");
        subtitle.setTextColor(Color.rgb(100, 181, 246));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 24);
        root.addView(subtitle);
        playerRankView = new TextView(this);
        playerRankView.setText("YOUR RANK: Calculating...");
        playerRankView.setTextColor(Color.WHITE);
        playerRankView.setTextSize(16);
        playerRankView.setGravity(Gravity.CENTER);
        playerRankView.setPadding(12, 12, 12, 20);

        root.addView(playerRankView);
        ScrollView scrollView = new ScrollView(this);

        scoresContainer = new LinearLayout(this);
        scoresContainer.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(scoresContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);

        loadLeaderboard();
        loadPlayerRank();
    }

    private void loadPlayerRank() {
        if (currentPlayerId == null) {
            playerRankView.setText(
                    "Sign in to see your rank."
            );
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.firebase.firestore.CollectionReference scoresRef =
                db.collection("stageLeaderboards")
                        .document(String.valueOf(stageNumber))
                        .collection("scores");

        // Find this player's saved best time.
        scoresRef.document(currentPlayerId)
                .get()
                .addOnSuccessListener(playerDocument -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!playerDocument.exists()) {
                        playerRankView.setText(
                                "Complete this stage to get your rank."
                        );
                        return;
                    }

                    Long bestTimeMs =
                            playerDocument.getLong("bestTimeMs");

                    if (bestTimeMs == null) {
                        playerRankView.setText(
                                "Your rank is currently unavailable."
                        );
                        return;
                    }

                    // Count players with a strictly faster time.
                    scoresRef
                            .whereLessThan("bestTimeMs", bestTimeMs)
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(fasterSnapshot -> {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                long rank = fasterSnapshot.getCount() + 1;

                                // Count all players with a score for this stage.
                                scoresRef.count()
                                        .get(AggregateSource.SERVER)
                                        .addOnSuccessListener(totalSnapshot -> {

                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            long totalPlayers =
                                                    totalSnapshot.getCount();

                                            playerRankView.setText(
                                                    "YOUR RANK: #" + rank
                                                            + " / " + totalPlayers
                                            );
                                        })
                                        .addOnFailureListener(error -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            playerRankView.setText(
                                                    "YOUR RANK: #" + rank
                                            );
                                        });
                            })
                            .addOnFailureListener(error -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                playerRankView.setText(
                                        "Could not calculate your rank."
                                );

                                android.util.Log.e(
                                        "DailyBrain",
                                        "Failed to calculate player rank",
                                        error
                                );
                            });
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    playerRankView.setText(
                            "Could not load your personal best."
                    );

                    android.util.Log.e(
                            "DailyBrain",
                            "Failed to load player's score",
                            error
                    );
                });
    }

    private void loadLeaderboard() {
        addMessage("Loading leaderboard...");

        FirebaseFirestore.getInstance()
                .collection("stageLeaderboards")
                .document(String.valueOf(stageNumber))
                .collection("scores")
                .orderBy("bestTimeMs")
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    scoresContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        addMessage("No scores yet. Be the first!");
                        return;
                    }

                    int rank = 1;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String playerId = doc.getString("playerId");
                        String displayName = doc.getString("displayName");
                        Long timeMs = doc.getLong("bestTimeMs");

                        if (displayName == null) {
                            displayName = "Player";
                        }

                        if (timeMs == null) {
                            continue;
                        }

                        boolean isCurrentPlayer =
                                currentPlayerId != null
                                        && currentPlayerId.equals(playerId);

                        addScoreRow(
                                rank,
                                displayName,
                                timeMs,
                                isCurrentPlayer
                        );

                        rank++;
                    }
                })
                .addOnFailureListener(error -> {
                    scoresContainer.removeAllViews();
                    addMessage("Could not load scores. Please try again.");
                    android.util.Log.e(
                            "DailyBrain",
                            "Leaderboard loading failed",
                            error
                    );
                });
    }


    private void addScoreRow(
            int rank,
            String displayName,
            long timeMs,
            boolean isCurrentPlayer) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(14, 16, 14, 16);

        // Highlight the current player.
        if (isCurrentPlayer) {
            row.setBackgroundColor(Color.rgb(35, 65, 75));
        } else {
            row.setBackgroundColor(Color.rgb(25, 35, 52));
        }

        // Rank and medal.
        TextView rankView = new TextView(this);

        String rankLabel;

        if (rank == 1) {
            rankLabel = "🥇 #1";
        } else if (rank == 2) {
            rankLabel = "🥈 #2";
        } else if (rank == 3) {
            rankLabel = "🥉 #3";
        } else {
            rankLabel = "#" + rank;
        }

        rankView.setText(rankLabel);
        rankView.setTextColor(
                rank == 1 ? Color.rgb(255, 215, 0)
                        : rank == 2 ? Color.rgb(210, 210, 220)
                        : rank == 3 ? Color.rgb(205, 127, 50)
                        : Color.rgb(100, 181, 246)
        );
        rankView.setTypeface(null, Typeface.BOLD);
        rankView.setTextSize(15);

        // Player name.
        TextView nameView = new TextView(this);
        nameView.setText(
                displayName + (isCurrentPlayer ? " (YOU)" : "")
        );
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(15);
        nameView.setSingleLine(true);
        nameView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        // Best completion time.
        TextView timeView = new TextView(this);
        timeView.setText(formatTime(timeMs));
        timeView.setTextColor(Color.rgb(100, 220, 130));
        timeView.setTextSize(14);
        timeView.setTypeface(null, Typeface.BOLD);

        row.addView(
                rankView,
                new LinearLayout.LayoutParams(75, -2)
        );

        row.addView(
                nameView,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        row.addView(timeView);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = 6;

        scoresContainer.addView(row, params);
    }


    private void addMessage(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(Color.LTGRAY);
        textView.setTextSize(15);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(12, 28, 12, 28);
        scoresContainer.addView(textView);
    }

    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long centiseconds = (milliseconds % 1000) / 10;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d.%02d",
                minutes,
                seconds,
                centiseconds
        );
    }
}
