package com.example.smartTraffic.modules.PlacesAutoCompleteModule;

import java.util.List;

public interface PlacesFinderListener {
        void onPlacesFinderStart();
        void onPlacesFinderSuccess(List<String> listPlaces);
}
