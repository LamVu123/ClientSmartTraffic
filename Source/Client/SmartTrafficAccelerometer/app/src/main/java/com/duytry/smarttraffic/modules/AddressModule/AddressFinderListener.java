package com.duytry.smarttraffic.modules.AddressModule;

import android.app.AlertDialog;
import android.widget.EditText;

public interface AddressFinderListener {
    void onAddressFinderStart(String latlng, AlertDialog dialog);
    void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address, AlertDialog dialog);
}
