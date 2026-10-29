package com.mssf.dailybrain;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class DailyReminderScheduler {

    private static final String WORK_NAME = "daily_brain_reminder";

    private DailyReminderScheduler() {
    }

    public static void scheduleDailyReminder(Context context) {

        Calendar now = Calendar.getInstance();

        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.HOUR_OF_DAY, 10);
        nextRun.set(Calendar.MINUTE, 0);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);

        // If today's 10 AM has passed, schedule for tomorrow.
        if (!nextRun.after(now)) {
            nextRun.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelay =
                nextRun.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest reminderWork =
                new PeriodicWorkRequest.Builder(
                        DailyReminderWorker.class,
                        24,
                        TimeUnit.HOURS
                )
                        .setInitialDelay(
                                initialDelay,
                                TimeUnit.MILLISECONDS
                        )
                        .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        reminderWork
                );
    }
}