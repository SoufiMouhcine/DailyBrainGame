
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

    public static void saveBestTime(
            int stageNumber,
            long completionTimeMs,
            String displayName,
            LeaderboardCallback callback
    ) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError(
                    new IllegalStateException("Player is not signed in")
            );
            return;
        }

        if (stageNumber < 1 || completionTimeMs <= 0
                || displayName == null || displayName.trim().isEmpty()) {
            callback.onError(
                    new IllegalArgumentException("Invalid leaderboard data")
            );
            return;
        }

        String playerId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference scoreRef = db
                .collection("stageLeaderboards")
                .document(String.valueOf(stageNumber))
                .collection("scores")
                .document(playerId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot existing =
                    transaction.get(scoreRef);

            long previousBest = Long.MAX_VALUE;

            if (existing.exists()) {
                Long savedTime = existing.getLong("bestTimeMs");

                if (savedTime != null) {
                    previousBest = savedTime;
                }
            }

            if (existing.exists() && completionTimeMs >= previousBest) {
                return previousBest;
            }

            Map<String, Object> score = new HashMap<>();
            score.put("playerId", playerId);
            score.put("displayName", displayName.trim());
            score.put("stageNumber", stageNumber);
            score.put("bestTimeMs", completionTimeMs);
            score.put("completedAt", FieldValue.serverTimestamp());

            transaction.set(scoreRef, score);

            return completionTimeMs;
        }).addOnSuccessListener(savedBestTime -> {
            boolean newBest = savedBestTime == completionTimeMs;
            callback.onSaved(newBest, savedBestTime);
        }).addOnFailureListener(callback::onError);
    }
}
