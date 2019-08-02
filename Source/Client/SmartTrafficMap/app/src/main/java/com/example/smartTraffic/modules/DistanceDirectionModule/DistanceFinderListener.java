package com.example.smartTraffic.modules.DistanceDirectionModule;

import com.example.smartTraffic.modules.DirectionModule.Route;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;


public interface DistanceFinderListener {
    void onDistanceFinderStart(LatLng currentLocation, LatLng shockPointLocation);
    void onDistanceFinderSuccess(int distance);
}
