
package com.mssf.dailybrain;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

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

            Log.d(TAG, "Daily reminder worker started.");

            try {
                DailyReminderNotification.show(context);

                Log.d(TAG, "Daily reminder notification requested.");

                completer.set(Result.success());

            } catch (Exception exception) {

                Log.e(TAG, "Failed to show daily reminder.", exception);

                completer.set(Result.retry());
            }

            return "daily-reminder-notification";
        });
    }
}
