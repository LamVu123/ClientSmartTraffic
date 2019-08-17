package com.duytry.smarttraffic.modules.AddressModule;

import android.widget.EditText;

public interface AddressFinderListener {
    void onAddressFinderStart(String latlng, EditText shortName, EditText longName);
    void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address, EditText shortName, EditText longName);
}
