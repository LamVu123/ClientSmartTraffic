package com.example.smartTraffic.modules.DistanceDirectionModule;

import android.location.Location;

import com.example.smartTraffic.entity.ShockPointEntity;
import com.example.smartTraffic.modules.DirectionModule.Route;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;


public interface DistanceFinderListener {
    void onDistanceFinderStart(Location currentLocation, ShockPointEntity shockPoint, float distanceAsTheCrowFlies);
    void onDistanceFinderSuccess(int distance, ShockPointEntity shockPoint, float distanceAsTheCrowFlies);
}
