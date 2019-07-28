package PlacesAutoCompleteModule;

import java.util.List;

import DirectionModule.Route;

public interface PlacesFinderListener {
        void onPlacesFinderStart();
        void onPlacesFinderSuccess(List<String> listPlaces);
}
