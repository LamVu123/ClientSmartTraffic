package com.example.smartTraffic.modules.ShockPointModule;

import com.example.smartTraffic.entity.ShockPointEntity;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class ShockPointAnallyzer {

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


}
