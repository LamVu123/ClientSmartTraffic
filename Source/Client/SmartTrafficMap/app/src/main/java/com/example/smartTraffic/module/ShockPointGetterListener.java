package com.example.smartTraffic.module;

import com.example.smartTraffic.entity.ShockPointEntity;

import java.util.ArrayList;

public interface ShockPointGetterListener {
    void onShockPointGetterStart();
    void onShockPointGetterSuccess(ArrayList<ShockPointEntity> shockPointList);

}
