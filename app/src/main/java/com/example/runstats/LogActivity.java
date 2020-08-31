package com.example.runstats;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LogActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        // set LogActivity to be selected by default
        bottomNavigationView.setSelectedItemId(R.id.log);

        // set bottom nav listener
        bottomNavigationView.setOnNavigationItemSelectedListener((item) -> {
            switch(item.getItemId()) {
                case R.id.log:
                    // current activity, do nothing
                    break;
                case R.id.run:
                    Intent intentMain = new Intent(LogActivity.this, MainActivity.class);
                    intentMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intentMain);
                    break;
                case R.id.profile:
                    Intent intentProfile = new Intent(LogActivity.this, ProfileActivity.class);
                    intentProfile.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intentProfile);
                    break;
            }
            return false;
        });
    }
}