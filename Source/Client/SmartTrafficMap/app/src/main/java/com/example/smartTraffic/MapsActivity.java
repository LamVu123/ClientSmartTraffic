package com.example.smartTraffic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartTraffic.entity.RoadEntity;
import com.example.smartTraffic.entity.ShockingPointEntity;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import DirectionModule.*;
import PlacesAutoCompleteModule.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        DirectionFinderListener, PlacesFinderListener {

    private GoogleMap mMap;
    private static final int REQUEST_CODE = 0;
    private static final String TAG = "MapsActivity";
    public static LatLng currentLocation;
    private Button btnFindPath;
    private ListView listView;
    private EditText etOrigin;
    private EditText etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private String placeTyping;
    Boolean checkStatusListView = false;
    boolean checkWhereTyping = false;

    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int DEFAULT_ZOOM = 15;
    private final LatLng mDefaultLocation = new LatLng(21.013138, 105.526876);
    private static final int WARNING_DISTANCE = 400;
    private static boolean isWarnningOn = false;
    private static boolean isMessageDisplayed = false;

    private static ArrayList<ShockingPointEntity> shockingPointAheads;
    private static ArrayList<ShockingPointEntity> incomingShockingPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        turnOnGPS();
        if (Build.VERSION.SDK_INT >= 23) {
            setPermission();
        } else {
            //getCurrentLocation();
            getDeviceLocation();
        }

        listView = (ListView) findViewById(R.id.listView);
        btnFindPath = (Button) findViewById(R.id.btnFindPath);
        etOrigin = (EditText) findViewById(R.id.etOrigin);
        etDestination = (EditText) findViewById(R.id.etDestination);


        //start event
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animation = new AlphaAnimation(1.1f, 0.3f);
                animation.setDuration(150);
                btnFindPath.startAnimation(animation);
                sendRequestDirection();
                CameraUpdate cameraUpdate = CameraUpdateFactory
                        .newLatLngZoom(currentLocation, 19);
                mMap.animateCamera(cameraUpdate);

            }
        });

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (checkStatusListView) {
                    checkStatusListView = false;
                    return;
                }
                placeTyping = etDestination.getText().toString();
                checkWhereTyping = false;
                autoCompletePlacesRequest();

            }
        });
        etOrigin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (checkStatusListView) {
                    checkStatusListView = false;
                    return;
                }
                placeTyping = etOrigin.getText().toString();
                checkWhereTyping = true;
                autoCompletePlacesRequest();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkStatusListView = true;
                String item = (String) parent.getAdapter().getItem(position);
                if (checkWhereTyping) {
                    etOrigin.setText(item);
                    etOrigin.setSelection(etOrigin.getText().length());
                } else {
                    etDestination.setText(item);
                    etDestination.setSelection(etDestination.getText().length());
                }

                listView.setVisibility(View.GONE);

            }
        });

        shockingPointAheads = new ArrayList<>();
        incomingShockingPoints = new ArrayList<>();
    }


    private void autoCompletePlacesRequest() {
        if (placeTyping.isEmpty()) {
            listView.setVisibility(View.GONE);
            return;
        }
        try {
            new PlacesListFinder(this, placeTyping).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlacesFinderStart() {

    }

    @Override
    public void onPlacesFinderSuccess(List<String> listPlaces) {
        try {
            listView.setVisibility(View.VISIBLE);
            ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(),
                    R.layout.listview_place, listPlaces);
            listView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    private void callAlert(String mess){
//        AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
//        alert.setTitle("ok");
//        alert.setMessage(mess);
//        alert.setPositiveButton("OK", null);
//        alert.show();
//    }

    //set permission to access location and getCurrentLocation
    private void setPermission() {
        //check and request permission
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                    , REQUEST_CODE);
        } else {
            mLocationPermissionGranted = true;
//            getCurrentLocation();
            getDeviceLocation();
        }
    }

    //user accept permission to access location
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = true;
        //getCurrentLocation();
        getDeviceLocation();
    }

    // Check gps, if turn off => request turn on .
    private void turnOnGPS() {
        //get gps status
        String provider = Settings.Secure.
                getString(getContentResolver(),
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if (!provider.contains("gps")) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Smart Traffic");
            builder.setMessage("Do you turn on your GPS?");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            , 0);
                }
            });
            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    private void sendRequestDirection() {
        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();

        if (origin.isEmpty()) {
            origin = mLastKnownLocation.getLatitude() + ", "
                    + mLastKnownLocation.getLongitude();
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter your destination!", Toast.LENGTH_LONG).show();
            return;
        }
        //Log.d(TAG,"etOrigin: "+origin);
        //Log.d(TAG,"etDestination: "+destination);

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 15));
            ((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
            ((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                    .title(route.startAddress)
                    .position(route.startLocation)
            ));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in FPT University
        //LatLng fptUniLocal = new LatLng(21.013138, 105.526876);
        //mMap.addMarker(new MarkerOptions().position(fptUniLocal).title("Marker in FPT University"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fptUniLocal, 15));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
                alert.setTitle("Event 1 Điểm trên map");
                alert.setPositiveButton("OK", null);
                alert.show();
            }
        });
        //dunglh 25/07 start
        // Prompt the user for permission.
        setPermission();
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        startLocationUpdates();
        mSocket.connect();
        getShockPoints();
        //dunglh 25/07 end
    }

    //dunglh 25/07/2019 start
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                setPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            currentLocation = new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getShockPoints() {
//        mSocket.emit("road", "test");
//        mSocket.on("points", onMessage_Results);
        testMarker();
        isWarnningOn = true;
    }

    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(BuildConfig.GetDataIp);
        } catch (URISyntaxException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private Emitter.Listener onMessage_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];

                    try {
                        JSONObject road = data.getJSONObject("road");
                        int id = road.getInt("id");
                        String name = road.getString("name");
                        RoadEntity roadEntity = new RoadEntity(id, name);
                        JSONArray points = (JSONArray) data.getJSONArray("points");
                        for (int i = 0; i < points.length(); i++) {
                            JSONObject point = (JSONObject) points.get(i);
                            int pointId = point.getInt("id");
                            double pointLat = point.getDouble("latitude");
                            double pointLng = point.getDouble("longitude");
                            ShockingPointEntity shockingPoint = new ShockingPointEntity(pointId, pointLat, pointLng, roadEntity);
                            shockingPointMarker(shockingPoint);
                        }

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private void testMarker() {
        ArrayList<ShockingPointEntity> shockingPoints = createSomeDummyPoints();
        for (ShockingPointEntity shockingPoint : shockingPoints) {
            shockingPointMarker(shockingPoint);
        }
        //TODO arrange points from distance
        shockingPointAheads = shockingPoints;
    }

    private ArrayList<ShockingPointEntity> createSomeDummyPoints() {
        ShockingPointEntity shockingPoint1 = new ShockingPointEntity(1, 21.016189, 105.554189);
        ShockingPointEntity shockingPoint2 = new ShockingPointEntity(2, 21.0129657, 105.5278376);
        ShockingPointEntity shockingPoint3 = new ShockingPointEntity(3, 21.010091, 105.523932);
        ShockingPointEntity shockingPoint4 = new ShockingPointEntity(4, 21.022830, 105.544446);

        ArrayList<ShockingPointEntity> shockingPoints = new ArrayList<>();
        shockingPoints.add(shockingPoint1);
        shockingPoints.add(shockingPoint2);
        shockingPoints.add(shockingPoint3);
        shockingPoints.add(shockingPoint4);
        return shockingPoints;
    }


    private void shockingPointMarker(ShockingPointEntity shockingPoint) {
        LatLng pointLatLng = new LatLng(shockingPoint.getLatitude(), shockingPoint.getLongitude());
        Marker pointMarker = mMap.addMarker(new MarkerOptions()
                .position(pointLatLng)
                .title("Shock point")
                .snippet(shockingPoint.toString())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_shocking_point)));
    }

    LocationRequest locationRequest;

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        createLocationRequest();
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
               mLastKnownLocation = location;
               if(isWarnningOn && !shockingPointAheads.isEmpty()){
                   ShockingPointEntity nearestPoint = shockingPointAheads.get(0);
                   Location nearestPointLocation = new Location("incoming shocking point");
                   nearestPointLocation.setLatitude(nearestPoint.getLatitude());
                   nearestPointLocation.setLongitude(nearestPoint.getLongitude());
                   float distanceInMeters =  nearestPointLocation.distanceTo(location);
                   if(distanceInMeters <= WARNING_DISTANCE){
                       //TODO improve remove last shocking point feature
                       if(!incomingShockingPoints.isEmpty()){
                           ShockingPointEntity incomingPoint = incomingShockingPoints.get(0);
                           Location incomingPointLocation = new Location("incoming shocking point");
                           incomingPointLocation.setLatitude(incomingPoint.getLatitude());
                           incomingPointLocation.setLongitude(incomingPoint.getLongitude());
                           float distance =  nearestPointLocation.distanceTo(location);
                           if(distance <= 100){
                               incomingShockingPoints.remove(0);
                           }
                       }
                       //TODO end

                       //start warning
                       incomingShockingPoints.add(shockingPointAheads.get(0));
                       shockingPointAheads.remove(0);
                       showAlertDialogAutoClose("Warning", "Ahead " + WARNING_DISTANCE + "m is a shocking point", 5000);
                   }
               }
            }
        };
    };

    private void showAlertDialogAutoClose(String tittle, String message, final int time){

//        String uriPath = "android.resource://"+getPackageName()+"/raw/alert1.mp3";
//        Uri notification = MediaStore.Audio.Media.getContentUriForPath(uriPath);;
//        final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//        r.play();

        final MediaPlayer[] mp = {MediaPlayer.create(this, R.raw.alert1)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(tittle);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mp[0].isPlaying()) {
                    mp[0].stop();
                    mp[0].release();
                }
            }
        });

        final AlertDialog dlg = builder.create();

        dlg.show();
        mp[0].start();


        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if(dlg.isShowing()){
                    if (mp[0].isPlaying()) {
                        mp[0].stop();
                        mp[0].release();
                    }
                    dlg.dismiss(); // when the task active then close the dialog
                }
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, time);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    //dunglh 25/07/2019 end

}
