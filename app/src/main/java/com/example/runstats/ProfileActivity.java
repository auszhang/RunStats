package com.example.runstats;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        // set ProfileActivity to be selected by default
        bottomNavigationView.setSelectedItemId(R.id.profile);

        // set bottom nav listener
        bottomNavigationView.setOnNavigationItemSelectedListener((item) -> {
            switch(item.getItemId()) {
                case R.id.log:
                    Intent intentLog = new Intent(ProfileActivity.this, LogActivity.class);
                    intentLog.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intentLog);
                    break;
                case R.id.run:
                    Intent intentMain = new Intent(ProfileActivity.this, MainActivity.class);
                    intentMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intentMain);
                    break;
                case R.id.profile:
                    // current activity, do nothing
                    break;
            }
            return false;
        });
    }
}