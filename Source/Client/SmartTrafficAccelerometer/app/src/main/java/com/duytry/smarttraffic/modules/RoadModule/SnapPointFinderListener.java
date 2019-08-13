package com.duytry.smarttraffic.modules.RoadModule;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface SnapPointFinderListener {
    void onSnapPointFinderStart(List<LatLng> listInputPoints, String filePath, String lastLine);
    void onSnapPointFinderSuccess(List<LatLng> listOutputPoints, String filePath, String lastLine);
}
