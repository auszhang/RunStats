package com.example.runstats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

import static java.lang.String.valueOf;

public class MainActivity extends AppCompatActivity {


    private boolean started;
    private boolean isRunning;
    private long timeSoFar;
    private double currentDistance;
    private double currentDistanceMiles;
    private double averageSpeed;
    private int totalMillisRan;
    private Time totalTimeRan;
    private ArrayList<Location> locations;
    private ArrayList<LatLng> latLngs;

    private LocationManager locationManager;
    private LocationListener listener;

    // Views
    Button startBtn;
    Button pauseResumeBtn;
    Button finishBtn;
    Chronometer runTimer;
    TextView distanceValue;
    TextView avgSpeedValue;

    PowerManager mgr;
    PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiate variables
        started = false;
        isRunning = false;
        timeSoFar = 0;
        currentDistance = 0;
        currentDistanceMiles = 0;
        totalMillisRan = 0;
        totalTimeRan = new Time(0,0,0);
        locations = new ArrayList();
        latLngs = new ArrayList();

        // instantiate views
        startBtn = (Button) findViewById(R.id.startButton);
        pauseResumeBtn = (Button) findViewById(R.id.startPauseButton);
        finishBtn = (Button) findViewById(R.id.finishButton);
        runTimer = (Chronometer) findViewById(R.id.runTimer);
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        avgSpeedValue = (TextView) findViewById(R.id.avgSpeedValue);


        // ask for location permissions
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // disable the pause/resume button and the finish button
        pauseResumeBtn.setEnabled(false);
        finishBtn.setEnabled(false);
    }

    public void startRun(View view) {
        started = true;
        isRunning = true;
        Toast.makeText(getBaseContext(), "Starting Run", Toast.LENGTH_LONG).show();

        // start the timer
        runTimer.setBase(SystemClock.elapsedRealtime() - timeSoFar);
        runTimer.start();

        // start tracking distance
        trackDistance();

        // disable start button and enable pause/resume button + finish button
        startBtn.setEnabled(false);
        pauseResumeBtn.setEnabled(true);
        finishBtn.setEnabled(true);

        // allow app to run when phone is locked
        mgr = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RunStats:MyWakeLock");
        wakeLock.acquire();
    }

    // continuously updates distance to display while running
    public void trackDistance() {
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locations.add(location);

                // update distance traveled whenever moved
                computeDistance();
                computeAverageSpeed();
                distanceValue.setText(String.format("%.2f", currentDistanceMiles));
                avgSpeedValue.setText(String.format("%.2f", averageSpeed));
            }
        };

        // request location permissions here?
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, listener);
        }

        // TODO: the app can now tell when the distance has changed. convert this info to distance traveled
        // https://stackoverflow.com/questions/41719550/android-calculate-distance-traveled
    }

    public void toggleTimer(View view) {
        if(started) {
            if (isRunning) {
                timeSoFar = SystemClock.elapsedRealtime() - runTimer.getBase();
                runTimer.stop();
                isRunning = false;
            } else {
                runTimer.setBase(SystemClock.elapsedRealtime() - timeSoFar);
                runTimer.start();
                isRunning = true;
            }
        }

    }

    public void finishRun(View view) {
        // when finished, record the results of the workout (in a db?): save distance traveled (stop recording distance), time exercised (stop timer)

        // reset button states
        startBtn.setEnabled(true);
        pauseResumeBtn.setEnabled(false);
        finishBtn.setEnabled(false);

        // resets the timer and spits out the duration of the run. TODO: save run duration in a database?
        if(isRunning) {
            totalMillisRan = (int) (SystemClock.elapsedRealtime() - runTimer.getBase());
        } else {
            totalMillisRan = (int) (timeSoFar);
        }

        //runTimer.setBase(SystemClock.elapsedRealtime());
        runTimer.stop();

        totalTimeRan = millisToTime(totalMillisRan);

         Toast.makeText(getBaseContext(), "Run finished. You ran for "
                 + totalTimeRan.stringifyTime() + "!", Toast.LENGTH_LONG).show();

        computeDistance();
        computeAverageSpeed();

        // reset state so user can begin a new run any time
        isRunning = false;
        started = false;
        timeSoFar = 0;
        currentDistance = 0;
        currentDistanceMiles = 0;
        averageSpeed = 0;

        locations.clear();
        latLngs.clear();
        wakeLock.release();

        // stop the listener
        locationManager.removeUpdates(listener);
    }



    // updates the current distance traveled for the currently existing values in locations
    public void computeDistance() {
        for(Location loc : locations) {
            latLngs.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }
        currentDistance = SphericalUtil.computeLength(latLngs);
        currentDistanceMiles = currentDistance/1609;
    }

    public void computeAverageSpeed() {
        averageSpeed = currentDistanceMiles/totalMillisRan*3600000;
    }

    public Time millisToTime(int timeInMillis) {
        Time result;
        int hours = 0, minutes = 0, seconds = 0;

        if(timeInMillis > 3600000) {
            hours = timeInMillis / 3600000;
            timeInMillis = timeInMillis - (hours * 3600000);
        }
        if(timeInMillis > 60000) {
            minutes = timeInMillis / 60000;
            timeInMillis = timeInMillis - (minutes * 60000);
        }
        if(timeInMillis > 1000) {
            seconds = timeInMillis / 1000;
            timeInMillis = timeInMillis - (seconds * 1000);
        }

        result = new Time(hours, minutes, seconds);
        return result;
    }
}