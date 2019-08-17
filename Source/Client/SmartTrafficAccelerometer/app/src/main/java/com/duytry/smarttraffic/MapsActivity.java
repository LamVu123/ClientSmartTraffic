package com.duytry.smarttraffic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.duytry.smarttraffic.adapter.MyRoadAdapter;
import com.duytry.smarttraffic.common.Common;
import com.duytry.smarttraffic.common.MySocketFactory;
import com.duytry.smarttraffic.fragment.MyDialogFragment;
import com.duytry.smarttraffic.entity.RoadEntity;
import com.duytry.smarttraffic.entity.ShockPointEntity;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private TextView roadInfoTextView;
    private Button goBackButton, changeRoadButton, toolButton;
    private ArrayList<RoadEntity> roadList;
    private MutableLiveData<RoadEntity> currentRoad = new MutableLiveData<>();
    private ArrayList<ShockPointEntity> shockPointList;
    private ArrayList<Marker> markerList;
    private AlertDialog dialogChooseRoad;
    private AlertDialog dialogShockPointInfo;
    private AlertDialog dialogRoadInfo;
    private static final int DEFAULT_ZOOM = 16;
    private static final int WIDE_ZOOM = 13;

    private Socket mSocket = MySocketFactory.getInstance().getMySocket();

    private static final String SHOCK_POINT_MARKER_TITTLE = "Shock point";
    private static final String GET_ROAD_EVENT_SOCKET = "listRoadResult";
    private static final String GET_POINT_EVENT_SOCKET = "listPointsResult";
    private static final String GET_DATA_EVENT_SOCKET = "dataPoint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initLayout();
        roadList = new ArrayList<>();
        shockPointList = new ArrayList<>();
        markerList = new ArrayList<>();
        getRoadsFromServer();
    }

    private void initLayout() {
        roadInfoTextView = findViewById(R.id.tv_current_road);
        goBackButton = findViewById(R.id.btn_back_to_main);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        changeRoadButton = findViewById(R.id.btn_change_road);
        changeRoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRoadsToChange();
            }
        });

        toolButton = findViewById(R.id.btn_tool);
        toolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageRoad();
            }
        });
        currentRoad.observe(this, new Observer<RoadEntity>() {
            @Override
            public void onChanged(@Nullable RoadEntity roadEntity) {
                if(roadEntity == null){
                    roadInfoTextView.setText("You are not in any road");
                    return;
                }
                roadInfoTextView.setText("You are in " + roadEntity.getShortName());
            }
        });
    }

    private void getRoadsFromServer(){
        if(!mSocket.connected()){
            mSocket.connect();
        }
        mSocket.emit("listRoad", "listRoad");
        mSocket.on(GET_ROAD_EVENT_SOCKET, onGetRoad_Results);
    }

    private void showRoadsToChange() {
        if (roadList.isEmpty()) {
           getRoadsFromServer();
            FragmentManager fragmentManager = getSupportFragmentManager();
            MyDialogFragment dialogFragment = new MyDialogFragment();
            dialogFragment.setTittle("Opps");
            dialogFragment.setMessage("please try again");
            dialogFragment.show(fragmentManager, "empty road");
            return;
        }
        RoadEntity[] roads = roadList.toArray(new RoadEntity[roadList.size()]);
        // our adapter instance
        MyRoadAdapter adapter = new MyRoadAdapter(MapsActivity.this, R.layout.list_view_road, roads);

        // create a new ListView, set the adapter and item click listener
        ListView listViewItems = new ListView(this);
        listViewItems.setAdapter(adapter);
        listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textViewItem = ((TextView) view.findViewById(R.id.textView_road_entitty));
                int listItemId = (int) textViewItem.getTag();
                showRoadInfo(listItemId);
            }
        });
        dialogChooseRoad = new AlertDialog.Builder(MapsActivity.this)
                .setView(listViewItems)
                .setTitle(R.string.title_show_road)
                .show();
    }

    private void showRoadInfo(int roadId) {
        dialogChooseRoad.dismiss();
        for (RoadEntity road : roadList) {
            if (road.getId() == roadId) {
                currentRoad.postValue(road);
                break;
            }
        }
        mSocket.emit("idRoad", roadId);
        mSocket.on(GET_POINT_EVENT_SOCKET, onGetPoint_Results);
    }

    private void manageRoad(){
        if(currentRoad.getValue() == null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            MyDialogFragment dialogFragment = new MyDialogFragment();
            dialogFragment.setTittle("Opps");
            dialogFragment.setMessage("Please choose road to manage");
            dialogFragment.show(fragmentManager, "empty road");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View roadInfoView = inflater.inflate(R.layout.display_road_info, null);
        TextView roadId = roadInfoView.findViewById(R.id.textView_road_info_id);
        final EditText roadName = roadInfoView.findViewById(R.id.editText_road_name);
        final Button deleteButton = roadInfoView.findViewById(R.id.btn_delete_road);
        final Button editButton = roadInfoView.findViewById(R.id.btn_edit_road);
        final Button saveButton = roadInfoView.findViewById(R.id.btn_save_road);

        final RoadEntity road = currentRoad.getValue();

        roadId.setText("ID: " + road.getId());
        roadName.setText(road.getShortName());
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogRoadInfo.dismiss();
                deleteRoad(road);
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
                roadName.setEnabled(true);
                deleteButton.setEnabled(false);
                roadName.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isValid = validateNewName(roadName.getText().toString());
                if(isValid){
                    editRoad(road, roadName.getText().toString());
                    editButton.setVisibility(View.VISIBLE);
                    saveButton.setVisibility(View.GONE);
                    deleteButton.setEnabled(true);
                    roadName.setEnabled(false);
                } else {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    MyDialogFragment dialogFragment = new MyDialogFragment();
                    dialogFragment.setTittle(getString(R.string.road_name_not_valid_tittle));
                    dialogFragment.setMessage(getString(R.string.road_name_not_valid_message));
                    dialogFragment.show(fragmentManager, "warning road name not valid");
                }

            }
        });
        builder.setView(roadInfoView);
        dialogRoadInfo = builder.create();
        dialogRoadInfo.show();

    }

    private boolean validateNewName(String newName){
        if(TextUtils.isEmpty(newName)){
            return false;
        }
        for ( RoadEntity road : roadList ) {
            if(TextUtils.equals(road.getShortName(), newName)){
                return false;
            }
        }
        return true;
    }

    private void deleteRoad(RoadEntity road){
        mSocket.emit("roadDelete", road.getId());
        currentRoad.postValue(null);
        roadList.remove(road);
        Toast.makeText(this, "deleted road "+ road.getId(), Toast.LENGTH_LONG).show();
    }

    private boolean editRoad(RoadEntity road, String newName){
        String json = "{" + "\"idRoad\" : \"" + road.getId() + "\", \"newName\" : " + "\"" + newName + "\"" + "}";
        try {
            JSONObject data = new JSONObject(json);
            mSocket.emit("updateNameRoad", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        roadList.get(roadList.indexOf(currentRoad.getValue())).setShortName(newName);
        this.currentRoad.getValue().setShortName(newName);
        currentRoad.setValue(currentRoad.getValue());
        Toast.makeText(this, "edit road "+ road.getId() + " to new name " + newName, Toast.LENGTH_LONG).show();
        return true;
    }



    private Emitter.Listener onGetRoad_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        JSONArray roads = data.getJSONArray("dataListRoad");
                        if (roads != null && roads.length() > 0) {
                            roadList.clear();
                            for (int i = 0; i < roads.length(); i++) {
                                JSONObject road = roads.getJSONObject(i);
                                int roadId = road.getInt("idroad");
                                String roadName = road.getString("nameroad");
                                RoadEntity roadEntity = new RoadEntity(roadId, roadName);
                                roadList.add(roadEntity);
                            }
                            Toast.makeText(MapsActivity.this, "get " + roadList.size() + " road success", Toast.LENGTH_LONG).show();
                        } else {
                            alertNoRoadAvailable();
                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private Emitter.Listener onGetPoint_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        JSONArray points = data.getJSONArray("points");
                        if (points != null && points.length() > 0) {
                            shockPointList.clear();
                            markerList.clear();
                            for (int i = 0; i < points.length(); i++) {
                                JSONObject point = points.getJSONObject(i);
                                int pointId = point.getInt("id");
                                int roadId = point.getInt("idroad");
                                double latitude = point.getDouble("latitude");
                                double longitude = point.getDouble("longitude");
                                String timeCollect = point.getString("timecollect");
                                String nameFileData = point.getString("namefiledata");
                                ShockPointEntity shockPointEntity;
                                if (roadId == currentRoad.getValue().getId()) {
                                    shockPointEntity = new ShockPointEntity(pointId, currentRoad.getValue(), latitude, longitude, timeCollect, nameFileData);
                                } else {
                                    shockPointEntity = new ShockPointEntity(pointId, latitude, longitude, timeCollect, nameFileData);
                                }
                                shockPointList.add(shockPointEntity);
                            }
                            Toast.makeText(MapsActivity.this, "get " + shockPointList.size() + " points success", Toast.LENGTH_LONG).show();
                            displayShockPoint();
                        } else if(points.length() == 0){
                            alertNoPointAvailable();
                        } else {

                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private Emitter.Listener onGetData_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String dataStr = data.getString("dataResult");
                        viewData(dataStr);

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private void displayShockPoint() {
        mMap.clear();
        for (ShockPointEntity shockPoint : shockPointList) {
            shockingPointMarker(shockPoint);
        }
        //Move to first shock point
        if(!shockPointList.isEmpty()){
            ShockPointEntity shockPoint = shockPointList.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(shockPoint.getLatitude(),
                            shockPoint.getLongitude()), WIDE_ZOOM));
        }
    }

    private void displayShockPointInfo(final ShockPointEntity shockPoint){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View shockPointInfoView = inflater.inflate(R.layout.display_shock_point_info, null);
        TextView infoTextView = shockPointInfoView.findViewById(R.id.textView_shock_point_info);
        Button deleteButton = shockPointInfoView.findViewById(R.id.btn_delete_shock_point);
        Button viewDataButton = shockPointInfoView.findViewById(R.id.btn_view_data);

        infoTextView.setText(shockPoint.toString());
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogShockPointInfo.dismiss();
                deleteShockPoint(shockPoint);
            }
        });

        viewDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewData(shockPoint);
            }
        });
        builder.setView(shockPointInfoView);
        dialogShockPointInfo = builder.create();
        dialogShockPointInfo.show();
    }

    private void viewData(ShockPointEntity shockPoint){
        dialogShockPointInfo.dismiss();
        mSocket.emit("viewData", shockPoint.getId());
        mSocket.on(GET_DATA_EVENT_SOCKET, onGetData_Results);
    }

    private void viewData(String data) {
        Intent intent = new Intent(this, ViewDataActivity.class);
        intent.putExtra("data", data);
        startActivityForResult(intent, Common.VIEW_DATA_REQUEST_CODE);
    }

    private void deleteShockPoint(ShockPointEntity shockPointEntity){
        mSocket.emit("idPointDelete", shockPointEntity.getId());
        shockPointList.remove(shockPointEntity);
        displayShockPoint();
        Toast.makeText(this, "deleted shock point id " + shockPointEntity.getId(), Toast.LENGTH_LONG).show();
    }

    private void shockingPointMarker(ShockPointEntity shockingPoint) {
        LatLng pointLatLng = new LatLng(shockingPoint.getLatitude(), shockingPoint.getLongitude());
        Marker pointMarker = mMap.addMarker(new MarkerOptions()
                .position(pointLatLng)
                .title(SHOCK_POINT_MARKER_TITTLE)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shock_point)));
        pointMarker.setTag(shockingPoint);
        markerList.add(pointMarker);
    }

    private void alertNoRoadAvailable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.title_road_empty)
                .setTitle(R.string.message_road_empty)
                .setNeutralButton(R.string.ok_button, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void alertNoPointAvailable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.title_road_empty)
                .setTitle(R.string.message_point_empty)
                .setNeutralButton(R.string.ok_button, null);
        AlertDialog dialog = builder.create();
        dialog.show();
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(TextUtils.equals(marker.getTitle(), SHOCK_POINT_MARKER_TITTLE)){
                    ShockPointEntity shockPoint = (ShockPointEntity) marker.getTag();
                    displayShockPointInfo(shockPoint);
                }
                return false;
            }
        });

        getDeviceLocation();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            Task locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location myLocation = task.getResult();
                        LatLng currentLocation = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(myLocation.getLatitude(),
                                        myLocation.getLongitude()), DEFAULT_ZOOM));
                    } else {
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    protected void onDestroy() {
        if(mSocket.hasListeners(GET_ROAD_EVENT_SOCKET)){
            mSocket.off(GET_ROAD_EVENT_SOCKET);
        }
        if(mSocket.hasListeners(GET_POINT_EVENT_SOCKET)){
            mSocket.off(GET_POINT_EVENT_SOCKET);
        }
        if(mSocket.hasListeners(GET_DATA_EVENT_SOCKET)){
            mSocket.off(GET_DATA_EVENT_SOCKET);
        }
        super.onDestroy();
    }
}
