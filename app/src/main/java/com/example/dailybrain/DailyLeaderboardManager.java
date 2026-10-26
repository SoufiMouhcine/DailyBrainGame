package com.example.dailybrain;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;

public class DailyLeaderboardManager {

    public interface DailyLeaderboardCallback {
        void onSaved(boolean firstCompletion, long officialTimeMs);

        void onError(Exception exception);
    }

    private static class SaveResult {
        final boolean firstCompletion;
        final long officialTimeMs;

        SaveResult(boolean firstCompletion, long officialTimeMs) {
            this.firstCompletion = firstCompletion;
            this.officialTimeMs = officialTimeMs;
        }
    }

    private DailyLeaderboardManager() {
        // Utility class.
    }

    public static void saveDailyTime(
            String dateKey,
            long completionTimeMs,
            String displayName,
            DailyLeaderboardCallback callback) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError(
                    new IllegalStateException("Player is not signed in")
            );
            return;
        }

        if (dateKey == null
                || !dateKey.matches("\\d{4}-\\d{2}-\\d{2}")
                || completionTimeMs <= 0
                || displayName == null
                || displayName.trim().isEmpty()) {

            callback.onError(
                    new IllegalArgumentException(
                            "Invalid daily leaderboard data"
                    )
            );
            return;
        }

        String playerId = user.getUid();

        DocumentReference scoreRef = FirebaseFirestore.getInstance()
                .collection("dailyLeaderboards")
                .document(dateKey)
                .collection("scores")
                .document(playerId);

        FirebaseFirestore.getInstance()
                .runTransaction(transaction -> {

                    DocumentSnapshot existing =
                            transaction.get(scoreRef);

                    // One official result per player per date.
                    // Never overwrite the original result.
                    if (existing.exists()) {

                        Long officialTime =
                                existing.getLong("completionTimeMs");

                        if (officialTime == null) {
                            throw new IllegalStateException(
                                    "Existing daily score is invalid"
                            );
                        }

                        return new SaveResult(false, officialTime);
                    }

                    Map<String, Object> score = new HashMap<>();

                    score.put("playerId", playerId);
                    score.put("displayName", displayName.trim());
                    score.put("dateKey", dateKey);
                    score.put("completionTimeMs", completionTimeMs);
                    score.put("completedAt", FieldValue.serverTimestamp());

                    transaction.set(scoreRef, score);

                    return new SaveResult(true, completionTimeMs);

                })
                .addOnSuccessListener(result ->
                        callback.onSaved(
                                result.firstCompletion,
                                result.officialTimeMs
                        )
                )
                .addOnFailureListener(callback::onError);
    }
    public interface DailyRankCallback {
        void onRankFound(int rank, long officialTimeMs);

        void onNoResult();

        void onError(Exception exception);
    }

    public static void getDailyRank(
            String dateKey,
            DailyRankCallback callback) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError(
                    new IllegalStateException("Player is not signed in")
            );
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("dailyLeaderboards")
                .document(dateKey)
                .collection("scores")
                .orderBy("completionTimeMs", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    int rank = 1;
                    String playerId = user.getUid();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        if (document.getId().equals(playerId)) {

                            Long officialTime =
                                    document.getLong("completionTimeMs");

                            if (officialTime == null) {
                                callback.onError(
                                        new IllegalStateException(
                                                "Daily score is invalid"
                                        )
                                );
                                return;
                            }

                            callback.onRankFound(rank, officialTime);
                            return;
                        }

                        rank++;
                    }

                    callback.onNoResult();
                })
                .addOnFailureListener(callback::onError);
    }
}