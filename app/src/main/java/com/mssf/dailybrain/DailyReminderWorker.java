package com.mssf.dailybrain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DailyReminderWorker extends ListenableWorker {

    private static final String TAG = "DailyBrainReminder";

    public DailyReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {

        return CallbackToFutureAdapter.getFuture(completer -> {

            Context context = getApplicationContext();
            String today = DailyChallengeManager.getTodayDateKey();

            SharedPreferences preferences =
                    context.getSharedPreferences(
                            "daily_reminder",
                            Context.MODE_PRIVATE
                    );

            String completedDate = preferences.getString(
                    "completed_date",
                    ""
            );

            if (today.equals(completedDate)) {
                completer.set(Result.success());
                return "daily-reminder-already-completed";
            }

            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                completer.set(Result.retry());
                return "daily-reminder-no-user";
            }

            FirebaseFirestore.getInstance()
                    .collection("dailyLeaderboards")
                    .document(today)
                    .collection("scores")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {

                        if (document.exists()) {
                            Log.d(TAG, "Today's challenge is completed.");
                        } else {
                            Log.d(TAG, "Today's challenge is not completed.");
                            DailyReminderNotification.show(context);
                        }

                        completer.set(Result.success());
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Firebase completion check failed.", exception);
                        completer.set(Result.retry());
                    });

            return "daily-reminder-firebase-check";
        });
    }
}