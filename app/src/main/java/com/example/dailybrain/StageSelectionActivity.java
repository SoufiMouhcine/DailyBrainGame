
package com.example.dailybrain;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import androidx.appcompat.app.AppCompatActivity;

public class StageSelectionActivity extends AppCompatActivity {

    private GridLayout stageGrid;
    private int selectedWorld = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stage_selection);

        stageGrid = findViewById(R.id.stageGrid);

        Button world1Button = findViewById(R.id.world1Button);
        Button world2Button = findViewById(R.id.world2Button);
        Button world3Button = findViewById(R.id.world3Button);
        Button world4Button = findViewById(R.id.world4Button);

        world1Button.setOnClickListener(v -> selectWorld(1));
        world2Button.setOnClickListener(v -> selectWorld(2));
        world3Button.setOnClickListener(v -> selectWorld(3));
        world4Button.setOnClickListener(v -> selectWorld(4));

        Button dailyButton =
                findViewById(R.id.dailyChallengeButton);

        dailyButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    StageSelectionActivity.this,
                    DailyChallengeActivity.class
            );

            startActivity(intent);
        });

        createStageButtons();
        updateWorldButtons();
    }
    private void updateWorldTitle() {
        TextView worldTitleText = findViewById(R.id.worldTitleText);

        String[] worldNames = {
                "WORLD 1 — BEGINNER",
                "WORLD 2 — EXPLORER",
                "WORLD 3 — CHALLENGER",
                "WORLD 4 — MASTER"
        };

        worldTitleText.setText(worldNames[selectedWorld - 1]);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (stageGrid != null) {
            createStageButtons();
        }
    }

    private void addDailyChallengeButton() {

        LinearLayout parent =
                (LinearLayout) stageGrid.getParent();

        Button dailyButton = new Button(this);
        dailyButton.setText("DAILY CHALLENGE");
        dailyButton.setTextSize(16);

        dailyButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    StageSelectionActivity.this,
                    DailyChallengeActivity.class
            );

            startActivity(intent);
        });

        int index = parent.indexOfChild(stageGrid);

        parent.addView(
                dailyButton,
                index,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void createStageButtons() {

        stageGrid.removeAllViews();

        android.content.SharedPreferences preferences =
                getSharedPreferences("game_progress", MODE_PRIVATE);

        int unlockedStage =
                preferences.getInt("unlocked_stage", 1);
        Set<String> completedStages = new HashSet<>(
                preferences.getStringSet("completed_stages", new HashSet<>())
        );

        int firstStage = selectedWorld == 1 ? 1
                : selectedWorld == 2 ? 11
                : selectedWorld == 3 ? 21 : 36;

        int lastStage = selectedWorld == 1 ? 10
                : selectedWorld == 2 ? 20
                : selectedWorld == 3 ? 35 : 50;

        for (int stage = firstStage; stage <= lastStage; stage++) {


            Button button = new Button(this);

            button.setTextSize(12);
            button.setAllCaps(false);
            button.setSingleLine(false);
            button.setPadding(2, 4, 2, 4);

            if (stage <= unlockedStage) {

                button.setEnabled(true);

                if (completedStages.contains(String.valueOf(stage))) {

                    button.setText("✓\nSTAGE " + stage + "\nCOMPLETED");
                    button.setTextColor(Color.rgb(110, 231, 183));

                    button.setBackgroundResource(R.drawable.stage_completed);
                    button.setTextColor(Color.rgb(110, 231, 183));

                } else if (stage == unlockedStage) {

                    button.setText("★\nSTAGE " + stage + "\nNEXT");
                    button.setTextColor(Color.rgb(16, 24, 39));

                    button.setBackgroundResource(R.drawable.stage_available);
                    button.setTextColor(Color.WHITE);

                } else {

                    button.setText("▶\nSTAGE " + stage + "\n"
                            + getDifficultyLabel(stage));
                    button.setTextColor(Color.WHITE);

                    button.setBackgroundResource(R.drawable.stage_next);
                    button.setTextColor(Color.rgb(16, 24, 39));
                }

                final int selectedStage = stage;

                button.setOnClickListener(v ->
                        openStage(selectedStage));

            } else {
                button.setEnabled(false);
                button.setText("🔒\nSTAGE " + stage + "\nLOCKED");
                button.setTextColor(Color.LTGRAY);

                button.setBackgroundResource(R.drawable.stage_locked);
                button.setTextColor(Color.rgb(132, 149, 169));
            }


            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = (int) (105 * getResources()
                    .getDisplayMetrics().density);

            params.columnSpec =
                    GridLayout.spec(GridLayout.UNDEFINED, 1f);

            params.setMargins(4, 4, 4, 4);

            button.setLayoutParams(params);

            stageGrid.addView(button);
        }
        updateStageProgress();
    }

    private String getDifficultyLabel(int stage) {
        if (stage <= 5) {
            return "EASY";
        } else if (stage <= 10) {
            return "NORMAL";
        } else if (stage <= 20) {
            return "HARD";
        } else if (stage <= 35) {
            return "EXPERT";
        } else {
            return "MASTER";
        }
    }

    private void openStage(int stageNumber) {

        Intent intent = new Intent(
                StageSelectionActivity.this,
                MainActivity.class
        );

        intent.putExtra("stage_number", stageNumber);

        startActivity(intent);
    }

    private void selectWorld(int world) {
        selectedWorld = world;

        updateWorldButtons();
        updateWorldTitle();
        createStageButtons();
    }

    private void updateWorldButtons() {
        int activeColor = Color.rgb(100, 220, 130);
        int inactiveColor = Color.rgb(38, 53, 72);

        int white = Color.WHITE;
        int dark = Color.rgb(16, 24, 39);

        int[] buttonIds = {
                R.id.world1Button,
                R.id.world2Button,
                R.id.world3Button,
                R.id.world4Button
        };

        for (int i = 0; i < buttonIds.length; i++) {
            Button button = findViewById(buttonIds[i]);

            boolean selected = (i + 1) == selectedWorld;

            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            selected ? activeColor : inactiveColor
                    )
            );

            button.setTextColor(selected ? dark : white);
        }
    }

    private void updateStageProgress() {
        android.content.SharedPreferences preferences =
                getSharedPreferences("game_progress", MODE_PRIVATE);

        java.util.Set<String> completedStages =
                preferences.getStringSet(
                        "completed_stages",
                        java.util.Collections.emptySet()
                );

        int completedCount = completedStages.size();

        TextView progressText =
                findViewById(R.id.stageProgressText);

        ProgressBar progressBar =
                findViewById(R.id.stageProgressBar);

        progressText.setText(
                completedCount + " / 50 STAGES COMPLETED"
        );

        progressBar.setProgress(completedCount);
    }

}
