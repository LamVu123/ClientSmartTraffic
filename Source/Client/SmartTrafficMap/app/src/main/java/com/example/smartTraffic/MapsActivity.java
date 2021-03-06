package com.example.smartTraffic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartTraffic.common.MySocketFactory;
import com.example.smartTraffic.entity.ShockPointEntity;
import com.example.smartTraffic.modules.DistanceDirectionModule.DistanceFinderListener;
import com.example.smartTraffic.modules.DistanceDirectionModule.DistanceFinder;
import com.github.nkzawa.emitter.Emitter;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.example.smartTraffic.modules.DirectionModule.*;
import com.example.smartTraffic.modules.PlacesAutoCompleteModule.*;
import com.example.smartTraffic.modules.AddressModule.*;
import com.example.smartTraffic.modules.RoadModule.SnapPointFinder;
import com.example.smartTraffic.modules.RoadModule.SnapPointFinderListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        DirectionFinderListener, PlacesFinderListener, AddressFinderListener,
        SnapPointFinderListener, DistanceFinderListener {

    private GoogleMap mMap;
    private View view;
    private static final int REQUEST_CODE = 0;
    private static final String TAG = "MapsActivity";
    public static LatLng currentLocation;
    private Button btnFindPath;
    private Button btnSetting;
    private ListView listView;
    private EditText etOrigin;
    private EditText etDestination;
    private TextView tvDuration;
    private TextView tvDistance;
    private LinearLayout layout_dur_dis;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private String placeTyping;
    Boolean checkStatusListView = false;
    boolean checkWhereTyping = false;
    private ImageView iv_about;

    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private static String roadName;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int DEFAULT_ZOOM = 16;
    private static final int DIRECTION_ZOOM = 18;
    private final LatLng mDefaultLocation = new LatLng(21.013138, 105.526876);
    private static final int CONSIDER_DISTANCE = 400;
    private static final int WARNING_DISTANCE = 300;
    private static boolean isWarnningOn = false;
    //    private static boolean lastCheckShockPointAhead = false;
    private static boolean isMessageDisplayed = false;
    private static String SHOCK_POINT_MARKER_TITTLE = "Shock point";
    private MutableLiveData<Boolean> checkLocationMode = new MutableLiveData<>();

    private static ArrayList<ShockPointEntity> shockPointLists;
    //    private static ArrayList<ShockPointEntity> incomingShockPoints;
    private static ArrayList<Marker> shockPointMarkers = new ArrayList<>();
    private PowerManager pm = null;
    private boolean isDirection = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock keep_app_running = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Keep App Running");
        try {
            keep_app_running.acquire();
        } catch (Exception e) {
            e.printStackTrace();
        }

        turnOnGPS();
        if (Build.VERSION.SDK_INT >= 23) {
            setPermission();
        } else {
//            getDeviceLocation();
        }

        view = (View) findViewById(android.R.id.content);
        layout_dur_dis = findViewById(R.id.layout_dur_dis);
        tvDuration = (TextView) findViewById(R.id.tvDuration);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        iv_about = (ImageView) findViewById(R.id.iv_about);
        listView = (ListView) findViewById(R.id.listView);
        btnFindPath = (Button) findViewById(R.id.btnFindPath);
        btnSetting = (Button) findViewById(R.id.btnSetting);
        etOrigin = (EditText) findViewById(R.id.etOrigin);
        etDestination = (EditText) findViewById(R.id.etDestination);

        layout_dur_dis.setVisibility(View.INVISIBLE);

        //start event
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animation = new AlphaAnimation(1.1f, 0.3f);
                animation.setDuration(150);
                btnFindPath.startAnimation(animation);
                listView.setVisibility(View.GONE);
                if (!isDirection) {
                    sendRequestDirection();
                } else {
                    btnFindPath.setBackgroundResource(R.drawable.ic_find2);
                    etOrigin.setText("");
                    etDestination.setText("");
                    tvDistance.setText("");
                    tvDuration.setText("");
                    clearDirection();
                    isDirection = false;
                    layout_dur_dis.setVisibility(View.INVISIBLE);
                }
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Animation animation = new AlphaAnimation(1.1f, 0.3f);
                    animation.setDuration(150);
                    btnSetting.startAnimation(animation);
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    intent.putExtra("maptype", mMap.getMapType());
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        shockPointLists = new ArrayList<>();
//        incomingShockPoints = new ArrayList<>();
        checkLocationMode.postValue(true);
        checkLocationMode.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean mode) {
                if (mode != null) {
                    isWarnningOn = !mode;
                    if (mode) {
                        stopLocationUpdates(locationShockPointCallback);
                    } else {
                        createShockPointLocationRequest();
                        mFusedLocationProviderClient.requestLocationUpdates(shockPointLocationRequest,
                                locationShockPointCallback,
                                null /* Looper */);
                    }
                }
            }
        });

        iv_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setIcon(R.drawable.ic_logo);
                builder.setTitle("Smart Traffic");
                builder.setMessage(
                        "Lâm Hải Vũ\n" +
                                "Vũ Văn Cường\n" +
                                "Lê Hồng Dũng\n" +
                                "Nguyễn Duy Trí\n" +
                                "\n" +
                                "A good journey to you!!!\n" +
                                "\n" +
                                "Copyright\u00A9 FPT University");

                builder.setPositiveButton("Love Us !", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog alertDialog = builder.create();
                builder.show();
            }
        });
//        List<LatLng> listInputPoints = new ArrayList<>();
//        listInputPoints.add(new LatLng(21.003226, 105.663500));
//        listInputPoints.add(new LatLng(21.002905, 105.663350));
//        listInputPoints.add(new LatLng(21.002865, 105.662840));
//        listInputPoints.add(new LatLng(21.003053, 105.661649));
//        listInputPoints.add(new LatLng(21.002858, 105.662652));
//        listInputPoints.add(new LatLng(21.002833, 105.662258));
//        listInputPoints.add(new LatLng(21.002750, 105.661569));
//        onSnapPointFinderStart(listInputPoints);
//        onShockPointGetterStart("DCT08");

//        ShockPointEntity pointEntity = new ShockPointEntity(1,21.002833,105.662258);
//        Location location = new Location("start point");
//        location.setLatitude(21.002858);
//        location.setLongitude(105.662652);
//        onDistanceFinderStart(location,pointEntity,10);
//        showAlertDialogAutoClose("Warning", "Ahead " + WARNING_DISTANCE + "m is a shocking point", 3000);
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
        }
    }

    //user accept permission to access location
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = true;
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
            builder.show();
        }
    }

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

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog =
                ProgressDialog.show(this, "Please wait !",
                        "Finding direction...", true);
        if (isDirection) {

        }
        hideKeyboard(getApplicationContext(), view);
        clearDirection();
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        isDirection = true;
        layout_dur_dis.setVisibility(View.VISIBLE);
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        btnFindPath.setBackgroundResource(R.drawable.ic_park);
        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, DIRECTION_ZOOM));
            tvDuration.setText(route.duration.text);
            tvDistance.setText(route.distance.text);

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
                    width(20);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    private void clearDirection() {
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
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
                String mlatlng = String.valueOf(latLng.latitude) + ", " + latLng.longitude;
                Marker markerPin = mMap.addMarker(new MarkerOptions()
                        .position(latLng));
                onAddressFinderStart(mlatlng, markerPin);
//                markerPin.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_markerpin));
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if(marker.getTitle().equals(SHOCK_POINT_MARKER_TITTLE)){
                    return false;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle(marker.getSnippet());
                CharSequence[] choices = {"Direction from here","Direction to here","Get shock point","Delete Point","Close"};
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String pinLatLng = String.valueOf(marker.getPosition().latitude)+", "+marker.getPosition().longitude;
                        switch (which){
                            case 0:
                                etOrigin.setText(pinLatLng);
                                etDestination.setSelection(0,etDestination.length());
                                checkStatusListView = true;
                                break;
                            case 1:
                                etDestination.setText(pinLatLng);
                                etOrigin.setSelection(0,etOrigin.length());
                                checkStatusListView = true;
                                break;
                            case 2:
                                //String[] roadName = marker.getTitle().split(" - ");
                                onShockPointGetterStart(marker.getTitle());
                                break;
                            case 3:
                                marker.remove();
                                break;
                            default:
                                dialog.dismiss();
                                break;
                        }
                    }
                });
                builder.create().show();
                return true;
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {
                if(marker.getTitle().equals(SHOCK_POINT_MARKER_TITTLE)){
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                //builder.setTitle("Road: " + marker.getTitle());
                //builder.setMessage("Address: " + marker.getSnippet());
                builder.setTitle(marker.getSnippet());
                CharSequence[] choices = {"Direction from here","Direction to here","Get shock point","Delete Point","Close"};
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String pinLatLng = String.valueOf(marker.getPosition().latitude)+", "+marker.getPosition().longitude;
                        switch (which){
                            case 0:
                                etOrigin.setText(pinLatLng);
                                checkStatusListView = true;
                                break;
                            case 1:
                                etDestination.setText(pinLatLng);
                                checkStatusListView = true;
                                break;
                            case 2:
                                //String[] roadName = marker.getTitle().split(" - ");
                                onShockPointGetterStart(marker.getTitle());
                                break;
                            case 3:
                                marker.remove();
                                break;
                            default:
                                dialog.dismiss();
                                break;
                        }
                    }
                });
                builder.create().show();
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
            String provider = Settings.Secure.
                    getString(getContentResolver(),
                            Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (mLocationPermissionGranted && provider.contains("gps")) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            currentLocation = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
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

    private void shockingPointMarker(ShockPointEntity shockingPoint) {
        LatLng pointLatLng = new LatLng(shockingPoint.getLatitude(), shockingPoint.getLongitude());
        Marker pointMarker = mMap.addMarker(new MarkerOptions()
                .position(pointLatLng)
                .title(SHOCK_POINT_MARKER_TITTLE)
                //.snippet(shockingPoint.toString())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_shocking_point)));
        shockPointMarkers.add(pointMarker);
    }

    LocationRequest roadLocationRequest;
    LocationRequest shockPointLocationRequest;

    protected void createShockPointLocationRequest() {
        shockPointLocationRequest = LocationRequest.create();
        shockPointLocationRequest.setInterval(1000);
        shockPointLocationRequest.setFastestInterval(500);
        shockPointLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void createRoadLocationRequest() {
        roadLocationRequest = LocationRequest.create();
        roadLocationRequest.setInterval(10000);
        roadLocationRequest.setFastestInterval(5000);
        roadLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        createRoadLocationRequest();
        mFusedLocationProviderClient.requestLocationUpdates(roadLocationRequest,
                locationRoadCallback,
                null /* Looper */);
    }

    private LocationCallback locationShockPointCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                mLastKnownLocation = location;
                if (isWarnningOn) {
//                   if(!shockPointLists.isEmpty() && !lastCheckShockPointAhead){
                    if (!shockPointLists.isEmpty()) {
                        int numberOfConsider = shockPointLists.size();
                        numberOfConsider = numberOfConsider > 3 ? 3 : numberOfConsider;
                        for (int i = 0; i < numberOfConsider; i++) {
                            ShockPointEntity shockPoint = shockPointLists.get(i);
                            Location pointLocation = new Location("shock point ahead");
                            pointLocation.setLatitude(shockPoint.getLatitude());
                            pointLocation.setLongitude(shockPoint.getLongitude());
                            float distanceInMeters = pointLocation.distanceTo(location);
                            if (distanceInMeters <= CONSIDER_DISTANCE) {
                                onDistanceFinderStart(mLastKnownLocation, shockPoint, distanceInMeters);
                            }
                        }
                    }
//                   if(!incomingShockPoints.isEmpty()){
//                       ShockPointEntity incomingPoint = incomingShockPoints.get(0);
//                       Location incomingPointLocation = new Location("incoming shock point");
//                       incomingPointLocation.setLatitude(incomingPoint.getLatitude());
//                       incomingPointLocation.setLongitude(incomingPoint.getLongitude());
//                       float incomingDistance =  incomingPointLocation.distanceTo(location);
//                       if(incomingDistance <= WARNING_DISTANCE){
//                           showAlertDialogAutoClose("Warning", "Ahead " + WARNING_DISTANCE + "m is a shocking point", 3000);
//                           incomingShockPoints.remove(incomingPoint);
//                       }
//                   }
                }

            }
        }

        ;
    };

    private LocationCallback locationRoadCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                mLastKnownLocation = location;
                String mlatlng = String.valueOf(location.getLatitude()) + ", " + location.getLongitude();
                currentLocation = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                if (isDirection) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory
                            .newLatLngZoom(currentLocation, DIRECTION_ZOOM);
                    mMap.animateCamera(cameraUpdate);
                }
                onAddressFinderStart(mlatlng);
            }
        }

        ;
    };

    private void showAlertDialogAutoClose(String tittle, String message, final int time) {

//        String uriPath = "android.resource://"+getPackageName()+"/raw/alert1.mp3";
//        Uri notification = MediaStore.Audio.Media.getContentUriForPath(uriPath);;
//        final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//        r.play();

        final MediaPlayer[] mp;

        Locale current = getResources().getConfiguration().locale;
        if (TextUtils.equals(current.getCountry(), "VN")) {
            mp = new MediaPlayer[]{MediaPlayer.create(this, R.raw.alert_vietnamese)};
        } else {
            mp = new MediaPlayer[]{MediaPlayer.create(this, R.raw.alert_english)};
        }

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
        mp[0].setVolume(1, 1);


        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if (dlg.isShowing()) {
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
//        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket.hasListeners(ON_GET_POINTS_EVENT)) {
            mSocket.off(ON_GET_POINTS_EVENT);
        }
        if (mSocket.connected()) {
            mSocket.disconnect();
        }
    }
//dunglh 25/07/2019 end

    private void stopLocationUpdates(LocationCallback locationCallback) {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onAddressFinderStart(String latlng) {
        try {
            new AddressFinder(latlng, this).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddressFinderStart(String latlng, Marker marker) {
        try {
            new AddressFinder(latlng, this, marker).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address) {
        if (!TextUtils.equals(roadName, currentShortNameRoad)
                && !TextUtils.equals(currentLongNameRoad, "Unnamed Road")) {
            roadName = currentShortNameRoad;
            onShockPointGetterStart(roadName);
        }
    }

    Marker markerPin;

    @Override
    public void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address, Marker marker) {
        if (markerPin != null) {
            markerPin.remove();
        }
        //marker.setTitle(currentShortNameRoad + " - " + currentLongNameRoad);
        marker.setTitle(currentShortNameRoad);
        marker.setSnippet(address);
        marker.showInfoWindow();
        markerPin = marker;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if (resultCode == MapsActivity.RESULT_OK) {
                int maptype = data.getIntExtra("typeResult", 1);
                mMap.setMapType(maptype);
            }
        }
    }

    @Override
    public void onSnapPointFinderStart(List<LatLng> listInputPoints) {
        try {
            new SnapPointFinder(this, listInputPoints).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSnapPointFinderSuccess(List<LatLng> listOutputPoints) {
        //đã test ok.
    }

    //Shock Point Module Start

    private Socket mSocket = MySocketFactory.getInstance().getMySocket();
    private final String ON_GET_POINTS_EVENT = "onGetPointSuccess";

    private ArrayList<ShockPointEntity> createSomeDummyPoints() {
        ShockPointEntity shockingPoint1 = new ShockPointEntity(1, 21.016189, 105.554189);
        ShockPointEntity shockingPoint2 = new ShockPointEntity(2, 21.0129657, 105.5278376);
        ShockPointEntity shockingPoint3 = new ShockPointEntity(3, 21.010091, 105.523932);
        ShockPointEntity shockingPoint4 = new ShockPointEntity(4, 21.022830, 105.544446);

        ArrayList<ShockPointEntity> shockingPoints = new ArrayList<>();
        shockingPoints.add(shockingPoint3);
        shockingPoints.add(shockingPoint2);
        shockingPoints.add(shockingPoint4);
        shockingPoints.add(shockingPoint1);
        return shockingPoints;
    }

    private Emitter.Listener onGetShockPointsListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    ArrayList<ShockPointEntity> shockPoints = new ArrayList<>();
                    try {
                        JSONArray points = data.getJSONArray("dataResults");
                        if (points != null && points.length() > 0) {
                            for (int i = 0; i < points.length(); i++) {
                                JSONObject point = points.getJSONObject(i);
                                int pointId = point.getInt("id");
                                double latitude = point.getDouble("latitude");
                                double longitude = point.getDouble("longitude");
                                ShockPointEntity shockPointEntity;
                                shockPointEntity = new ShockPointEntity(pointId, latitude, longitude);
                                shockPoints.add(shockPointEntity);
                            }
                        }
                        onShockPointGetterSuccess(shockPoints);
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    public void onShockPointGetterStart(String roadName) {
        if (!mSocket.connected()) {
            mSocket.connect();
        }
        mSocket.emit("nameRoad", roadName);
        mSocket.on(ON_GET_POINTS_EVENT, onGetShockPointsListener);
    }

    Timer shockPointSortingTimer;

    public void onShockPointGetterSuccess(final ArrayList<ShockPointEntity> shockPointList) {
        if (mLastKnownLocation == null) {
            final Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    onShockPointGetterSuccess(shockPointList);
                    t.cancel();
                }
            }, 2000);
            return;
        }
        //sort based on current location
        Collections.sort(shockPointList, new ShockPointEntity.SortByDistance(mLastKnownLocation));
        shockPointSortingTimer = new Timer();
        shockPointSortingTimer.schedule(new TimerTask() {
            public void run() {
                Collections.sort(shockPointList, new ShockPointEntity.SortByDistance(mLastKnownLocation));
//                Toast.makeText(MapsActivity.this, "Last know location: " + mLastKnownLocation.getLatitude() + ", " +mLastKnownLocation.getLongitude(), Toast.LENGTH_LONG).show();
            }
        }, 20000, 20000);
        this.shockPointLists = shockPointList;
//        mMap.clear();
        //clear old shock point
        for (int i = 0; i < shockPointMarkers.size(); i++) {
            shockPointMarkers.get(i).remove();
        }
        for (ShockPointEntity shockPoint : shockPointList) {
            shockingPointMarker(shockPoint);
        }
        checkLocationMode.postValue(false);
    }

    @Override
    public void onDistanceFinderStart(Location currentLocation, ShockPointEntity shockPoint, float distanceAsTheCrowFlies) {
        try {
            new DistanceFinder(this, currentLocation, shockPoint, distanceAsTheCrowFlies).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDistanceFinderSuccess(int distance, ShockPointEntity shockPoint, float distanceAsTheCrowFlies) {
        if (distance < distanceAsTheCrowFlies * 1.1) {
//            incomingShockPoints.add(shockPoint);
            showAlertDialogAutoClose("Warning", "There is a shock point ahead", 3000);
        }
//        shockPointLists.remove(shockPoint);
    }
//Shock Point Module End
}
