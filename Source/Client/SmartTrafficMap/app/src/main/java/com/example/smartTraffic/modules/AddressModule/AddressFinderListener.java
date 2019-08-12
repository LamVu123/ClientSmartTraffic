package com.example.smartTraffic.modules.AddressModule;

import com.google.android.gms.maps.model.Marker;

public interface AddressFinderListener {
    void onAddressFinderStart(String latlng);
    void onAddressFinderStart(String latlng, Marker marker);
    void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address);
    void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address, Marker marker);
}
