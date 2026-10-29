
package com.mssf.dailybrain;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import java.util.Locale;

public class DailyLeaderboardActivity extends AppCompatActivity {

    private LinearLayout leaderboardList;
    private TextView rankText;
    private TextView timeText;
    private TextView statusText;
    private LinearLayout podiumContainer;
    private String dateKey;
    private long officialTimeMs;
    private InterstitialAd dailyInterstitialAd;
    private boolean leaderboardDisplayed = false;
    private boolean shouldShowDailyInterstitial;
    private boolean dailyInterstitialHandled = false;
    private LinearLayout leaderboardRoot;
    private static final String DAILY_INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/1033173712";
    private final int backgroundColor =
            android.graphics.Color.rgb(16, 24, 39);
    private final int cardColor =
            android.graphics.Color.rgb(31, 41, 55);
    private final int white =
            android.graphics.Color.WHITE;
    private final int muted =
            android.graphics.Color.LTGRAY;
    private final int green =
            android.graphics.Color.rgb(110, 231, 183);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dateKey = getIntent().getStringExtra("date_key");

        officialTimeMs = getIntent().getLongExtra(
                "official_time_ms", 0L
        );

        shouldShowDailyInterstitial = getIntent().getBooleanExtra(
                "show_daily_interstitial", false
        );

        leaderboardRoot = new LinearLayout(this);
        LinearLayout root = leaderboardRoot;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(backgroundColor);

        TextView title = makeText("DAILY CHALLENGE", 26, white);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView date = makeText(
                dateKey == null ? "" : dateKey, 16, muted
        );
        date.setGravity(Gravity.CENTER);
        addSpace(root, 8);
        root.addView(date);


        rankText = makeText("YOUR RANK\nLoading...", 28, green);
        rankText.setGravity(Gravity.CENTER);
        rankText.setTypeface(null, android.graphics.Typeface.BOLD);
        rankText.setPadding(12, 20, 12, 20);
        rankText.setBackgroundColor(cardColor);

        addSpace(root, 20);
        root.addView(rankText);

        timeText = makeText(
                "Official time: " + formatDuration(officialTimeMs),
                18, white
        );
        timeText.setGravity(Gravity.CENTER);
        addSpace(root, 8);
        root.addView(timeText);

        statusText = makeText("Loading leaderboard...", 14, muted);
        statusText.setGravity(Gravity.CENTER);
        addSpace(root, 16);
        root.addView(statusText);
// Top 3 players podium
        podiumContainer = new LinearLayout(this);
        podiumContainer.setOrientation(LinearLayout.HORIZONTAL);
        podiumContainer.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams podiumParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        podiumParams.topMargin = 20;
        podiumParams.bottomMargin = 12;

        root.addView(podiumContainer, podiumParams);
        TextView heading = makeText("FULL RANKING", 20, white);
        addSpace(root, 16);
        root.addView(heading);

        ScrollView scrollView = new ScrollView(this);
        leaderboardList = new LinearLayout(this);
        leaderboardList.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(leaderboardList);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
                );
        scrollParams.topMargin = 12;
        root.addView(scrollView, scrollParams);


        TextView backButton = makeText("←  BACK", 16, green);
        backButton.setGravity(Gravity.CENTER);
        backButton.setTypeface(null, android.graphics.Typeface.BOLD);
        backButton.setPadding(16, 16, 16, 16);
        backButton.setBackgroundColor(cardColor);
        backButton.setOnClickListener(v -> finish());

        LinearLayout.LayoutParams backParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        backParams.topMargin = 12;
        root.addView(backButton, backParams);

        backButton.setGravity(Gravity.CENTER);
        backButton.setPadding(16, 16, 16, 16);
        backButton.setBackgroundColor(cardColor);
        backButton.setOnClickListener(v -> finish());

        addSpace(root, 12);

        if (shouldShowDailyInterstitial) {
            showLoadingScreen();
            loadDailyInterstitialAd();
        } else {
            setContentView(root);
            loadLeaderboard();
        }
    }
    private void loadDailyInterstitialAd() {
        InterstitialAd.load(
                new AdRequest.Builder(
                        DAILY_INTERSTITIAL_AD_UNIT_ID
                ).build(),
                new AdLoadCallback<InterstitialAd>() {

                    @Override
                    public void onAdLoaded(
                            @NonNull InterstitialAd ad
                    ) {
                        runOnUiThread(() -> {
                            dailyInterstitialAd = ad;

                            android.util.Log.d(
                                    "DailyBrainAds",
                                    "DAILY INTERSTITIAL LOADED"
                            );

                            maybeShowDailyInterstitial();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError error
                    ) {
                        runOnUiThread(() -> {
                            dailyInterstitialAd = null;

                            android.util.Log.e(
                                    "DailyBrainAds",
                                    "DAILY INTERSTITIAL FAILED: " + error.getMessage()
                            );

                            showLeaderboard();
                        });
                    }
                }
        );
    }
    private void maybeShowDailyInterstitial() {
        if (!shouldShowDailyInterstitial
                || dailyInterstitialHandled
                || dailyInterstitialAd == null
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        dailyInterstitialHandled = true;

        InterstitialAd ad = dailyInterstitialAd;
        dailyInterstitialAd = null;

        ad.setAdEventCallback(
                new InterstitialAdEventCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        runOnUiThread(() -> {
                            android.util.Log.d(
                                    "DailyBrainAds",
                                    "DAILY INTERSTITIAL DISMISSED"
                            );

                            showLeaderboard();
                        });
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(
                            @NonNull FullScreenContentError error
                    ) {
                        runOnUiThread(() -> {
                            android.util.Log.e(
                                    "DailyBrainAds",
                                    "DAILY INTERSTITIAL FAILED TO SHOW: "
                                            + error.getMessage()
                            );

                            showLeaderboard();
                        });
                    }
                }
        );

        ad.show(DailyLeaderboardActivity.this);
    }
    private void loadLeaderboard() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || dateKey == null
                || !dateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showLoadError("Could not identify today's challenge.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("dailyLeaderboards")
                .document(dateKey)
                .collection("scores")
                .orderBy("completionTimeMs", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    leaderboardList.removeAllViews();
                    podiumContainer.removeAllViews();
                    int myRank = -1;
                    int position = 1;

                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        Long scoreTime =
                                document.getLong("completionTimeMs");

                        String playerName =
                                document.getString("displayName");

                        if (scoreTime == null) {
                            continue;
                        }

                        if (playerName == null
                                || playerName.trim().isEmpty()) {
                            playerName = "Anonymous Player";
                        }

                        if (document.getId().equals(user.getUid())) {
                            myRank = position;
                            officialTimeMs = scoreTime;
                        }

                        addPlayerRow(
                                position,
                                playerName,
                                scoreTime,
                                document.getId().equals(user.getUid())
                        );

                        position++;
                    }
                    addPodium(snapshot.getDocuments());
                    if (myRank > 0) {
                        rankText.setText("YOUR RANK\n#" + myRank);
                        timeText.setText(
                                "Official time: "
                                        + formatDuration(officialTimeMs)
                        );
                    } else {
                        rankText.setText("YOUR RANK\nNot ranked yet");
                    }

                    statusText.setText(
                            snapshot.size() + " player(s) completed today's challenge"
                    );

                    if (snapshot.isEmpty()) {
                        statusText.setText(
                                "No scores have been saved for today yet."
                        );
                    }
                })
                .addOnFailureListener(exception -> {
                    android.util.Log.e(
                            "DailyBrain",
                            "Failed to load daily leaderboard",
                            exception
                    );
                    showLoadError(
                            "Could not load leaderboard. Check your connection and Firestore rules."
                    );
                });
    }

    private void addPlayerRow(
            int rank,
            String name,
            long timeMs,
            boolean isCurrentPlayer
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(14, 14, 14, 14);
        row.setBackgroundColor(
                isCurrentPlayer
                        ? android.graphics.Color.rgb(35, 68, 60)
                        : cardColor
        );

        row.setElevation(2f);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        rowParams.bottomMargin = 8;
        leaderboardList.addView(row, rowParams);

        int rankColor = white;

        if (rank == 1) {
            rankColor = android.graphics.Color.rgb(255, 204, 77);
        } else if (rank == 2) {
            rankColor = android.graphics.Color.rgb(190, 200, 215);
        } else if (rank == 3) {
            rankColor = android.graphics.Color.rgb(205, 127, 82);
        }

        if (isCurrentPlayer) {
            rankColor = green;
        }

        TextView rankView = makeText(
                "#" + rank,
                16,
                rankColor
        );

        rankView.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(rankView, new LinearLayout.LayoutParams(58,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView nameView = makeText(
                isCurrentPlayer ? name + " (You)" : name,
                14,
                isCurrentPlayer ? green : white
        );

        nameView.setMaxLines(1);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(nameView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        TextView timeView = makeText(
                formatDuration(timeMs),
                14,
                white
        );

        timeView.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(timeView);
    }

    private void showLoadError(String message) {
        rankText.setText("RANK UNAVAILABLE");
        statusText.setText(message);
    }

    private TextView makeText(
            String text, float size, int color
    ) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void addSpace(LinearLayout layout, int height) {
        TextView space = new TextView(this);
        space.setHeight(height);
        layout.addView(space);
    }

    private String formatDuration(long timeMs) {
        long minutes = timeMs / 60000;
        long seconds = (timeMs % 60000) / 1000;
        long hundredths = (timeMs % 1000) / 10;

        return String.format(
                Locale.US,
                "%02d:%02d.%02d",
                minutes,
                seconds,
                hundredths
        );
    }

    private void addPodium(
            java.util.List<DocumentSnapshot> documents
    ) {
        java.util.ArrayList<DocumentSnapshot> topPlayers =
                new java.util.ArrayList<>();

        for (DocumentSnapshot document : documents) {
            if (document.getLong("completionTimeMs") != null) {
                topPlayers.add(document);

                if (topPlayers.size() == 3) {
                    break;
                }
            }
        }

        if (topPlayers.isEmpty()) {
            return;
        }

        // Display second place, first place, third place.
        if (topPlayers.size() >= 2) {
            addPodiumCard(
                    topPlayers.get(1), 2,
                    android.graphics.Color.rgb(170, 180, 195)
            );
        }

        addPodiumCard(
                topPlayers.get(0), 1,
                android.graphics.Color.rgb(255, 204, 77)
        );

        if (topPlayers.size() >= 3) {
            addPodiumCard(
                    topPlayers.get(2), 3,
                    android.graphics.Color.rgb(205, 127, 82)
            );
        }
    }

    private void addPodiumCard(
            DocumentSnapshot document,
            int rank,
            int medalColor
    ) {
        String playerName = document.getString("displayName");

        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Anonymous";
        }

        Long timeMs = document.getLong("completionTimeMs");

        if (timeMs == null) {
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(6, 16, 6, 16);
        card.setBackgroundColor(cardColor);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        cardParams.setMargins(4, 0, 4, 0);

        podiumContainer.addView(card, cardParams);

        String medal;
        if (rank == 1) {
            medal = "GOLD";
        } else if (rank == 2) {
            medal = "SILVER";
        } else {
            medal = "BRONZE";
        }

        TextView medalView = makeText(
                medal, 12, medalColor
        );
        medalView.setGravity(Gravity.CENTER);
        card.addView(medalView);

        TextView rankView = makeText(
                "#" + rank, 24, medalColor
        );
        rankView.setGravity(Gravity.CENTER);
        rankView.setPadding(0, 6, 0, 6);
        card.addView(rankView);

        TextView nameView = makeText(
                playerName, 12, white
        );
        nameView.setGravity(Gravity.CENTER);
        nameView.setMaxLines(2);
        card.addView(nameView);

        TextView timeView = makeText(
                formatDuration(timeMs), 12, muted
        );
        timeView.setGravity(Gravity.CENTER);
        timeView.setPadding(0, 8, 0, 0);
        card.addView(timeView);
    }
    private void showLoadingScreen() {
        LinearLayout loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setPadding(32, 32, 32, 32);
        loadingLayout.setBackgroundColor(backgroundColor);

        TextView message = makeText(
                "Preparing your daily results...",
                18,
                white
        );

        message.setGravity(Gravity.CENTER);
        loadingLayout.addView(message);

        setContentView(loadingLayout);
    }
    private void showLeaderboard() {
        if (leaderboardDisplayed || isFinishing() || isDestroyed()) {
            return;
        }

        leaderboardDisplayed = true;
        setContentView(leaderboardRoot);
        loadLeaderboard();
    }
}
