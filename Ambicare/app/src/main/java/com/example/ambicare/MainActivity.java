package com.example.ambicare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 300; // in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start your OnboardingActivity
                startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
                finish(); // close the current activity
            }
        }, SPLASH_DURATION);
    }
}
