package com.example.smartTraffic.modules.AddressModule;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class AddressFinder {

    private static final String GOOGLE_API_KEY = "AIzaSyBn63e-gCePem-dRTD3vABf4PL2KKDlYoY";
    private static final String Geocoding_URL_API
            = "https://maps.googleapis.com/maps/api/geocode/json?";

    private String currentAddress;
    private String currentLongNameRoad;
    private String currentShortNameRoad;
    private String currentLatLong;
    private AddressFinderListener listener;
    private Marker marker;

    public AddressFinder(String currentLatLong, AddressFinderListener listener) {
        this.currentLatLong = currentLatLong;
        this.listener = listener;
    }

    public AddressFinder(String currentLatLong, AddressFinderListener listener, Marker marker) {
        this.currentLatLong = currentLatLong;
        this.listener = listener;
        this.marker = marker;
    }

    //create url with Geocoding API
    private String createUrlGeocoding(String latlng) throws UnsupportedEncodingException {
        String currlaglng = URLEncoder.encode(latlng, "utf-8");
        return Geocoding_URL_API + "latlng=" + currlaglng
                + "&key=" + GOOGLE_API_KEY;
    }

    public void execute() throws UnsupportedEncodingException {
        new DownloadRawData().execute(createUrlGeocoding(currentLatLong));
    }

    private class DownloadRawData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parseJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void parseJSon(String data) throws JSONException {
            if (data == null) {
                return;
            }
            List<String> listPlaces = new ArrayList<>();

            JSONObject jsonData = new JSONObject(data);
            JSONArray jsonRoutes = jsonData.getJSONArray("results");

            for (int i = 0; i < jsonRoutes.length(); i++) {
                JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
                JSONArray jType = jsonRoute.getJSONArray("types");
                if(jType.get(0).toString().equals("street_address")){
                    currentAddress = jsonRoute.getString("formatted_address");
                    JSONArray addressComponents = jsonRoute.getJSONArray("address_components");
                    for (int j = 0; j < addressComponents.length(); j++){
                        JSONObject addressComponent = addressComponents.getJSONObject(j);
                        if(addressComponent.getJSONArray("types").get(0).equals("route")){
                            currentLongNameRoad = addressComponent.getString("long_name");
                            currentShortNameRoad = addressComponent.getString("short_name");
                            break;
                        }
                    }
                    break;
                } else {
                    if(jType.get(0).toString().equals("route")){
                        currentAddress = jsonRoute.getString("formatted_address");
                        JSONArray addressComponents = jsonRoute.getJSONArray("address_components");
                        for (int j = 0; j < addressComponents.length(); j++){
                            JSONObject addressComponent = addressComponents.getJSONObject(j);
                            if(addressComponent.getJSONArray("types").get(0).equals("route")){
                                currentLongNameRoad = addressComponent.getString("long_name");
                                currentShortNameRoad = addressComponent.getString("short_name");
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if(marker != null ){
                listener.onAddressFinderSuccess(currentLongNameRoad, currentShortNameRoad, currentAddress, marker);
            } else {
                listener.onAddressFinderSuccess(currentLongNameRoad, currentShortNameRoad, currentAddress);
            }

        }
    }
}
