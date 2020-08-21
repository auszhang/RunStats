package com.example.runstats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.String.valueOf;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private boolean started;
    private long timeSoFar; // used when calculating stops

    private double currentDistanceMiles;
    private double averageSpeed;
    private int currentMillisRan;
    private int totalMillisRan;
    private Time totalTimeRan;

    private LocationManager locationManager;

    // LocationService
    public LocationService locationService;
    private boolean bound = false;

    // Views
    Button startBtn;
    Button pauseResumeBtn;
    Button finishBtn;
    Chronometer runTimer;
    TextView distanceValue;
    TextView avgSpeedValue;

    PowerManager mgr;
    PowerManager.WakeLock wakeLock;


    /** OVERRIDED METHODS **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiate variables
        started = false;
        timeSoFar = 0;
        currentDistanceMiles = 0;
        totalMillisRan = 0;
        totalTimeRan = new Time(0,0,0);
        averageSpeed = 0;
        currentMillisRan = 0;

        // instantiate views
        startBtn = (Button) findViewById(R.id.startButton);
        pauseResumeBtn = (Button) findViewById(R.id.startPauseButton);
        finishBtn = (Button) findViewById(R.id.finishButton);
        runTimer = (Chronometer) findViewById(R.id.runTimer);
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        avgSpeedValue = (TextView) findViewById(R.id.avgSpeedValue);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // start LocationService
        final Intent serviceStart = new Intent(this.getApplication(), LocationService.class);
        this.getApplication().startService(serviceStart);
        this.getApplication().bindService(serviceStart, serviceConnection, Context.BIND_AUTO_CREATE);

        // disable the pause/resume button and the finish button
        pauseResumeBtn.setEnabled(false);
        finishBtn.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // bind to Service
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // unbind from Service
        if (bound) {
            locationService.setCallbacks(null); // unregister
            unbindService(serviceConnection);
            bound = false;
        }
    }

    // Callbacks for service binding, passed to bindService()
    // listener of binding events between MainActivity and LocationService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            String name = componentName.getClassName();

            if(name.endsWith("LocationService")) {
                locationService = ((LocationService.LocationServiceBinder) iBinder).getService();
                locationService.startUpdatingLocation();
            }

            // cast the IBinder and get LocationService instance
            LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) iBinder;
            locationService = binder.getService();
            bound = true;
            locationService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(componentName.getClassName().equals("LocationService")) {
                locationService = null;
            }
            bound = false;
        }
    };

    /* Defined by ServiceCallbacks interface */
    @Override
    public void trackDistance() {
        computeDistance();
        computeAverageSpeed();
        distanceValue.setText(String.format("%.2f", currentDistanceMiles));
        avgSpeedValue.setText(String.format("%.2f", averageSpeed));
    }


    /** BUTTON FUNCTIONS **/

    public void startRun(View view) {
        started = true;
        timeSoFar = 0;
        currentDistanceMiles = 0;
        totalMillisRan = 0;
        totalTimeRan = new Time(0,0,0);        averageSpeed = 0;
        currentMillisRan = 0;
        this.locationService.startRunning();
        Toast.makeText(getBaseContext(), "Starting Run", Toast.LENGTH_LONG).show();

        // start the timer
        runTimer.setBase(SystemClock.elapsedRealtime() - timeSoFar);
        runTimer.start();

        // start tracking distance HERE
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

    public void toggleTimer(View view) {
        if(started) {
            if (this.locationService.isRunning()) {
                timeSoFar = SystemClock.elapsedRealtime() - runTimer.getBase();
                runTimer.stop();
                this.locationService.stopRunning();
            } else {
                runTimer.setBase(SystemClock.elapsedRealtime() - timeSoFar);
                runTimer.start();
                this.locationService.startRunning();
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
        totalMillisRan = getCurrentTimeRan();

        //runTimer.setBase(SystemClock.elapsedRealtime());
        runTimer.stop();

        totalTimeRan = millisToTime(totalMillisRan);

         Toast.makeText(getBaseContext(), "Run finished. You ran for "
                 + totalTimeRan.stringifyTime() + "!", Toast.LENGTH_LONG).show();

//        computeDistance();
//        computeAverageSpeed();

        // reset state so user can begin a new run any time
        started = false;
        timeSoFar = 0;
        currentDistanceMiles = 0;
        totalMillisRan = 0;
        averageSpeed = 0;
        currentMillisRan = 0;

        // reset state so user can begin a new run any time
        this.locationService.stopRunning();
        this.locationService.clearArrayLists();
        wakeLock.release();
    }


    /** UTILITY FUNCTIONS **/

    // gets current time ran in millis
    public int getCurrentTimeRan() {
        if(this.locationService.isRunning()) {
            return (int) (SystemClock.elapsedRealtime() - runTimer.getBase());
        } else {
            return (int) (timeSoFar);
        }
    }

    // updates the current distance traveled for the currently existing values in locations
    public void computeDistance() {
        currentDistanceMiles = this.locationService.getCurrentDistanceMiles();
    }

    public void computeAverageSpeed() {
        currentMillisRan = getCurrentTimeRan();
        averageSpeed = currentDistanceMiles/currentMillisRan*3600000;
        Log.i("AVGSPEEDDEBUG", "averageSpeed: " + valueOf(averageSpeed));
        Log.i("AVGSPEEDDEBUG", "currentDistanceMiles: " + valueOf(currentDistanceMiles));
        Log.i("AVGSPEEDDEBUG", "currentMillisRan: " + valueOf(currentMillisRan));
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