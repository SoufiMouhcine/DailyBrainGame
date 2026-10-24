
package com.example.dailybrain;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DailyChallengeManager {

    private DailyChallengeManager() {
        // Utility class
    }

    public static String getTodayDateKey() {

        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // One consistent daily reset for all players.
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(new Date());
    }

    public static Puzzle generateTodayPuzzle() {

        String dateKey = getTodayDateKey();

        return PuzzleGenerator.generateDailyPuzzle(dateKey);
    }
}
