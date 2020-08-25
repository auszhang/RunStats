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

import static java.lang.String.valueOf;

public class LocationService extends Service implements LocationListener, GpsStatus.Listener {

    public static final String LOG_TAG = LocationService.class.getSimpleName();
    public static final double NOISE_THRESHOLD = 0.0007; // in miles; 1 meters
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private ServiceCallbacks serviceCallbacks;

    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private boolean isRunning = false;
    private boolean finishedRunning = true;
    private double currentDistanceMiles = 0;
    private Location currentLocation;

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

        if(currentLocation != null) {
            updateDistance(newLocation);
        }

        // updates distance changes in UI
        if(serviceCallbacks != null && isRunning) {
            serviceCallbacks.trackDistance();
        }

        Intent intent = new Intent("LocationUpdated");
        intent.putExtra("location", newLocation);

        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        currentLocation = newLocation;
    }

    /** LOCATION FUNCTIONS **/

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in MILES
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance)/1609.0;
    }

    // update currentDistanceMiles using old value of currentDistanceMiles + newly computed distance
    public void updateDistance(Location newLocation) {
        double newDistance = distance(currentLocation.getLatitude(), newLocation.getLatitude(),
                currentLocation.getLongitude(), newLocation.getLongitude(), 0.0, 0.0);

        // standing still
        if(newDistance < NOISE_THRESHOLD) {
            newDistance = 0;
        }

        currentDistanceMiles = currentDistanceMiles + newDistance;
        Log.d(LOG_TAG, valueOf(newDistance));
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
        // starting run for the first time
        if(finishedRunning) {
            finishedRunning = false;
            currentDistanceMiles = 0;
            currentLocation = null;
        }
        // starting run after resuming
        isRunning = true;

    }

    public void stopRunning() {
        isRunning = false;
    }

    public void finishRunning() {
        finishedRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
