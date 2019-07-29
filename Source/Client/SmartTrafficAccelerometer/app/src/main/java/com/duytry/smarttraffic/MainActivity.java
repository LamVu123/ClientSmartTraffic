package com.duytry.smarttraffic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.duytry.smarttraffic.common.Common;
import com.duytry.smarttraffic.entity.MyLocation;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private Queue dataExample;
    private static final int REQUEST_PERMISSION_REQUEST_CODE = 1000;
    private static final int INITIAL_REQUEST=1337;
    private static final int NUM_OF_ENTRY = 1000;
    private static final String DATE_FORMAT = "yyyyMMddhhmmss";
    private static final String TAG = "MainActivity";
    private static final int MESSAGE_REQUEST = 01;
    private EditText fileNameResult;
    private String fileNameToLoad;
    private String pathFileToLoad;
    private static final int CHOOSE_FILE_MESS_CODE = 02;
    private int mVisibleXRangeMaximum = 600;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private LineChart mChartX, mChartY, mChartZ;
    private boolean isRunning = true;
    private boolean onLoadFile = false;
    private int idLoadFile = 0;
    Intent myFileIntent;
    private static ArrayList<MyLocation> locationData;
    private static ArrayList<String> timeData;

    private static String dataDirectory;

    private Button btnOpen, btnStop, btnResume, btnSave;
    private Button btnFinish;
    private Button btnShockPoint, btnSpeedUp, btnBrakeDown, btnParking;
    private Spinner spinnerSpeed;
    private TextView textViewUserInfo;

    private View.OnClickListener stopListener, openListener, resumeListener, saveListener;
    private View.OnClickListener finishListener;
    private View.OnClickListener saveShockPointListener, saveSpeedUpListener, saveBrakeDownListener, saveParkingListener;
    private SharedPreferences userInformation;
    SimpleDateFormat simpleDateFormat;

    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;


//    Date startDate;

    //cuongvv add start
    private Socket mSocket;
    {

        try {
            mSocket = IO.socket(BuildConfig.SendDataIp);
        } catch (URISyntaxException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    //cuongvv add end

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

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_REQUEST_CODE);
        }

        mLocationPermissionGranted = true;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //turn on GPS
        turnOnGPS();
        startLocationUpdates();

        //get sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.sensor_error_tittle);
            builder.setMessage(R.string.sensor_error_message);
            builder.setCancelable(true);
            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if(accelerometer != null){
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        userInformation = getSharedPreferences(Common.PREFERENCES,MODE_PRIVATE);

        //init layout
        initLayout();

        //create folder to save data
        this.dataDirectory = makeDirectory();
        checkOldData();

        //init graph
        initChart();

        locationData = new ArrayList<>();
        timeData = new ArrayList<>();

        //cuongvv start
        mSocket.connect();
        //cuongvv end

//        startDate = new Date();
    }

    private void checkOldData(){
        final File fileDirectory = new File(dataDirectory);
        //check old data
        if(fileDirectory != null && fileDirectory.exists()){
            final String[] oldDatas = fileDirectory.list();
            if(oldDatas.length >= 1){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.overwrite_data_warning);
                builder.setMessage(R.string.overwrite_data_warning_message);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for ( String oldFileStr : oldDatas
                        ) {
                            File oldFile = new File(fileDirectory.getAbsolutePath() + File.separator + oldFileStr);
                            boolean deleted = oldFile.delete();
                            if(!deleted){
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
//cuongvv start
    private void sendFileToServer(String filePath){

//        mSocket.emit("image", road + File.separator + filePath);

        mSocket.emit("filename", filePath);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dataDirectory + File.separator + filePath);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "send file to server error");
            Toast.makeText(this, Common.OPEN_FILE_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        StringBuilder data = new StringBuilder();
        if(inputStream != null){
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String strCurrentLine;
                while ((strCurrentLine = br.readLine()) != null) {
                    data.append(strCurrentLine);
                    data.append(System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mSocket.emit("data", data.toString());

    }
//cuongvv end
/*    //cuongvv start
    private Emitter.Listener onMessage_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String table;
                    try{
                        table = data.getString("table");
                        Toast.makeText(getApplicationContext(),table,Toast.LENGTH_SHORT).show();
                    }catch(JSONException e){
                        return;
                    }
                }
            });
        }
    };
    //cuongvv end
*/
    /**
     * Init layout
     */
    private void initLayout(){
        //stop button
        btnStop = (Button) findViewById(R.id.stop);
        stopListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Stop clicked");
                sensorManager.unregisterListener(MainActivity.this);
                isRunning = false;
            }
        };
        btnStop.setOnClickListener(stopListener);

        //resume button
        btnResume = (Button) findViewById(R.id.resume);
        resumeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Resume clicked");
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                isRunning = true;
            }
        };
        btnResume.setOnClickListener(resumeListener);

        //save button
        btnSave = (Button) findViewById(R.id.btn_push_data);
        saveListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushDataToServer();
            }
        };
        btnSave.setOnClickListener(saveListener);

        //open button
        btnOpen = (Button) findViewById(R.id.open);
        openListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Open clicked");
                sensorManager.unregisterListener(MainActivity.this);
                loadData();
            }
        };
        btnOpen.setOnClickListener(openListener);

        //dropdown select speed
        spinnerSpeed = (Spinner) findViewById(R.id.spinner_speed);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.speeds_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(adapter);

        //shock point button
        btnShockPoint = (Button) findViewById(R.id.btn_shock_point);
        saveShockPointListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Shock Point clicked");
                String file = saveData(Common.SHOCK_POINT_ACTION);
            }
        };
        btnShockPoint.setOnClickListener(saveShockPointListener);

        //speed up button
        btnSpeedUp = (Button) findViewById(R.id.btn_speed_up);
        saveSpeedUpListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Speed Up clicked");
                saveData(Common.SPEED_UP_ACTION);
            }
        };
        btnSpeedUp.setOnClickListener(saveSpeedUpListener);

        //brake down button
        btnBrakeDown = (Button) findViewById(R.id.btn_brake_down);
        saveBrakeDownListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Brake down clicked");
                saveData(Common.BRAKE_DOWN_ACTION);
            }
        };
        btnBrakeDown.setOnClickListener(saveBrakeDownListener);

        //parking button
        btnParking = (Button) findViewById(R.id.btn_parking);
        saveParkingListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Parking clicked");
                saveData(Common.PARKING_ACTION);
            }
        };
        btnParking.setOnClickListener(saveParkingListener);

        //finish button
        btnFinish = (Button) findViewById(R.id.btn_finish);
        finishListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Finish clicked");
                MainActivity.this.finish();
            }
        };
        btnFinish.setOnClickListener(finishListener);

        //user info
        String name = userInformation.getString(Common.NAME_PREFERENCES_KEY, Common.UNDEFINED);
        String road = userInformation.getString(Common.ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        StringBuilder userInfo = new StringBuilder();
        userInfo.append(name);
        userInfo.append(Common.SPACE_CHARACTER);
        userInfo.append(Common.DASH_CHARACTER);
        userInfo.append(Common.SPACE_CHARACTER);
        userInfo.append(road);

        textViewUserInfo = (TextView) findViewById(R.id.textView_user_info);
        textViewUserInfo.setText(userInfo.toString());
    }

    /*
    init chart graph for accelerometer status view
     */
    private void initChart(){
        mChartX = (LineChart) findViewById(R.id.chartX);
        mChartX.getDescription().setText("Real time accelerometer Data Plot X");
        mChartX.setTouchEnabled(true);
        mChartX.setDragEnabled(true);
        mChartX.setScaleEnabled(true);
        mChartX.setDrawGridBackground(true);
        mChartX.setPinchZoom(true);
        mChartX.setBackgroundColor(Color.WHITE);

        mChartY = (LineChart) findViewById(R.id.chartY);
        mChartY.getDescription().setText("Real time accelerometer Data Plot Y");
        mChartY.setTouchEnabled(true);

        mChartY.setDragEnabled(true);
        mChartY.setScaleEnabled(true);
        mChartY.setDrawGridBackground(true);
        mChartY.setPinchZoom(true);
        mChartY.setBackgroundColor(Color.WHITE);

        mChartZ = (LineChart) findViewById(R.id.chartZ);
        mChartZ.getDescription().setText("Real time accelerometer Data Plot Z");
        mChartZ.setTouchEnabled(true);
        mChartZ.setDragEnabled(true);
        mChartZ.setScaleEnabled(true);
        mChartZ.setDrawGridBackground(true);
        mChartZ.setPinchZoom(true);
        mChartZ.setBackgroundColor(Color.WHITE);

        LineData dataX = new LineData();
        dataX.setValueTextColor(Color.RED);
        mChartX.setData(dataX);
        LineData dataY = new LineData();
        dataY.setValueTextColor(Color.GREEN);
        mChartY.setData(dataY);
        LineData dataZ = new LineData();
        dataZ.setValueTextColor(Color.BLUE);
        mChartZ.setData(dataZ);

        Legend legendX = mChartX.getLegend();
        legendX.setForm(Legend.LegendForm.LINE);
        legendX.setTextColor(Color.WHITE);

        Legend legendY = mChartY.getLegend();
        legendY.setForm(Legend.LegendForm.LINE);
        legendY.setTextColor(Color.WHITE);

        Legend legendZ = mChartZ.getLegend();
        legendZ.setForm(Legend.LegendForm.LINE);
        legendZ.setTextColor(Color.WHITE);

        XAxis xl = mChartX.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawAxisLine(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        xl = mChartY.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawAxisLine(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        xl = mChartZ.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawAxisLine(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChartX.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        leftAxis = mChartY.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        leftAxis = mChartZ.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChartX.getAxisRight();
        rightAxis.setEnabled(false);

        rightAxis = mChartY.getAxisRight();
        rightAxis.setEnabled(false);

        rightAxis = mChartZ.getAxisRight();
        rightAxis.setEnabled(false);

        mChartX.getAxisLeft().setDrawGridLines(false);
        mChartX.getXAxis().setDrawGridLines(false);
        mChartX.setDrawBorders(false);
        mChartY.getAxisLeft().setDrawGridLines(false);
        mChartY.getXAxis().setDrawGridLines(false);
        mChartY.setDrawBorders(false);
        mChartZ.getAxisLeft().setDrawGridLines(false);
        mChartZ.getXAxis().setDrawGridLines(false);
        mChartZ.setDrawBorders(false);
    }

    private void loadData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CHOOSE_FILE_MESS_CODE);
        sensorManager.unregisterListener(MainActivity.this);

    }

    /**
     * add data to graph viewer
     * @param event
     * @param axe
     */
    private void addEndtry(SensorEvent event, int axe) {
        LineChart mchart = null;
        switch (axe){
            case 0: mchart = mChartX;
                break;
            case 1: mchart = mChartY;
                break;
            case 2: mchart = mChartZ;
                break;
            default: break;
        }

        LineData data = mchart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                switch (axe){
                    case 0: set = createSet(Color.RED);
                        break;
                    case 1: set = createSet(Color.BLUE);
                        break;
                    case 2: set = createSet(Color.GREEN);
                        break;
                    default: break;
                }
                data.addDataSet(set);
            }

            Entry entry = new Entry(set.getEntryCount(), event.values[axe] + 5);
            data.addEntry(entry, 0);
            LineData tmpData = mchart.getData();

            data.notifyDataChanged();
            String description = "";
            switch (axe){
                case 0: description = "x: ";
                    break;
                case 1: description = "y: ";
                    break;
                case 2: description = "z: ";
                    break;
                default: break;
            }
            mchart.getDescription().setText(description + event.values[axe]);
            mchart.notifyDataSetChanged();
            mchart.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mchart.moveViewToX(data.getEntryCount());
        }
    }

    private void addEndtryX(SensorEvent event) {
        addEndtry(event, 0);
    }

    private void addEndtryY(SensorEvent event) {
        addEndtry(event, 1);
    }

    private void addEndtryZ(SensorEvent event) {
        addEndtry(event, 2);
    }

    private void addLocation(){
        if (locationData == null) {
            locationData = new ArrayList<>();
        }
        if(mLastKnownLocation == null){
            locationData.add(new MyLocation(0, 0));
        } else {
            locationData.add(new MyLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
        }

    }

    private void addTime(){
        if (timeData == null) {
            timeData = new ArrayList<>();
        }
        Date date = new Date();
        timeData.add(simpleDateFormat.format(date));
    }

    private LineDataSet createSet(int color){
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(color);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void pushDataToServer() {
        String road = userInformation.getString(Common.ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        mSocket.emit("road", road);
        File folder = new File(dataDirectory);
        String[] files = folder.list();
        for ( String file : files ) {
            sendFileToServer(file);
        }
        mSocket.emit("end_send_file", "true");

    }

    /**
     * Create file name to save data
     * @param action
     * @param speed
     * @return filename
     */
    private String makeFileName(String action, String speed){
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
     * @param input
     * @return speed without slash
     */
    private String removeSlash(String input){
        String[]array = input.split("/");
        StringBuilder output = new StringBuilder();
        for(String item : array){
            output.append(item);
        }
        return output.toString();
    }

    /**
     * Create folder to save data
     */
    private String makeDirectory(){
        String road = userInformation.getString(Common.ROAD_PREFERENCES_KEY, Common.UNDEFINED);
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
     * @param action
     * @return
     */
    private String saveData(String action){
//        Date clickTime = new Date();
//        long time = clickTime.getTime() - startDate.getTime();
        String speed = spinnerSpeed.getSelectedItem().toString();
        if(TextUtils.isEmpty(speed)){
            speed = Common.UNDEFINED;
        }
        String fileName = makeFileName(action, speed);
        Log.d(TAG, "Saving data to filename: " + fileName);

        if(TextUtils.isEmpty(dataDirectory)){
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
            LineData xData = mChartX.getData();
            LineData yData = mChartY.getData();
            LineData zData = mChartZ.getData();
            int minEntryCount = Math.min(xData.getEntryCount(), yData.getEntryCount());
            minEntryCount = Math.min(minEntryCount, zData.getEntryCount());
            int minCount = Math.min(minEntryCount, NUM_OF_ENTRY);
            DecimalFormat df = new DecimalFormat("0.0000");

            //first row print number of point
            bw.write(String.valueOf(minCount));
            bw.newLine();

            String saveMode = "1";

            for (int i = xData.getEntryCount() - minCount; i < xData.getEntryCount() ; i++) {
                float xValue = (float) (xData.getDataSetByIndex(0).getEntryForIndex(i).getY() - 5);
                String xStrValue = df.format(xValue);
                float yValue = (float) (yData.getDataSetByIndex(0).getEntryForIndex(i).getY() - 5);
                String yStrValue = df.format(yValue);
                float zValue = (float) (zData.getDataSetByIndex(0).getEntryForIndex(i).getY() - 5);
                String zStrValue = df.format(zValue);

                String strLatitudeValue = String.valueOf(locationData.get(i).getLatitude());
                String strLongitudeValue = String.valueOf(locationData.get(i).getLongitude());

                String strTime = timeData.get(i);

                bw.write(saveMode);
                bw.write(Common.SPACE_CHARACTER);

                bw.write(xStrValue.replace(",", "."));
                bw.write(Common.SPACE_CHARACTER);

                bw.write(yStrValue.replace(",", "."));
                bw.write(Common.SPACE_CHARACTER);

                bw.write(zStrValue.replace(",", "."));
                bw.write(Common.SPACE_CHARACTER);

                bw.write(strLatitudeValue);
                bw.write(Common.SPACE_CHARACTER);

                bw.write(strLongitudeValue);
                bw.write(Common.SPACE_CHARACTER);

                bw.write(strTime);
                bw.write(Common.SPACE_CHARACTER);

                bw.write(Common.SEMICOLON_CHARACTER);
                bw.newLine();
            }

            Log.d(TAG, "Saved data to filename: " + fileName);
//            Toast.makeText(this, "Saved with " + minCount + " " + time + " //" + time/minCount, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Saved with resolution " + minCount, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        String path = null;
        try {
            path = file.getPath();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intentData) {
        switch (requestCode) {
            case CHOOSE_FILE_MESS_CODE:
                if (resultCode == RESULT_OK) {
                    openFile(intentData);
                }
                break;
        }
    }

    private void openFile(@Nullable Intent intentData){
        sensorManager.unregisterListener(MainActivity.this);
         isRunning = false;
         if (intentData != null) {
             Uri uri = intentData.getData();
             ContentResolver res = this.getContentResolver();
             try {
                 InputStream inputStream = res.openInputStream(uri);
                 if(inputStream != null){
                     try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                         String strCurrentLine = br.readLine();
                         int pointNumber = Integer.parseInt(strCurrentLine);
                         int i = 0;

                         mChartX.clearValues();
                         LineData dataX = mChartX.getData();
                         ILineDataSet setX = null;
                         if (dataX != null) {
                             setX = dataX.getDataSetByIndex(0);
                             if (setX == null) {
                                 setX = createSet(Color.RED);
                                 dataX.addDataSet(setX);
                             }
                         }
                         mChartY.clearValues();
                         LineData dataY = mChartY.getData();
                         ILineDataSet setY = null;
                         if (dataY != null) {
                             setY = dataY.getDataSetByIndex(0);
                             if (setY == null) {
                                 setY = createSet(Color.BLUE);
                                 dataY.addDataSet(setY);
                             }
                         }
                         mChartZ.clearValues();
                         LineData dataZ = mChartZ.getData();
                         ILineDataSet setZ = null;
                         if (dataZ != null) {
                             setZ = dataZ.getDataSetByIndex(0);
                             if (setZ == null) {
                                 setZ = createSet(Color.GREEN);
                                 dataZ.addDataSet(setZ);
                             }
                         }

                         while ((strCurrentLine = br.readLine()) != null) {
                             String[] arrValues = strCurrentLine.split(Common.SPACE_CHARACTER);
                             if(arrValues.length != 8){
                                 Toast.makeText(this, Common.WRONG_FILE_FORMAT_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                                 return;
                             }
                             dataX.addEntry(new Entry(setX.getEntryCount(), Float.parseFloat(arrValues[1])+5), 0);
                             dataY.addEntry(new Entry(setY.getEntryCount(), Float.parseFloat(arrValues[2])+5), 0);
                             dataZ.addEntry(new Entry(setZ.getEntryCount(), Float.parseFloat(arrValues[3])+5), 0);
                             i++;
                         }
                         dataX.notifyDataChanged();
                         mChartX.notifyDataSetChanged();
                         mChartX.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                         mChartX.moveViewToX(dataX.getEntryCount());
                         dataY.notifyDataChanged();
                         mChartY.notifyDataSetChanged();
                         mChartY.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                         mChartY.moveViewToX(dataY.getEntryCount());
                         dataZ.notifyDataChanged();
                         mChartZ.notifyDataSetChanged();
                         mChartZ.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                         mChartZ.moveViewToX(dataZ.getEntryCount());
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 }
             } catch (FileNotFoundException | NullPointerException e) {
                 Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                 e.printStackTrace();
             } catch (NumberFormatException e){
                 Toast.makeText(this, Common.WRONG_FILE_FORMAT_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                 e.printStackTrace();
             }
         }
    }


    @Override
    public final void onAccuracyChanged (Sensor sensor,int accuracy){

    }

    @Override
    public final void onSensorChanged (SensorEvent sensorEvent){
        if(isRunning){
            addEndtryX(sensorEvent);
            addEndtryY(sensorEvent);
            addEndtryZ(sensorEvent);
            addLocation();
            addTime();
        }
    }

    @Override
    protected void onPause () {
        super.onPause();
    }


    @Override
    protected void onDestroy () {
        sensorManager.unregisterListener(MainActivity.this);
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off();
    }

    @Override
    protected void onResume () {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions,
    @NonNull int[] grantResults){
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
            default: break;
        }
    }

    /**
     * check permissions
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
                        } else {
                            Log.d(TAG, "Current location is null.");
                            Log.e(TAG, "Exception: %s", task.getException());
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
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
        };
    };

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

}
