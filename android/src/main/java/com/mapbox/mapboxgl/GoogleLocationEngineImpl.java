package com.mapbox.mapboxgl;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.mapboxsdk.location.engine.LocationEngine;
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback;
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult;

import java.util.Collections;

/**
 * Wraps implementation of Fused Location Provider
 */
class GoogleLocationEngineImpl implements LocationEngine {
    private final FusedLocationProviderClient fusedLocationProviderClient;

    GoogleLocationEngineImpl(@NonNull Context context) {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
        GoogleLastLocationEngineCallbackTransport transport =
                new GoogleLastLocationEngineCallbackTransport(callback);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(transport).addOnFailureListener(transport);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, @NonNull LocationEngineCallback<LocationEngineResult> callback, @Nullable Looper looper) throws SecurityException {
        LocationListenerWrapper listener = new LocationListenerWrapper(callback);
        fusedLocationProviderClient.requestLocationUpdates(toGMSLocationRequest(request), listener, looper);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, PendingIntent pendingIntent) throws SecurityException {
        fusedLocationProviderClient.requestLocationUpdates(toGMSLocationRequest(request), pendingIntent);
    }

    @Override
    public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
        LocationCallbackWrapper callbackWrapper =
                new LocationCallbackWrapper(callback);
        fusedLocationProviderClient.removeLocationUpdates(callbackWrapper);
    }

    @Override
    public void removeLocationUpdates(PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            fusedLocationProviderClient.removeLocationUpdates(pendingIntent);
        }
    }

    // Utility


    private static LocationRequest toGMSLocationRequest(LocationEngineRequest request) {
        LocationRequest.Builder builder = new LocationRequest.Builder(request.getInterval());
        builder.setMinUpdateIntervalMillis(request.getFastestInterval());
        builder.setMinUpdateDistanceMeters(request.getDisplacement());
        builder.setMaxUpdateDelayMillis(request.getMaxWaitTime());
        builder.setPriority(toGMSLocationPriority(request.getPriority()));
        return builder.build();
    }

    private static int toGMSLocationPriority(int enginePriority) {
        switch (enginePriority) {
            case LocationEngineRequest.PRIORITY_HIGH_ACCURACY:
                return Priority.PRIORITY_HIGH_ACCURACY;
            case LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                return Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            case LocationEngineRequest.PRIORITY_LOW_POWER:
                return Priority.PRIORITY_LOW_POWER;
            case LocationEngineRequest.PRIORITY_NO_POWER:
            default:
                return Priority.PRIORITY_PASSIVE;
        }
    }

    //LocationListener

    @VisibleForTesting
    static final class LocationListenerWrapper implements LocationListener {
        private final LocationEngineCallback<LocationEngineResult> callback;

        LocationListenerWrapper(LocationEngineCallback<LocationEngineResult> callback) {

            this.callback = callback;
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            final LocationEngineResult results =LocationEngineResult.create(location);
            callback.onSuccess(results);
        }
    }

    @VisibleForTesting
    static final class LocationCallbackWrapper extends LocationCallback {
        private final LocationEngineCallback<LocationEngineResult> callback;

        LocationCallbackWrapper(LocationEngineCallback<LocationEngineResult> callback) {
            this.callback = callback;
        }

        @Override
        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            if(!locationAvailability.isLocationAvailable()) {
                callback.onFailure(new RuntimeException("Location Not available"));
            }
        }

        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            final LocationEngineResult results =LocationEngineResult.create(locationResult.getLocations());
            callback.onSuccess(results);
        }
    }

    @VisibleForTesting
    static final class GoogleLastLocationEngineCallbackTransport
            implements OnSuccessListener<Location>, OnFailureListener {
        private final LocationEngineCallback<LocationEngineResult> callback;

        GoogleLastLocationEngineCallbackTransport(LocationEngineCallback<LocationEngineResult> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(Location location) {
            callback.onSuccess(location != null ? LocationEngineResult.create(location) :
                    LocationEngineResult.create(Collections.<Location>emptyList()));
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            callback.onFailure(e);
        }
    }
}
