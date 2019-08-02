package com.example.smartTraffic.modules.ShockPointModule;

import com.example.smartTraffic.entity.ShockPointEntity;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public interface ShockPointAnallyzerListener {
    void onShockPointAnallyzerStart(ArrayList<ShockPointEntity> shockPointList, LatLng currentLocation);
    void onShockPointAnallyzerSuccess(ArrayList<ShockPointEntity> shockPointList);
}
