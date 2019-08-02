package com.example.smartTraffic.modules.ShockPointModule;

import com.example.smartTraffic.entity.ShockPointEntity;

import java.util.ArrayList;

public interface ShockPointGetterListener {
    void onShockPointGetterStart();
    void onShockPointGetterSuccess(ArrayList<ShockPointEntity> shockPointList);

}
