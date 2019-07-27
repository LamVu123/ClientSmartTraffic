package PlacesAutoCompleteModule;

import android.os.AsyncTask;

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

public class PlacesListFinder {
    private static final String AutoPlace_URL_API = "https://maps.googleapis.com/maps/api/place/autocomplete/json?";
    private static final String SessionToken = "1234567890";
    private static final String GOOGLE_API_KEY = "AIzaSyAknAPT_Qiyp5u9xtCuKzuGL9auMxsYWuU";
    private String placeTyping;
    private PlacesFinderListener listener;

    public PlacesListFinder(PlacesFinderListener placesFinderListener, String placeType) {
        this.placeTyping = placeType;
        this.listener = placesFinderListener;
    }

    //create url to suggest list places
    private String createUrlToPlaces(String placeTyping) throws UnsupportedEncodingException {
        String placeTypeedcoded = URLEncoder.encode(placeTyping, "utf-8");
        return AutoPlace_URL_API + "input=" + placeTypeedcoded + "&key="
                + GOOGLE_API_KEY + "&sessiontoken=" + SessionToken;
    }

    public void execute() throws UnsupportedEncodingException {
//        listener.onPlacesFinderStart();
        new DownloadRawData().execute(createUrlToPlaces(placeTyping));
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
            List<String> listPlaces = new ArrayList<>();
            if (data == null) {
                return;
            }
            JSONObject jsonData = new JSONObject(data);
            JSONArray jsonRoutes = jsonData.getJSONArray("predictions");
            for (int i = 0; i < jsonRoutes.length(); i++) {
                JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
                String place = jsonRoute.getString("description");
                listPlaces.add(place);
            }
            listener.onPlacesFinderSuccess(listPlaces);
        }
    }
}
