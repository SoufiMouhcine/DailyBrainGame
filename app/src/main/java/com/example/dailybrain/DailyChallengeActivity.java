
package com.example.dailybrain;

import android.os.Bundle;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class DailyChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(
                DailyChallengeActivity.this,
                MainActivity.class
        );

        intent.putExtra("daily_challenge", true);

        startActivity(intent);
        finish();
    }
}
