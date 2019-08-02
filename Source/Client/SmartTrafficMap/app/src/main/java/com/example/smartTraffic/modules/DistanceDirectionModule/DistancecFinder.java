package com.example.smartTraffic.modules.DistanceDirectionModule;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

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

public class DistancecFinder {
    private static final String DIRECTION_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyAknAPT_Qiyp5u9xtCuKzuGL9auMxsYWuU";
    private DistanceFinderListener listener;
    private LatLng currentLocation;
    private LatLng shockPointLocation;

    public DistancecFinder(DistanceFinderListener listener, LatLng currentLocation, LatLng shockPointLocation) {
        this.listener = listener;
        this.currentLocation = currentLocation;
        this.shockPointLocation = shockPointLocation;
    }

    public void execute() throws UnsupportedEncodingException {
        //listener.onDirectionFinderStart();
        new DownloadRawData().execute(createUrlToDirection());
    }

    //create url to direction
    private String createUrlToDirection() throws UnsupportedEncodingException {
        String urlOrigin = String.valueOf(currentLocation.latitude)+", "+currentLocation.longitude;
        String urlDestination = String.valueOf(shockPointLocation.latitude)+", "+shockPointLocation.longitude;

        return DIRECTION_URL_API + "origin=" + urlOrigin + "&destination=" + urlDestination + "&key=" + GOOGLE_API_KEY;
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
    }

    private void parseJSon(String data) throws JSONException {
        if (data == null)
            return;
        int distance = 0;
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
            JSONObject jsonRoute = jsonRoutes.getJSONObject(0);
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            JSONObject jsonLeg = jsonLegs.getJSONObject(0);
            JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
            distance = jsonDistance.getInt("value");
        listener.onDistanceFinderSuccess(distance);
    }
}
