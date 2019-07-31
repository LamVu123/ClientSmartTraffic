package RoadModule;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface SnapPointFinderListener {
    void onSnapPointFinderStart(List<LatLng> listInputPoints);
    void onSnapPointFinderSuccess(List<LatLng> listOutputPoints);
}
