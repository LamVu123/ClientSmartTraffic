package com.duytry.smarttraffic;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private Queue dataExample;
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

//    Date startDate;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
        }

        //get sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        userInformation = getSharedPreferences(Common.PREFERENCES,MODE_PRIVATE);

        //init layout
        initLayout();

        //create folder to save data
        makeDirectory();

        //init graph
        initChart();

//        startDate = new Date();
    }

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
//        btnSave = (Button) findViewById(R.id.save);
//        saveListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (threadX != null) {
//                    threadX.interrupt();
//                }
//                if (threadY != null) {
//                    threadY.interrupt();
//                }
//                if (threadZ != null) {
//                    threadZ.interrupt();
//                }
//                sensorManager.unregisterListener(MainActivity.this);
//                openSaveActivity();
//                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
//            }
//        };
//        btnSave.setOnClickListener(saveListener);

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
                saveData(Common.SHOCK_POINT_ACTION);
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

    private void addEndtryX(SensorEvent event) {

        LineData data = mChartX.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet(Color.RED);
                data.addDataSet(set);
            }
            // Log.d(TAG, "addEndtry: " + set.getEntryCount());
//            data.addEntry(new Entry(set.getEntryCount()));

            Entry entry = new Entry(set.getEntryCount(), event.values[0] + 5);
            data.addEntry(entry, 0);
            LineData tmpData = mChartX.getData();

            data.notifyDataChanged();
            mChartX.getDescription().setText("x: " + event.values[0]);
            mChartX.notifyDataSetChanged();
            mChartX.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mChartX.moveViewToX(data.getEntryCount());
        }
    }

    private void loadData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CHOOSE_FILE_MESS_CODE);
        sensorManager.unregisterListener(MainActivity.this);

    }


    private void addEndtryY(SensorEvent event) {
        LineData data = mChartY.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet(Color.BLUE);
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), event.values[1] + 5), 0);
            data.notifyDataChanged();
            mChartY.getDescription().setText("y: " + event.values[1]);
            mChartY.notifyDataSetChanged();
            mChartY.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mChartY.moveViewToX(data.getEntryCount());
        }
    }

    private void addEndtryZ(SensorEvent event) {
        LineData data = mChartZ.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet(Color.GREEN);
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), event.values[2] + 5), 0);
            data.notifyDataChanged();
            mChartZ.getDescription().setText("z: " + event.values[2]);
            mChartZ.notifyDataSetChanged();
            mChartZ.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mChartZ.moveViewToX(data.getEntryCount());

        }
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

    private void openSaveActivity() {
        Intent intent = new Intent(this, SaveActivity.class);
        startActivityForResult(intent, MESSAGE_REQUEST);
    }

    /**
     * Create file name to save data
     * @param action
     * @param speed
     * @return filename
     */
    private String makeFileName(String action, String speed){
        String name = userInformation.getString(Common.NAME_PREFERENCES_KEY, Common.UNDEFINED);
        String road = userInformation.getString(Common.ROAD_PREFERENCES_KEY, Common.UNDEFINED);
        Date currentTime = Calendar.getInstance().getTime();
        String time = simpleDateFormat.format(currentTime);

        StringBuilder fileName = new StringBuilder();
        fileName.append(name);
        fileName.append(Common.UNDERLINED);
        fileName.append(road);
        fileName.append(Common.UNDERLINED);
        fileName.append(action);
        fileName.append(Common.UNDERLINED);
        fileName.append(removeSlash(speed));
        fileName.append(Common.UNDERLINED);
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
    private void makeDirectory(){
        StringBuilder directory = new StringBuilder();
        directory.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        directory.append(File.separator);
        directory.append(Common.FILENAME_DIRECTORY);
        this.dataDirectory = directory.toString();
        File folder = new File(this.dataDirectory);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }


    /**
     * save data to internal storage
     * @param action
     */
    private void saveData(String action){
//        Date clickTime = new Date();
//        long time = clickTime.getTime() - startDate.getTime();
        String speed = spinnerSpeed.getSelectedItem().toString();
        if(TextUtils.isEmpty(speed)){
            speed = Common.UNDEFINED;
        }
        String fileName = makeFileName(action, speed);
        Log.d(TAG, "Saving data to filename: " + fileName);
        FileOutputStream fos = null;

        try {
            if(TextUtils.isEmpty(dataDirectory)){
                makeDirectory();
            }
            File file = new File(dataDirectory, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(fos);
            LineData xData = mChartX.getData();
            LineData yData = mChartY.getData();
            LineData zData = mChartZ.getData();

            int minEntryCount = Math.min(xData.getEntryCount(), yData.getEntryCount());
            minEntryCount = Math.min(minEntryCount, zData.getEntryCount());
            int minCount = Math.min(minEntryCount, NUM_OF_ENTRY);
            DecimalFormat df = new DecimalFormat("0.0000");

            int dem = 1;
            dos.writeChars(String.valueOf(minCount));

            dos.writeChar(';');
            for (int i = xData.getEntryCount(); i > xData.getEntryCount() - minCount; i--) {
                float xValue = (float) (xData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                String xStrValue = df.format(xValue);
                dos.writeChars(xStrValue);
                dos.writeChar(';');
                dem++;
            }
            for (int i = yData.getEntryCount(); i > yData.getEntryCount() - minCount; i--) {
                float yValue = (float) (yData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                String yStrValue = df.format(yValue);
                dos.writeChars(yStrValue);
                dos.writeChar(';');
                dem++;
            }
            for (int i = zData.getEntryCount(); i > zData.getEntryCount() - minCount; i--) {
                float zValue = (float) (zData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                String zStrValue = df.format(zValue);
                dos.writeChars(zStrValue);
                dos.writeChar(';');
                dem++;
            }
            fos.close();
            Log.d(TAG, "Saved data to filename: " + fileName);
//            Toast.makeText(this, "Saved with " + minCount + " " + time + " //" + time/minCount, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Saved with resolution " + minCount, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, Common.ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intentData) {
        switch (requestCode) {
            case CHOOSE_FILE_MESS_CODE:
                if (resultCode == RESULT_OK) {
                    sensorManager.unregisterListener(MainActivity.this);
                    isRunning = false;
                    if (intentData != null) {
                        Uri uri = intentData.getData();
                        String fileName = uri.getLastPathSegment().toString();
                        String fileNameRes = "";
                        int i = 0;
                        while ((i < fileName.length()) && (fileName.charAt(i) != ':')) i++;
                        i++;
                        while (i < fileName.length()) {
                            fileNameRes += fileName.charAt(i);
                            i++;
                        }
                        Toast.makeText(this, "Uri: " + fileNameRes, Toast.LENGTH_LONG).show();
                        StringBuilder sb = new StringBuilder();
                        try {
                            File textFile = new File(Environment.getExternalStorageDirectory(), fileNameRes);
                            FileInputStream fis = new FileInputStream(textFile);
                            if (fis != null) {
                                InputStreamReader isr = new InputStreamReader(fis);
                                BufferedReader buff = new BufferedReader(isr);
                                String line = null;
                                while ((line = buff.readLine()) != null) {
                                    sb.append(line + "\n");
                                }
                                fis.close();
                            }
                            String sbLast = "";
                            for (i = 0; i < sb.length(); i++) {
                                if ((sb.charAt(i) >= '0' && sb.charAt(i) <= '9')
                                        || (sb.charAt(i) == '.') || (sb.charAt(i) == '-') || (sb.charAt(i) == ',') ||
                                        (sb.charAt(i) == ';')) {
                                    if (sb.charAt(i) == ',') sbLast += '.';
                                    else
                                        sbLast += sb.charAt(i);
                                }
                            }
                            Log.d(TAG, "onActivityResult: " + sbLast);

                            String strArray[] = sbLast.toString().split(";");
                            Log.d(TAG, "onActivityResult: strArray len:" + strArray.length);
                            float floatArr[] = new float[strArray.length];
                            for (i = 0; i < strArray.length; i++) {
                                floatArr[i] = Float.valueOf(strArray[i]);
                            }
                            int num = (int)floatArr[0];

                            mChartX.clearValues();
                            LineData dataX = mChartX.getData();
                            if (dataX != null) {
                                ILineDataSet set = dataX.getDataSetByIndex(0);
                                if (set == null) {
                                    set = createSet(Color.RED);
                                    dataX.addDataSet(set);
                                }
                                Log.d(TAG, "addEndtry: " + set.getEntryCount());

                                for (i = 1; i <= num; i++) {
                                    dataX.addEntry(new Entry(set.getEntryCount(), floatArr[i]+5), 0);
                                    dataX.notifyDataChanged();
                                    mChartX.notifyDataSetChanged();
                                    mChartX.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                                    mChartX.moveViewToX(dataX.getEntryCount());
                                }
                            }
                            mChartY.clearValues();
                            LineData dataY = mChartY.getData();
                            if (dataY != null) {
                                ILineDataSet set = dataY.getDataSetByIndex(0);
                                if (set == null) {
                                    set = createSet(Color.BLUE);
                                    dataY.addDataSet(set);
                                }
                                Log.d(TAG, "addEndtry: " + set.getEntryCount());

                                for (i = (num+1); i <= (num*2) ; i++) {
                                    dataY.addEntry(new Entry(set.getEntryCount(), floatArr[i]+5), 0);
                                    dataY.notifyDataChanged();
                                    mChartY.notifyDataSetChanged();
                                    mChartY.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                                    mChartY.moveViewToX(dataY.getEntryCount());
                                }
                            }
                            mChartZ.clearValues();
                            LineData dataZ = mChartZ.getData();
                            if (dataZ != null) {
                                ILineDataSet set = dataZ.getDataSetByIndex(0);
                                if (set == null) {
                                    set = createSet(Color.GREEN);
                                    dataZ.addDataSet(set);
                                }
                                Log.d(TAG, "addEndtry: " + set.getEntryCount());

                                for (i = (num*2+1); i <= num*3; i++) {
                                    dataZ.addEntry(new Entry(set.getEntryCount(), floatArr[i]+5), 0);
                                    dataZ.notifyDataChanged();
                                    mChartZ.notifyDataSetChanged();
                                    mChartZ.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                                    mChartZ.moveViewToX(dataZ.getEntryCount());
                                }
                            }

                            }catch(IOException e){
                                e.printStackTrace();
                            }


                        }
                    }
                    break;
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
            }
        }

        @Override
        protected void onPause () {
            super.onPause();
            sensorManager.unregisterListener(this);
        }


        @Override
        protected void onDestroy () {
            sensorManager.unregisterListener(MainActivity.this);
            super.onDestroy();
        }

        @Override
        protected void onResume () {
            super.onResume();
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults){
            switch (requestCode) {
                case 1000:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
            }
        }
    }
