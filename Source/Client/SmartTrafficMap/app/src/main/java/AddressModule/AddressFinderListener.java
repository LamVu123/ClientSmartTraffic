package AddressModule;

public interface AddressFinderListener {
    void onAddressFinderStart(String latlng);
    void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address);
}
