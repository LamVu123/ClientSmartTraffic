package com.example.smartTraffic.modules.RoadModule;

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
import java.util.ArrayList;
import java.util.List;

public class SnapPointFinder {
    private static final String GOOGLE_API_KEY = "AIzaSyBn63e-gCePem-dRTD3vABf4PL2KKDlYoY";
    private static final String SNAPPOINT_URL_API
            = "https://roads.googleapis.com/v1/snapToRoads?";

    private SnapPointFinderListener listener;
    private List<LatLng> listInputPoints = new ArrayList<>();

    public SnapPointFinder(SnapPointFinderListener listener, List<LatLng> listInputPoints) {
        this.listener = listener;
        this.listInputPoints = listInputPoints;
    }

    //create url with Snap point road
    private String createUrl() throws UnsupportedEncodingException {
        String points = "";
        if(!listInputPoints.isEmpty()) {
            for (LatLng item:listInputPoints) {
                points += String.valueOf(item.latitude)+", "+item.longitude + "|";
            };
            if(!points.isEmpty()){
                points = points.substring(0,points.length()-1);
            }
        }
        return SNAPPOINT_URL_API + "path=" + points + "&interpolate=true"
                + "&key=" + GOOGLE_API_KEY;
    }
    public void execute() throws UnsupportedEncodingException {
        new DownloadRawData().execute(createUrl());
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
            List<LatLng> listOutputPoint = new ArrayList<>();

            JSONObject jsonData = new JSONObject(data);
            JSONArray jsonRoutes = jsonData.getJSONArray("snappedPoints");

            for (int i = 0; i < jsonRoutes.length(); i++) {
                JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
                JSONObject jLocation = jsonRoute.getJSONObject("location");
                double lat = Double.parseDouble(jLocation.getString("latitude"));
                double lng = Double.parseDouble(jLocation.getString("longitude"));
                listOutputPoint.add(new LatLng(lat,lng));
            }
            listener.onSnapPointFinderSuccess(listOutputPoint);
        }
    }

}
