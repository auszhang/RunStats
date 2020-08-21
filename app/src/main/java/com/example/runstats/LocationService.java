package com.example.runstats;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class LocationService extends Service implements LocationListener, GpsStatus.Listener {

    public static final String LOG_TAG = LocationService.class.getSimpleName();
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private ServiceCallbacks serviceCallbacks;

    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private boolean isRunning = false;
    private double currentDistanceMiles;

    public LocationService() {
    }

    // Binder class
    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }


    /** OVERRIDED METHODS **/

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    @Override
    public void onProviderDisabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER)) {
            notifyLocationProviderStatusUpdated(false);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            notifyLocationProviderStatusUpdated(true);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                notifyLocationProviderStatusUpdated(false);
            } else {
                notifyLocationProviderStatusUpdated(true);
            }
        }
    }

    @Override
    public void onLocationChanged(final Location newLocation) {
        Log.d(LOG_TAG, "(" + newLocation.getLatitude() + "," + newLocation.getLongitude() + ")");

        if(isRunning) {
            locations.add(newLocation);
        }
        computeDistance();

        // updates distance changes in UI
        if(serviceCallbacks != null && isRunning) {
            serviceCallbacks.trackDistance();
        }

        Intent intent = new Intent("LocationUpdated");
        intent.putExtra("location", newLocation);

        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
    }

    /** LOCATION FUNCTIONS **/

    public void computeDistance() {
        for(Location loc : locations) {
            latLngs.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }
        currentDistanceMiles = SphericalUtil.computeLength(latLngs)/1609;
    }

    public double getCurrentDistanceMiles() {
        return currentDistanceMiles;
    }

    public void onGpsStatusChanged(int event) {

    }

    private void notifyLocationProviderStatusUpdated(boolean isLocationProviderAvailable) {
        //Broadcast location provider status change here
        if(isLocationProviderAvailable) {
            Log.d(LOG_TAG, "Location provider now AVAILABLE.");
        }
        else {
            Log.d(LOG_TAG, "Location provider now UNAVAILABLE.");
        }
    }

    public void startUpdatingLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //Exception thrown when GPS or Network provider were not available on the user's device.
        try {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAltitudeRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setBearingRequired(false);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

            Integer gpsMinTime = 1000; // milliseconds
            Integer gpsMinDistance = 1; // meters

            locationManager.addGpsStatusListener(this);
            locationManager.requestLocationUpdates(gpsMinTime, gpsMinDistance, criteria, this, null);

        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
        }
    }


    /** UTILITY FUNCTIONS **/

    public void clearArrayLists() {
        locations.clear();
        latLngs.clear();
    }

    public void startRunning() {
        isRunning = true;
    }

    public void stopRunning() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
