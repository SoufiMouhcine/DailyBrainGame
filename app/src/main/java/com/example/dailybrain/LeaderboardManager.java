
package com.example.dailybrain;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardManager {

    public interface LeaderboardCallback {
        void onSaved(boolean newBest, long bestTimeMs);
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

    public static void saveBestTime(
            int stageNumber,
            long completionTimeMs,
            String displayName,
            LeaderboardCallback callback) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError(
                    new IllegalStateException("Player is not signed in")
            );
            return;
        }

        if (stageNumber < 1 || completionTimeMs <= 0
                || displayName == null
                || displayName.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("Invalid leaderboard data")
            );
            return;
        }

        String playerId = user.getUid();

        DocumentReference scoreRef = FirebaseFirestore.getInstance()
                .collection("stageLeaderboards")
                .document(String.valueOf(stageNumber))
                .collection("scores")
                .document(playerId);

        FirebaseFirestore.getInstance()
                .runTransaction(transaction -> {

                    com.google.firebase.firestore.DocumentSnapshot existing =
                            transaction.get(scoreRef);

                    // This player already has an official result.
                    // Never overwrite it, even with a faster time.
                    if (existing.exists()) {
                        Long officialTime =
                                existing.getLong("bestTimeMs");

                        if (officialTime == null) {
                            throw new IllegalStateException(
                                    "Existing official score is invalid"
                            );
                        }

                        return new SaveResult(false, officialTime);
                    }

                    // First official completion for this stage.
                    Map<String, Object> score = new HashMap<>();
                    score.put("playerId", playerId);
                    score.put("displayName", displayName.trim());
                    score.put("stageNumber", stageNumber);
                    score.put("bestTimeMs", completionTimeMs);
                    score.put(
                            "completedAt",
                            FieldValue.serverTimestamp()
                    );

                    transaction.set(scoreRef, score);

                    return new SaveResult(true, completionTimeMs);

                })
                .addOnSuccessListener(result ->
                        callback.onSaved(
                                result.firstCompletion,
                                result.officialTimeMs
                        ))
                .addOnFailureListener(callback::onError);
    }
}
