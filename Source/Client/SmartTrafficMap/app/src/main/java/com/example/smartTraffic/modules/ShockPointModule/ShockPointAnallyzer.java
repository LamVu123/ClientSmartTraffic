package com.example.smartTraffic.modules.ShockPointModule;

import com.example.smartTraffic.entity.ShockPointEntity;
import com.example.smartTraffic.modules.DistanceDirectionModule.DistanceFinderListener;
import com.example.smartTraffic.modules.DistanceDirectionModule.DistanceFinder;
import com.google.android.gms.maps.model.LatLng;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ShockPointAnallyzer implements DistanceFinderListener {

    ShockPointAnallyzerListener listener;
    ArrayList<ShockPointEntity> shockPoints;
    LatLng currentLatLng;

    public ShockPointAnallyzer(ShockPointAnallyzerListener listener, ArrayList<ShockPointEntity> shockPoints, LatLng currentLatLng) {
        this.listener = listener;
        this.shockPoints = shockPoints;
        this.currentLatLng = currentLatLng;
    }

    public void excute(){
        for (ShockPointEntity shockPoint : shockPoints ) {

            //TODO get distance follow the flight path

            //TODO get distance from direction

            //compare


        }
    }


    @Override
    public void onDistanceFinderStart(LatLng currentLocation, LatLng shockPointLocation) {
        try {
            new DistanceFinder(this,currentLocation,shockPointLocation).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDistanceFinderSuccess(int distance) {

    }
}
