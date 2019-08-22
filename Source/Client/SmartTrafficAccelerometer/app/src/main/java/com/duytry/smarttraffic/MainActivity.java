package com.duytry.smarttraffic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.duytry.smarttraffic.common.Common;
import com.duytry.smarttraffic.common.MySocketFactory;
import com.duytry.smarttraffic.fragment.MyDialogFragment;
import com.duytry.smarttraffic.fragment.ViewDataFragment;
import com.duytry.smarttraffic.modules.AddressModule.AddressFinder;
import com.duytry.smarttraffic.modules.AddressModule.AddressFinderListener;
import com.duytry.smarttraffic.modules.RoadModule.SnapPointFinder;
import com.duytry.smarttraffic.modules.RoadModule.SnapPointFinderListener;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener, SnapPointFinderListener, AddressFinderListener {
    private static final int REQUEST_PERMISSION_REQUEST_CODE = 1000;
    private static final int INITIAL_REQUEST = 1337;
    private static final int MAX_RESOLUTION_MANUAL = 1000;
    private static final int MAX_TIME_AUTO_SAVE_DATA = 60000;
    private static final String TAG = "MainActivity";
    private static final String MODE_AUTO = "0";
    private static final String MODE_MANUAL = "1";
    private static final int MAP_REQUEST_CODE = 1999;
    private static final int CHOOSE_FILE_MESS_CODE = 02;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    ViewDataFragment viewDataFragment;
    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>();
    private MutableLiveData<Boolean> isRecording = new MutableLiveData<>();
    private static ArrayList<String> lineData = new ArrayList<>();
    private static ArrayList<String> lineDataTemp = new ArrayList<>();
//    private static int count = 0;
    private static int fileCount = 0;
    private static int fileCountNumber = 0;


    private static String dataDirectory;

    private Button btnStop, btnResume, btnRoadName;
    private Button btnShockPoint, btnStartRecord, btnStopRecord;
    private Spinner spinnerSpeed;
    private TextView textViewUserInfo;

    private SharedPreferences userInformation;
    SimpleDateFormat simpleDateFormat;
    DecimalFormat df = new DecimalFormat("0.0000");

    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private Socket mSocket = MySocketFactory.getInstance().getMySocket();
    private Socket mSocket2 = MySocketFactory.getInstance().getMyBackUpSocket();
    Timer autoRecordingTimer = new Timer();



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request permission
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_REQUEST_CODE);
        }

        mLocationPermissionGranted = true;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //turn on GPS
        turnOnGPS();
        startLocationUpdates();

        //get sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        if (sensors.isEmpty()) {
            sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (sensors.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.sensor_error_tittle);
                builder.setMessage(R.string.sensor_error_message);
                builder.setCancelable(true);
                builder.setNegativeButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        } else {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
        if (accelerometer != null) {
            isRunning.postValue(true);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

//        isRecording.postValue(false);

        simpleDateFormat = new SimpleDateFormat(Common.DATE_INPUT_FORMAT);
        userInformation = getSharedPreferences(Common.PREFERENCES, MODE_PRIVATE);

//        count = 0;
        fileCount = 0;
        fileCountNumber = 0;
        //init layout
        initLayout();

        //create folder to save data
        this.dataDirectory = makeDirectory();
        checkOldData();

        //init graph
//        initChart();

//        startDate = new Date();
    }

    private void checkOldData() {
        final File fileDirectory = new File(dataDirectory);
        //check old data
        if (fileDirectory != null && fileDirectory.exists()) {
            final String[] oldDatas = fileDirectory.list();
            if (oldDatas.length >= 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.overwrite_data_warning);
                builder.setMessage(R.string.overwrite_data_warning_message);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (String oldFileStr : oldDatas
                        ) {
                            File oldFile = new File(fileDirectory.getAbsolutePath() + File.separator + oldFileStr);
                            boolean deleted = oldFile.delete();
                            if (!deleted) {
                                Toast.makeText(MainActivity.this, "Delete file error", Toast.LENGTH_LONG).show();
                            }
                        }

                    }
                });
                builder.setNegativeButton(R.string.keep_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog dlg = builder.create();
                dlg.show();
            }
        }
    }

    private void sendFileToServer(String fileName, String mode) {

        File file = new File(dataDirectory + File.separator + fileName);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "send file to server error");
            Toast.makeText(this, Common.OPEN_FILE_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        if (inputStream != null) {
            byte[] data = new byte[(int) file.length()];
            try {
                inputStream.read(data);
                inputStream.close();
                if(TextUtils.equals(mode, Common.AUTO_ACTION)){
                    mSocket2.emit("filename2", fileName);
                    mSocket2.emit("data2", new String(data, "UTF-8"));
                } else {
                    mSocket.emit("filename", fileName);
                    mSocket.emit("data", new String(data, "UTF-8"));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Init layout
     */
    private void initLayout() {
        //stop button
        btnStop = (Button) findViewById(R.id.stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSensor();
            }
        });

        //resume button
        btnResume = (Button) findViewById(R.id.resume);
        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeSensor();
            }
        });

//        //save button
//        btnSave = (Button) findViewById(R.id.btn_push_data);
//        btnSave.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pushDataToServer2();
//            }
//        });
//
//        //open button
//        btnOpen = (Button) findViewById(R.id.open);
//        btnOpen.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopSensor();
//                loadData();
//            }
//        });



        //dropdown select speed
        spinnerSpeed = (Spinner) findViewById(R.id.spinner_speed);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.speeds_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(adapter);

        //shock point button
        btnShockPoint = (Button) findViewById(R.id.btn_shock_point);
        btnShockPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = saveData(Common.SHOCK_POINT_ACTION, MODE_MANUAL);
//                beginSnapPoint(path);
            }
        });

//        //speed up button
//        btnSpeedUp = (Button) findViewById(R.id.btn_speed_up);
//        btnSpeedUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Button Speed Up clicked");
//                saveData(Common.SPEED_UP_ACTION, MODE_MANUAL);
//            }
//        });
//
//        //brake down button
//        btnBrakeDown = (Button) findViewById(R.id.btn_brake_down);
//        btnBrakeDown.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Button Brake down clicked");
//                saveData(Common.BRAKE_DOWN_ACTION, MODE_MANUAL);
//            }
//        });
//
//        //parking button
//        btnParking = (Button) findViewById(R.id.btn_parking);
//        btnParking.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Button Parking clicked");
//                saveData(Common.PARKING_ACTION, MODE_MANUAL);
//            }
//        });

//        //finish button
//        btnFinish = (Button) findViewById(R.id.btn_finish);
//        btnFinish.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });

        btnStartRecord = (Button) findViewById(R.id.btn_start_record);
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording.postValue(true);
            }
        });

        btnStopRecord = (Button) findViewById(R.id.btn_stop_record);
        btnStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording.postValue(false);
            }
        });

        isRecording.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isRecording) {
                if (isRecording == null) {
                    return;
                }
                if (isRecording) {
                    startAutoRecording();
                    btnStartRecord.setVisibility(View.GONE);
                    btnStopRecord.setVisibility(View.VISIBLE);
                } else {
                    stopAutoRecording();
                    btnStartRecord.setVisibility(View.VISIBLE);
                    btnStopRecord.setVisibility(View.GONE);
                }
            }
        });

//        //manage button
//        btnManage = (Button) findViewById(R.id.btn_manage);
//        btnManage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                goToMapsActivity();
//            }
//        });

        btnRoadName = (Button) findViewById(R.id.btn_road_name);
        btnRoadName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRoadName();
            }
        });

        isRunning.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isRunning) {
                if (isRunning == null) {
                    return;
                }
                if (isRunning) {
                    btnStop.setVisibility(View.VISIBLE);
                    btnResume.setVisibility(View.GONE);
                } else {
                    btnStop.setVisibility(View.GONE);
                    btnResume.setVisibility(View.VISIBLE);
                }
            }
        });

        //user info
        updateUserInfo();
        viewDataFragment = (ViewDataFragment) getSupportFragmentManager().findFragmentById(R.id.view_data_fragment);
        viewDataFragment.initChart();


    }

    private void openFile(){
        stopSensor();
        loadData();
    }

    private void updateNameInfo(){
        String name = userInformation.getString(Common.NAME_PREFERENCES_KEY, Common.UNDEFINED);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.alizarin)));
        actionBar.setIcon(R.mipmap.ic_user);
        actionBar.setTitle(name);
    }

    private void updateRoadInfo(){
        String shortNameRoad = userInformation.getString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        String longNameRoad = userInformation.getString(Common.LONG_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        StringBuilder roadName = new StringBuilder();
        roadName.append("Road: ");
        roadName.append(shortNameRoad);
        if(!TextUtils.equals(shortNameRoad, longNameRoad)){
            roadName.append(Common.SPACE_CHARACTER);
            roadName.append(Common.DASH_CHARACTER);
            roadName.append(Common.SPACE_CHARACTER);
            roadName.append(longNameRoad);
        }
        textViewUserInfo = (TextView) findViewById(R.id.textView_user_info);
        textViewUserInfo.setText(roadName.toString());
    }

    private void updateUserInfo() {
        updateNameInfo();
        updateRoadInfo();
    }

    private void stopSensor() {
        sensorManager.unregisterListener(MainActivity.this);
        isRunning.postValue(false);
    }

    private void resumeSensor() {
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        isRunning.postValue(true);
    }

    private void loadData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CHOOSE_FILE_MESS_CODE);
    }

    private void addEndtryX(SensorEvent event) {
        viewDataFragment.addEndtry(event, 0);
    }

    private void addEndtryY(SensorEvent event) {
        viewDataFragment.addEndtry(event, 1);
    }

    private void addEndtryZ(SensorEvent event) {
        viewDataFragment.addEndtry(event, 2);
    }

    private void addLine(SensorEvent event) {
        if (lineData == null) {
            lineData = new ArrayList<>();
        }
        float xValue = event.values[0];
        String xStrValue = df.format(xValue);
        float yValue = event.values[1];
        String yStrValue = df.format(yValue);
        float zValue = event.values[2];
        String zStrValue = df.format(zValue);

        String latValue;
        String lngValue;
        if(mLastKnownLocation != null){
            latValue = String.valueOf((double) Math.round(mLastKnownLocation.getLatitude() * 100000) / 100000);
            lngValue = String.valueOf((double) Math.round(mLastKnownLocation.getLongitude() * 100000) / 100000);
        } else {
            latValue = String.valueOf(0);
            lngValue = String.valueOf(0);
        }

        Date date = new Date();
        String timeValue = simpleDateFormat.format(date);

        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append(xStrValue);
        lineBuilder.append(Common.SPACE_CHARACTER);
        lineBuilder.append(yStrValue);
        lineBuilder.append(Common.SPACE_CHARACTER);
        lineBuilder.append(zStrValue);
        lineBuilder.append(Common.SPACE_CHARACTER);
        lineBuilder.append(latValue);
        lineBuilder.append(Common.SPACE_CHARACTER);
        lineBuilder.append(lngValue);
        lineBuilder.append(Common.SPACE_CHARACTER);
        lineBuilder.append(timeValue);
//        lineBuilder.append(Common.SPACE_CHARACTER);
//        lineBuilder.append(count++);

        synchronized (lineData) {
            lineData.add(lineBuilder.toString());
        }

    }

//    private void addLocation(){
//        if (locationData == null) {
//            locationData = new ArrayList<>();
//        }
//        if(mLastKnownLocation == null){
//            locationData.add(new MyLocation(0, 0));
//        } else {
//            locationData.add(new MyLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
//        }
//
//    }
//
//    private void addTime(){
//        if (timeData == null) {
//            timeData = new ArrayList<>();
//        }
//        Date date = new Date();
//        timeData.add(simpleDateFormat.format(date));
//    }

    private void showMessageSendFileSuccess(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        MyDialogFragment dialogFragment = new MyDialogFragment();
        dialogFragment.setTittle(getString(R.string.push_data_success_tittle));
        dialogFragment.setMessage(getString(R.string.push_data_success_message));
        dialogFragment.show(fragmentManager, "Save data success");
    }

    private void pushDataToServer() {
        if (!mSocket.connected()) {
            mSocket.connect();
        }
        String road = userInformation.getString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        mSocket.emit("road", road);
        File folder = new File(dataDirectory);
        String[] files = folder.list();
        for (String file : files) {
            String[] fileNameStrArr = file.split(Common.UNDERLINED_CHARACTER);
            if(fileNameStrArr.length > 2 && TextUtils.equals(fileNameStrArr[1], Common.SHOCK_POINT_ACTION)){
                sendFileToServer(file, fileNameStrArr[1]);
            }
        }
        mSocket.emit("end_send_file", "true");
        showMessageSendFileSuccess();
    }

    private void pushDataToServer2() {
        if (!mSocket2.connected()) {
            mSocket2.connect();
        }
        mSocket2.emit("startSent", "startSent");
        String road = userInformation.getString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        mSocket2.emit("roadname2", road);
        File folder = new File(dataDirectory);
        String[] files = folder.list();

        for (String file : files) {
            String[] fileNameStrArr = file.split(Common.UNDERLINED_CHARACTER);
            fileCountNumber = files.length;
            fileCount = 0;
            if(fileNameStrArr.length > 2 ){
                beginSnapPoint(file, fileNameStrArr[1]);
//                sendFileToServer(file, fileNameStrArr[1]);
            }
        }
//        mSocket2.emit("endSent", "true");
//        showMessageSendFileSuccess();
    }

    /**
     * Create file name to save data
     *
     * @param action
     * @param speed
     * @return filename
     */
    private String makeFileName(String action, String speed) {
        String name = userInformation.getString(Common.NAME_PREFERENCES_KEY, Common.UNDEFINED);
        Date currentTime = Calendar.getInstance().getTime();
        String time = simpleDateFormat.format(currentTime);

        StringBuilder fileName = new StringBuilder();
        fileName.append(name);
        fileName.append(Common.UNDERLINED_CHARACTER);
        fileName.append(action);
        fileName.append(Common.UNDERLINED_CHARACTER);
        fileName.append(removeSlash(speed));
        fileName.append(Common.UNDERLINED_CHARACTER);
        fileName.append(time);
        fileName.append(Common.FILENAME_EXTENSION);

        return fileName.toString();
    }

    /**
     * remove slash in speed
     *
     * @param input
     * @return speed without slash
     */
    private String removeSlash(String input) {
        String[] array = input.split("/");
        StringBuilder output = new StringBuilder();
        for (String item : array) {
            output.append(item);
        }
        return output.toString();
    }

    /**
     * Create folder to save data
     */
    private String makeDirectory() {
        String road = userInformation.getString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        StringBuilder directory = new StringBuilder();
        directory.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        directory.append(File.separator);
        directory.append(Common.FILENAME_DIRECTORY);
        File folder = new File(directory.toString());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        directory.append(File.separator);
        directory.append(road);
        folder = new File(directory.toString());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return directory.toString();
    }

    /**
     * save data to internal storage
     *
     * @param action
     * @return
     */
    private String saveData(String action, String saveMode) {
//        Date clickTime = new Date();
//        long time = clickTime.getTime() - startDate.getTime();
        String speed = spinnerSpeed.getSelectedItem().toString();
        if (TextUtils.isEmpty(speed)) {
            speed = Common.UNDEFINED;
        }
        String fileName = makeFileName(action, speed);
        Log.d(TAG, "Saving data to filename: " + fileName);

        if (TextUtils.isEmpty(dataDirectory)) {
            this.dataDirectory = makeDirectory();
        }
        File file = new File(dataDirectory, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            int resolution = 0;
            synchronized (lineData) {
                if(TextUtils.equals(saveMode, MODE_AUTO)){
                    resolution = lineData.size();
                    bw.write("" + resolution);
                    for (int i = lineData.size() - resolution; i < lineData.size(); i++) {
                        bw.newLine();
                        bw.write(saveMode);
                        bw.write(Common.SPACE_CHARACTER);
                        bw.write(lineData.get(i));
                    }
                } else {
                    resolution = Math.min(lineData.size() + lineDataTemp.size(), MAX_RESOLUTION_MANUAL);
                    bw.write("" + resolution);
                    int count = resolution;
                    if(resolution > lineData.size()){
                        for (int i = lineDataTemp.size() - (resolution - lineData.size()); i < lineDataTemp.size(); i++) {
                            bw.newLine();
                            bw.write(saveMode);
                            bw.write(Common.SPACE_CHARACTER);
                            bw.write(lineDataTemp.get(i));
                        }
                        count = lineData.size();
                    }
                    for (int i = lineData.size() - count; i < lineData.size(); i++) {
                        bw.newLine();
                        bw.write(saveMode);
                        bw.write(Common.SPACE_CHARACTER);
                        bw.write(lineData.get(i));
                    }
                }

                if(TextUtils.equals(saveMode, MODE_AUTO)){
                    lineDataTemp = (ArrayList<String>) lineData.clone();
                    lineData.clear();
                }

            }
            Log.d(TAG, "Saved data to filename: " + fileName);
//            Toast.makeText(this, "Saved with " + minCount + " " + time + " //" + time/minCount, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Saved with resolution " + resolution, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        String path = null;
        try {
            path = file.getPath();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intentData) {
        switch (requestCode) {
            case CHOOSE_FILE_MESS_CODE:
                if (resultCode == RESULT_OK) {
                    if (intentData != null) {
                        Uri uri = intentData.getData();
                        openFile(uri);
                    }
                }
                break;
            case MAP_REQUEST_CODE:
                if (resultCode == Common.VIEW_DATA_RESULT_CODE) {
                    String data = intentData.getStringExtra("data");
                    stopSensor();
                    viewDataFragment.viewDataFromString(data);
                }
                break;
            case Common.VIEW_DATA_REQUEST_CODE:
                stopSensor();
                break;
        }
    }

    private void openFile(Uri uri) {
        ContentResolver res = this.getContentResolver();
        try {
            InputStream inputStream = res.openInputStream(uri);
            if (inputStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder dataStr = new StringBuilder();
                    String strCurrentLine = "";
                    while ((strCurrentLine = br.readLine()) != null) {
                        dataStr.append(strCurrentLine);
                        dataStr.append(System.lineSeparator());
                    }
                    Intent intent = new Intent(this, ViewDataActivity.class);
                    intent.putExtra("data", dataStr.toString());
                    startActivityForResult(intent, Common.VIEW_DATA_REQUEST_CODE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException | NullPointerException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
        if (isRunning.getValue() != null && isRunning.getValue().booleanValue() == true) {
            //display graph
            addEndtryX(sensorEvent);
            addEndtryY(sensorEvent);
            addEndtryZ(sensorEvent);

            //collect data
            addLine(sensorEvent);
//            addLocation();
//            addTime();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        stopSensor();
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    /**
     * check permissions
     *
     * @param context
     * @param permissions
     * @return
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check gps, if turn off => turn on request.
     */
    private void turnOnGPS() {
        //get gps status
        String provider = Settings.Secure.
                getString(getContentResolver(),
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if (!provider.contains("gps")) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Smart Traffic");
            builder.setMessage("Do you want to turn on GPS?");
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
                    Toast.makeText(MainActivity.this, "You need to turn on GPS for location info!", Toast.LENGTH_LONG).show();
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationProviderClient.requestLocationUpdates(createLocationRequest(),
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
            }
        }

        ;
    };

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void goToMapsActivity() {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivityForResult(intent, MAP_REQUEST_CODE);
    }

    private void beginSnapPoint(String fileName, String mode) {
        File file = new File(dataDirectory + File.separator + fileName);

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder dataStr = new StringBuilder();
                String strCurrentLine = br.readLine();
                dataStr.append(strCurrentLine);
                dataStr.append(System.lineSeparator());
                double oldLatitude = -1;
                double oldLongitude = -1;
                List<LatLng> points = new ArrayList<>();
                while ((strCurrentLine = br.readLine()) != null) {
                    dataStr.append(strCurrentLine);
                    dataStr.append(System.lineSeparator());
                    String[] arrValues = strCurrentLine.split(Common.SPACE_CHARACTER);
                    double latitude = Double.valueOf(arrValues[4]);
                    double longitude = Double.valueOf(arrValues[5]);
                    if (latitude == oldLatitude && oldLongitude == longitude) {
                        continue;
                    }
                    LatLng point = new LatLng(latitude, longitude);
                    points.add(point);
                    oldLatitude = latitude;
                    oldLongitude = longitude;
                }
                if (!TextUtils.isEmpty(dataStr)) {
                    onSnapPointFinderStart(points, fileName, dataStr.toString(), mode);
                }
            } catch (IOException e) {
                Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

//        FileInputStream inputStream = null;
//        try {
//            inputStream = new FileInputStream(file);
//        } catch (FileNotFoundException e) {
//            Log.d(TAG, "send file to server error");
//            Toast.makeText(this, Common.OPEN_FILE_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
//            e.printStackTrace();
//        }
//
//        if (inputStream != null) {
//
//            byte[] data = new byte[(int) file.length()];
//            try {
//                inputStream.read(data);
//                inputStream.close();
//                if(TextUtils.equals(mode, Common.AUTO_ACTION)){
//                    mSocket2.emit("filename2", fileName);
//                    mSocket2.emit("data2", new String(data, "UTF-8"));
//                } else {
//                    mSocket.emit("filename", fileName);
//                    mSocket.emit("data", new String(data, "UTF-8"));
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

    }

    @Override
    public void onSnapPointFinderStart(List<LatLng> listInputPoints, String fileName, String data, String mode) {
        try {
            new SnapPointFinder(this, listInputPoints, fileName, data, mode).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSnapPointFinderSuccess(List<LatLng> listOutputPoints, String fileName, String data, String mode) {
        File file = new File(dataDirectory + File.separator + fileName);
        StringBuilder dataBuider = new StringBuilder(data);
        String[] dataLines = data.split(System.lineSeparator());
        int count = -1;
        double oldLatitude = -1;
        double oldLongitude = -1;
        for (int i = 1; i < dataLines.length; i++) {
            String[] arrValues = dataLines[i].split(Common.SPACE_CHARACTER);
            double latitude = Double.valueOf(arrValues[4]);
            double longitude = Double.valueOf(arrValues[5]);
            if (latitude == oldLatitude && oldLongitude == longitude) {
            } else {
                oldLatitude = latitude;
                oldLongitude = longitude;
                count++;
            }
            if(count < listOutputPoints.size() - 1){
                dataLines[i].replace(arrValues[4], String.valueOf((double) Math.round(listOutputPoints.get(count).latitude * 100000) / 100000));
                dataLines[i].replace(arrValues[5], String.valueOf((double) Math.round(listOutputPoints.get(count).longitude * 100000) / 100000));
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            dataBuider.append(dataLines[0]);
            bw.write(dataLines[0]);
            for (int i = 1; i < dataLines.length; i++) {
                bw.newLine();
                dataBuider.append(System.lineSeparator());
                bw.append(dataLines[i].toString());
                dataBuider.append(dataLines[i].toString());
            }
            if(TextUtils.equals(mode, Common.AUTO_ACTION)){
                mSocket2.emit("filename2", fileName);
                mSocket2.emit("data2", dataBuider.toString());
            } else {
                mSocket.emit("filename", fileName);
                mSocket.emit("data", dataBuider.toString());
            }
            this.fileCount++;
            if(this.fileCount == this.fileCountNumber){
                if(TextUtils.equals(mode, Common.AUTO_ACTION)){
                    mSocket2.emit("endSent", "true");
                } else {
                    mSocket.emit("end_send_file", "true");

                }
                showMessageSendFileSuccess();
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showRoadName() {
        AlertDialog dialogRoadInfo = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View roadInfoView = inflater.inflate(R.layout.display_get_road_information, null);
        final EditText shortNameET = (EditText) roadInfoView.findViewById(R.id.editText_short_name);
        final EditText longNameET = (EditText) roadInfoView.findViewById(R.id.editText_long_name);
        Button autoBtn = (Button) roadInfoView.findViewById(R.id.btn_auto_road);
        Button manualBtn = (Button) roadInfoView.findViewById(R.id.btn_manual_road);
        Button okBtn = (Button) roadInfoView.findViewById(R.id.btn_set_road_done);

        String shortNameRoad = userInformation.getString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        String longNameRoad = userInformation.getString(Common.LONG_NAME_ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        shortNameET.setText(shortNameRoad);
        longNameET.setText(longNameRoad);
        shortNameET.setEnabled(false);
        longNameET.setEnabled(false);

        builder.setView(roadInfoView);
        dialogRoadInfo = builder.create();
        dialogRoadInfo.show();

        final AlertDialog finalDialogRoadInfo = dialogRoadInfo;
        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoDetectRoadName(finalDialogRoadInfo);
            }
        });

        manualBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shortNameET.setEnabled(true);
                longNameET.setEnabled(true);
                shortNameET.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.showSoftInput(roadInfoView, InputMethodManager.SHOW_IMPLICIT);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor prefEditor = userInformation.edit();
                prefEditor.putString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, shortNameET.getText().toString());
                prefEditor.putString(Common.LONG_NAME_ROAD_PREFERENCES_KEY, longNameET.getText().toString());
                prefEditor.commit();
                finalDialogRoadInfo.dismiss();
                updateRoadInfo();
            }
        });
    }

    private void autoDetectRoadName(AlertDialog dialog) {
        if (mLastKnownLocation != null) {
            String mLatLng = String.valueOf(mLastKnownLocation.getLatitude()) + ", " + mLastKnownLocation.getLongitude();
            onAddressFinderStart(mLatLng, dialog);
        }
    }

    @Override
    public void onAddressFinderStart(String latlng, AlertDialog dialog) {
        try {
            new AddressFinder(latlng, this, dialog).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddressFinderSuccess(String currentLongNameRoad, String currentShortNameRoad, String address, AlertDialog dialog) {
        SharedPreferences.Editor prefEditor = userInformation.edit();
        prefEditor.putString(Common.SHORT_NAME_ROAD_PREFERENCES_KEY, currentShortNameRoad);
        prefEditor.putString(Common.LONG_NAME_ROAD_PREFERENCES_KEY, currentLongNameRoad);
        prefEditor.commit();
        dialog.dismiss();
        updateRoadInfo();
    }

    private void startAutoRecording(){
        synchronized (lineData){
            lineDataTemp = (ArrayList<String>) lineData.clone();
            lineData.clear();
        }
        //once a minute
        autoRecordingTimer = new Timer();
        autoRecordingTimer.schedule(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        saveData(Common.AUTO_ACTION, MODE_AUTO);
                    }
                });
            }
        }, MAX_TIME_AUTO_SAVE_DATA, MAX_TIME_AUTO_SAVE_DATA);
    }

    private void stopAutoRecording(){
        autoRecordingTimer.cancel();
        saveData(Common.AUTO_ACTION, MODE_AUTO);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_logout:
                finish();
                return true;
            case R.id.menu_open_file:
                openFile();
                return true;
            case R.id.menu_save_to_server:
                pushDataToServer2();
                return true;
            case R.id.menu_manage_roads:
                goToMapsActivity();
                return true;
            case R.id.menu_auto_mode:
                changeToAutoMode();
                return true;
            case R.id.menu_manual_mode:
                changeToManualMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeToAutoMode(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.peter_river)));
        this.btnShockPoint.setVisibility(View.GONE);
        this.btnStartRecord.setVisibility(View.VISIBLE);
    }

    private void changeToManualMode(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.alizarin)));
        if(isRecording.getValue() != null && isRecording.getValue()){
            isRecording.postValue(false);
        }
        this.btnStartRecord.setVisibility(View.GONE);
        this.btnStopRecord.setVisibility(View.GONE);
        this.btnShockPoint.setVisibility(View.VISIBLE);
    }
}
