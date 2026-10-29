
package com.mssf.dailybrain;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {

    public interface PlayerCallback {
        void onReady(String playerId, String displayName);
        void onError(Exception exception);
    }

    public static void createOrLoadPlayer(PlayerCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError(
                    new IllegalStateException("Player is not signed in")
            );
            return;
        }

        String playerId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("players")
                .document(playerId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String displayName = document.getString("displayName");

                        if (displayName != null && !displayName.isEmpty()) {
                            callback.onReady(playerId, displayName);
                        } else {
                            callback.onError(
                                    new IllegalStateException(
                                            "Player profile has no display name"
                                    )
                            );
                        }
                        return;
                    }

                    String displayName =
                            "Player-" + playerId.substring(0, 4).toUpperCase();

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("displayName", displayName);
                    profile.put(
                            "createdAt",
                            com.google.firebase.firestore.FieldValue.serverTimestamp()
                    );

                    db.collection("players")
                            .document(playerId)
                            .set(profile)
                            .addOnSuccessListener(unused ->
                                    callback.onReady(playerId, displayName))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}
