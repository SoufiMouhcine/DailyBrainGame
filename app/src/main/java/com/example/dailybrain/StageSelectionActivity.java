package com.example.dailybrain;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

public class StageSelectionActivity extends AppCompatActivity {

    private GridLayout stageGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stage_selection);

        stageGrid = findViewById(R.id.stageGrid);

        createStageButtons();
    }
    @Override
    protected void onResume() {
        super.onResume();
        createStageButtons();
    }
    private void createStageButtons() {
        stageGrid.removeAllViews();
        android.content.SharedPreferences preferences =
                getSharedPreferences("game_progress", MODE_PRIVATE);

        int unlockedStage =
                preferences.getInt("unlocked_stage", 1);

        for (int stage = 1; stage <= 10; stage++) {

            Button button = new Button(this);

            button.setText(String.valueOf(stage));
            button.setTextSize(18);

            if (stage <= unlockedStage) {

                button.setEnabled(true);

                final int selectedStage = stage;

                button.setOnClickListener(v -> {
                    openStage(selectedStage);
                });

            } else {

                button.setEnabled(false);
            }

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = 120;

            params.columnSpec =
                    GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                    );

            params.setMargins(8, 8, 8, 8);

            button.setLayoutParams(params);

            stageGrid.addView(button);
        }
    }

    private void unlockNextStage(int completedStage) {

        android.content.SharedPreferences preferences =
                getSharedPreferences("game_progress", MODE_PRIVATE);

        int unlockedStage =
                preferences.getInt("unlocked_stage", 1);

        if (completedStage >= unlockedStage) {

            preferences.edit()
                    .putInt(
                            "unlocked_stage",
                            completedStage + 1
                    )
                    .apply();
        }
    }

    private void openStage(int stageNumber) {

        android.content.Intent intent =
                new android.content.Intent(
                        StageSelectionActivity.this,
                        MainActivity.class
                );

        intent.putExtra("stage_number", stageNumber);

        startActivity(intent);
    }
}