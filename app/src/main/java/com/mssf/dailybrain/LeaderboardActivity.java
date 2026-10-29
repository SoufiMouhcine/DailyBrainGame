
package com.mssf.dailybrain;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.AggregateSource;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.Set;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import android.widget.Button;
import java.util.Locale;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;

public class LeaderboardActivity extends AppCompatActivity {
    private final Set<String> topPlayerIds = new HashSet<>();
    private long currentPlayerRank = -1;
    private LinearLayout scoresContainer;
    private TextView playerRankView;
    private LinearLayout personalScoreContainer;
    private String currentPlayerId;
    private int stageNumber;
    private InterstitialAd interstitialAd;

    private static final String INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/1033173712";
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
        root.setPadding(dp(20), dp(22), dp(20), dp(16));
        root.setBackgroundColor(Color.rgb(16, 24, 39));

// Header
        TextView title = new TextView(this);
        title.setText("STAGE " + stageNumber);
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("GLOBAL LEADERBOARD");
        subtitle.setTextColor(Color.rgb(100, 181, 246));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLetterSpacing(0.08f);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.bottomMargin = dp(22);
        root.addView(subtitle, subtitleParams);

// Player rank card
        LinearLayout rankCard = new LinearLayout(this);
        rankCard.setOrientation(LinearLayout.VERTICAL);
        rankCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        rankCard.setBackground(
                roundedBackground(Color.rgb(27, 42, 64), 18)
        );

        TextView rankLabel = new TextView(this);
        rankLabel.setText("YOUR OFFICIAL RANK");
        rankLabel.setTextColor(Color.rgb(155, 174, 199));
        rankLabel.setTextSize(12);
        rankLabel.setTypeface(null, Typeface.BOLD);
        rankCard.addView(rankLabel);

        playerRankView = new TextView(this);
        playerRankView.setText("Calculating your rank...");
        playerRankView.setTextColor(Color.WHITE);
        playerRankView.setTextSize(23);
        playerRankView.setTypeface(null, Typeface.BOLD);
        playerRankView.setPadding(0, dp(8), 0, 0);
        rankCard.addView(playerRankView);

        LinearLayout.LayoutParams rankParams =
                new LinearLayout.LayoutParams(-1, -2);
        rankParams.bottomMargin = dp(24);
        root.addView(rankCard, rankParams);

// Leaderboard section heading
        TextView listTitle = new TextView(this);
        listTitle.setText("FASTEST PLAYERS");
        listTitle.setTextColor(Color.WHITE);
        listTitle.setTextSize(17);
        listTitle.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams listTitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        listTitleParams.bottomMargin = dp(12);
        root.addView(listTitle, listTitleParams);

// Scrollable scores and personal result
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);

        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);

        scoresContainer = new LinearLayout(this);
        scoresContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.addView(scoresContainer);

        personalScoreContainer = new LinearLayout(this);
        personalScoreContainer.setOrientation(LinearLayout.VERTICAL);
        personalScoreContainer.setPadding(0, dp(12), 0, 0);
        contentContainer.addView(personalScoreContainer);

        scrollView.addView(contentContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

// Fixed bottom navigation
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(0, dp(14), 0, 0);

        Button continueButton = new Button(this);
        continueButton.setText("CONTINUE TO NEXT STAGE");
        continueButton.setAllCaps(false);
        continueButton.setTextColor(Color.rgb(16, 24, 39));
        continueButton.setTextSize(15);
        continueButton.setTypeface(null, Typeface.BOLD);
        continueButton.setBackground(
                roundedBackground(Color.rgb(100, 220, 130), 14)
        );
        continueButton.setElevation(dp(2));


        continueButton.setOnClickListener(v -> {

            int nextStage = stageNumber + 1;

            android.content.SharedPreferences preferences =
                    getSharedPreferences("game_progress", MODE_PRIVATE);

            int unlockedStage =
                    preferences.getInt("unlocked_stage", 1);

            // Keep the existing locked-stage protection.
            if (nextStage > unlockedStage) {
                nextStage = unlockedStage;
            }

            final int targetStage = nextStage;

            // Show an interstitial after every 5 completed stages.
            if (stageNumber % 5 == 0 && interstitialAd != null) {

                InterstitialAd ad = interstitialAd;
                interstitialAd = null;

                ad.setAdEventCallback(
                        new InterstitialAdEventCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                runOnUiThread(() ->
                                        openStage(targetStage)
                                );
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(
                                    @NonNull FullScreenContentError error) {

                                runOnUiThread(() ->
                                        openStage(targetStage)
                                );
                            }
                        }
                );

                ad.show(LeaderboardActivity.this);

            } else {
                // If no ad is ready, continue without delaying the player.
                openStage(targetStage);
            }
        });


        footer.addView(
                continueButton,
                new LinearLayout.LayoutParams(-1, dp(54))
        );

        Button allStagesButton = new Button(this);
        allStagesButton.setText("ALL STAGES");
        allStagesButton.setAllCaps(false);
        allStagesButton.setTextColor(Color.WHITE);
        allStagesButton.setTextSize(14);
        allStagesButton.setBackground(
                roundedBackground(Color.rgb(36, 51, 74), 14)
        );

        LinearLayout.LayoutParams allStagesParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        allStagesParams.topMargin = dp(8);
        footer.addView(allStagesButton, allStagesParams);

        allStagesButton.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            LeaderboardActivity.this,
                            StageSelectionActivity.class
                    );

            startActivity(intent);
            finish();
        });

        root.addView(
                footer,
                new LinearLayout.LayoutParams(-1, -2)
        );

        setContentView(root);
        loadInterstitialAd();
        loadLeaderboard();
        loadPlayerRank();
        loadPersonalScore();
    }

    private void openStage(int stage) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Intent intent = new Intent(
                LeaderboardActivity.this,
                MainActivity.class
        );

        intent.putExtra("stage_number", stage);

        startActivity(intent);
        finish();
    }

    private void loadInterstitialAd() {
        InterstitialAd.load(
                new AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build(),
                new AdLoadCallback<InterstitialAd>() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        runOnUiThread(() -> {
                            interstitialAd = ad;

                            android.util.Log.d(
                                    "DailyBrainAds",
                                    "LEADERBOARD INTERSTITIAL LOADED"
                            );
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        runOnUiThread(() -> {
                            interstitialAd = null;

                            android.util.Log.e(
                                    "DailyBrainAds",
                                    "LEADERBOARD INTERSTITIAL FAILED: "
                                            + adError.getMessage()
                            );
                        });
                    }
                }
        );
    }

    private void loadPersonalScore() {
        personalScoreContainer.removeAllViews();

        if (currentPlayerId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("stageLeaderboards")
                .document(String.valueOf(stageNumber))
                .collection("scores")
                .document(currentPlayerId)
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()
                            || !document.exists()) {
                        return;
                    }

                    Long timeMs = document.getLong("bestTimeMs");
                    String displayName = document.getString("displayName");

                    if (timeMs == null) {
                        return;
                    }

                    if (displayName == null) {
                        displayName = "Player";
                    }

                    TextView heading = new TextView(this);
                    heading.setText("YOUR PERSONAL RESULT");
                    heading.setTextColor(Color.rgb(100, 181, 246));
                    heading.setTextSize(14);
                    heading.setTypeface(null, Typeface.BOLD);
                    heading.setPadding(0, 8, 0, 12);

                    personalScoreContainer.addView(heading);

                    // The global rank is displayed in playerRankView.
                    // Show this separate row only when the player
                    // isn't already among the top 50.
                    int rank = 1;
                    for (int i = 0; i < scoresContainer.getChildCount(); i++) {
                        // The exact global rank is calculated separately.
                    }

                    if (!topPlayerIds.contains(currentPlayerId)) {
                        addPersonalScoreRow((int) currentPlayerRank,displayName, timeMs);
                    }
                })
                .addOnFailureListener(error ->
                        android.util.Log.e(
                                "DailyBrain",
                                "Failed to load personal score",
                                error
                        ));
    }

    private void addPersonalScoreRow(
            int rank,
            String displayName,
            long timeMs) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(14, 16, 14, 16);
        row.setBackgroundColor(Color.rgb(35, 65, 75));
        TextView rankView = new TextView(this);
        rankView.setText("#" + rank);
        rankView.setTextColor(Color.rgb(100, 181, 246));
        rankView.setTypeface(null, Typeface.BOLD);
        rankView.setTextSize(15);

        row.addView(
                rankView,
                new LinearLayout.LayoutParams(65, -2)
        );
        TextView nameView = new TextView(this);
        nameView.setText(displayName + " (YOU)");
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(15);
        nameView.setSingleLine(true);
        nameView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        TextView timeView = new TextView(this);
        timeView.setText(formatTime(timeMs));
        timeView.setTextColor(Color.rgb(100, 220, 130));
        timeView.setTextSize(14);
        timeView.setTypeface(null, Typeface.BOLD);

        row.addView(
                nameView,
                new LinearLayout.LayoutParams(0, -2, 1)
        );
        row.addView(timeView);

        personalScoreContainer.addView(row);
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
                                currentPlayerRank = rank;
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
                    topPlayerIds.clear();
                    if (snapshot.isEmpty()) {
                        addMessage("No scores yet. Be the first!");
                        return;
                    }

                    int rank = 1;

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String playerId = doc.getString("playerId");
                        if (playerId != null) {
                            topPlayerIds.add(playerId);
                        }
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

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable roundedBackground(
            int color,
            int radiusDp) {

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));

        return background;
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
