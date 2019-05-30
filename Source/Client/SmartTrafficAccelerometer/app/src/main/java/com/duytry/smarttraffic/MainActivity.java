package com.duytry.smarttraffic;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private Queue dataExample;
    private static final int NUM_OF_ENDTRY = 300;
    private static final String TAG = "MainActivity";
    private static final int message_request = 01;
    private EditText fileNameResult;
    private String fileNameToLoad;
    private String pathFileToLoad;
    private static final int choose_file_mess_code = 02;
    private int mVisibleXRangeMaximum = 200;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    //    TextView xValue, yValue, zValue;
    private LineChart mChartX, mChartY, mChartZ;
    private Thread threadX, threadY, threadZ;
    private boolean plotDataX, plotDataY, plotDataZ = true;
    private boolean onLoadFile = false;
    private int idLoadFile = 0;
    Intent myFileIntent;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        }
        //dataExample.add()
        Button btnStop = (Button) findViewById(R.id.stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG, "onClick: on click");
                if (threadX!= null){
                    threadX.interrupt();
                }
                if (threadY!= null){
                    threadY.interrupt();
                }
                if (threadZ!= null){
                    threadZ.interrupt();
                }
                sensorManager.unregisterListener(MainActivity.this);
            }
        });
        //
        Button btnResume = (Button) findViewById(R.id.resume);
        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
        });
        //
        Button btnSave = (Button) findViewById(R.id.save);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (threadX!= null){
                    threadX.interrupt();
                }
                if (threadY!= null){
                    threadY.interrupt();
                }
                if (threadZ!= null){
                    threadZ.interrupt();
                }
                sensorManager.unregisterListener(MainActivity.this);
                openSaveActivity();
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);


            }
        });
        //
        Button btnOpen = (Button) findViewById(R.id.open);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                plotDataX = false;
//                myFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                myFileIntent.setType("*/*");
//                startActivityForResult(myFileIntent, 2);

                if (threadX!= null){
                    threadX.interrupt();
                }
                if (threadY!= null){
                    threadY.interrupt();
                }
                if (threadZ!= null){
                    threadZ.interrupt();
                }
                sensorManager.unregisterListener(MainActivity.this);
//                plotDataX = false;
//                myFileIntent = new Intent(Intent.ACTION_GET_CONTENT);


                loadData("txt");


//                super.onPause();



            }
        });


        mChartX = (LineChart) findViewById(R.id.chartX);
        //  int i = (int) findViewById(R.id.chart1);
        mChartX.getDescription().setText("Real time accelerometer Data Plot X");
        mChartX.setTouchEnabled(true);
        mChartX.setDragEnabled(true);
        mChartX.setScaleEnabled(true);
        mChartX.setDrawGridBackground(true);
        mChartX.setPinchZoom(true);
        mChartX.setBackgroundColor(Color.WHITE);

        mChartY = (LineChart) findViewById(R.id.chartY);
        //  int i = (int) findViewById(R.id.chart1);
        mChartY.getDescription().setText("Real time accelerometer Data Plot Y");
        mChartY.setTouchEnabled(true);

        mChartY.setDragEnabled(true);
        mChartY.setScaleEnabled(true);
        mChartY.setDrawGridBackground(true);
        mChartY.setPinchZoom(true);
        mChartY.setBackgroundColor(Color.WHITE);

        mChartZ = (LineChart) findViewById(R.id.chartZ);
        //  int i = (int) findViewById(R.id.chart1);
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


        startPlotX();
        startPlotY();
        startPlotZ();

    }

    private void addEndtryX(SensorEvent event) {

        LineData data = mChartX.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSetX();
                data.addDataSet(set);
            }
           // Log.d(TAG, "addEndtry: " + set.getEntryCount());
//            data.addEntry(new Entry(set.getEntryCount()));

            Entry entry = new Entry(set.getEntryCount(), event.values[0] + 5);
            data.addEntry(entry, 0);
            LineData tmpData = mChartX.getData();
//            mChartX.getLineData().getDataSetByIndex(0).getEntryForIndex(0);

//            entry.getX()
//                    data.getDataSetByIndex(0).getEntryForIndex(0).getX();
                data.notifyDataChanged();
                mChartX.getDescription().setText("x: "+ event.values[0] + "=" + (tmpData.getDataSetByIndex(0).getEntryForIndex(tmpData.getEntryCount()-1).getY()-5) );
                mChartX.notifyDataSetChanged();
                mChartX.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
                mChartX.moveViewToX(data.getEntryCount());



//            mChart.setData(data);
//            mChart.invalidate();
        }
    }

    private void loadData(String fileName) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, choose_file_mess_code);
        
//        LineData data = mChartX.getData();
//        if (data != null) {
//            ILineDataSet set = data.getDataSetByIndex(0);
//            if (set == null) {
//                set = createSetX();
//                data.addDataSet(set);
//            }
            // Log.d(TAG, "addEndtry: " + set.getEntryCount());
//            data.addEntry(new Entry(set.getEntryCount()));

//            for (int i =0; i<1000; i++){
//                data.addEntry(new Entry(set.getEntryCount(), i%10), 0);
//                data.notifyDataChanged();
////                mChartX.getDescription().setText("x: "+ event.values[0]);
//                mChartX.notifyDataSetChanged();
//                mChartX.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
//                mChartX.moveViewToX(data.getEntryCount());
//            }




//            mChart.setData(data);
//            mChart.invalidate();
//        }
    }


    private void addEndtryY(SensorEvent event) {
        LineData data = mChartY.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSetY();
                data.addDataSet(set);
            }
//            Log.d(TAG, "addEndtry: " + event.values[0] + 5 +" " + event.values[1]+5);
//            data.addEntry(new Entry(set.getEntryCount()));
            data.addEntry(new Entry(set.getEntryCount(), event.values[1] + 5), 0);
            data.notifyDataChanged();
            mChartY.getDescription().setText("y: "+ event.values[1]);
            mChartY.notifyDataSetChanged();
            mChartY.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mChartY.moveViewToX(data.getEntryCount());
//            mChart.setData(data);
//            mChart.invalidate();
        }
    }
    private void addEndtryZ(SensorEvent event) {
        LineData data = mChartZ.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSetZ();
                data.addDataSet(set);
            }
           // Log.d(TAG, "addEndtry: " + set.getEntryCount());
//            data.addEntry(new Entry(set.getEntryCount()));
            data.addEntry(new Entry(set.getEntryCount(), event.values[2] + 5), 0);
            data.notifyDataChanged();
            mChartZ.getDescription().setText("z: "+ event.values[2]);
            mChartZ.notifyDataSetChanged();
            mChartZ.setVisibleXRangeMaximum(mVisibleXRangeMaximum);
            mChartZ.moveViewToX(data.getEntryCount());

//            mChart.setData(data);
//            mChart.invalidate();
        }
    }

    private LineDataSet createSetX() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.RED);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }
    private LineDataSet createSetY() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.BLUE);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }
    private LineDataSet createSetZ() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.GREEN);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }


    private void startPlotX() {
        if (threadX != null) {
            threadX.interrupt();
        }
        threadX = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    plotDataX = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadX.start();
    }
    private void startPlotY() {
        if (threadY != null) {
            threadY.interrupt();
        }
        threadY = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    plotDataY = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadY.start();
    }
    private void startPlotZ() {
        if (threadZ != null) {
            threadZ.interrupt();
        }
        threadZ = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    plotDataZ = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadZ.start();
    }
    private void openSaveActivity(){
        Intent intent = new Intent(this, SaveActivity.class);
        startActivityForResult(intent, message_request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case 01:
                if (resultCode == RESULT_OK) {

                    if (threadX != null) {
                        threadX.interrupt();
                    }
                    if (threadY != null) {
                        threadY.interrupt();
                    }
                    if (threadZ != null) {
                        threadZ.interrupt();
                    }
                    sensorManager.unregisterListener(MainActivity.this);


                    String fileNameFromMessage = data.getStringExtra("name");
                    Log.d(TAG, "onActivityResult: file name: " + fileNameFromMessage);
                    fileNameFromMessage += ".txt";
                    FileOutputStream fos = null;

                    try {
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileNameFromMessage);
                        fos = new FileOutputStream(file);
                        DataOutputStream dos = new DataOutputStream(fos);
                        LineData xData = mChartX.getData();
                        LineData yData = mChartY.getData();
                        LineData zData = mChartZ.getData();


                        int minEntryCount = Math.min(xData.getEntryCount(), yData.getEntryCount());
                        minEntryCount = Math.min(minEntryCount, zData.getEntryCount());
                        int minCount = Math.min(minEntryCount, NUM_OF_ENDTRY);


                        dos.writeChars(String.valueOf(minCount));
                        dos.writeChar(',');
                        Log.d(TAG, "onActivityResult: " + xData.getEntryCount() + " " + yData.getEntryCount() + " " + zData.getEntryCount());
                        for (int i = xData.getEntryCount(); i > xData.getEntryCount() - minCount; i--) {
                            float xValue = (float) (xData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                            Log.d(TAG, "onActivityResult: " + xValue);
                            dos.writeChars(String.valueOf(xValue));
                            dos.writeChar(',');
                        }
                        for (int i = yData.getEntryCount(); i > yData.getEntryCount() - minCount; i--) {
                            float yValue = (float) (yData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                            dos.writeChars(String.valueOf(yValue));
                            dos.writeChar(',');
                        }
                        for (int i = zData.getEntryCount(); i > zData.getEntryCount() - minCount; i--) {
                            float zValue = (float) (zData.getDataSetByIndex(0).getEntryForIndex(i - 1).getY() - 5);
                            dos.writeChars(String.valueOf(zValue));
                            dos.writeChar(',');
                        }
                        fos.close();
                        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, "Found", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    } catch (IOException e) {
                        Toast.makeText(this, "Found2", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
                break;
            case 2:
                if (data != null){
                    Uri uri = data.getData();
                    Toast.makeText(this, "Uri: "+ uri, Toast.LENGTH_LONG).show();
                    fileNameToLoad = uri.toString();
                    Toast.makeText(this, "Path: "+ uri.getPath(), Toast.LENGTH_LONG).show();
                    pathFileToLoad = uri.getPath().toString();
                }
                break;
            case 10:


                if (requestCode==RESULT_OK){
                    String path = data.getData().getPath();
                    loadData(path);
                }
                break;



            }
        }




//                LineData tmpData = mChartX.getData();
//                int tmpCount = tmpData.getEntryCount();
//                int min = Math.min(tmpCount, 300);
//                for (int i=tmpCount; i>tmpCount-min; i--){
//                    float tmpYValue = tmpData.getDataSetByIndex(0).getEntryForIndex(i-1).getY()-5;
//                    Log.d(TAG, "onActivityResult: "  + i + " " + tmpYValue);
//                }

//                mChartX.getLineData().getDataSetByIndex(0).getEntryForIndex(0).getX();
                //save file here




    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {

        if (plotDataX){

//            Log.d(TAG, "onSensorChanged: "+ sensorEvent.values[0] + " " + sensorEvent.values[1] +" " + sensorEvent.values[2]);
            addEndtryX(sensorEvent);
            plotDataX = false;
        }
        if (plotDataY){
//            Log.d(TAG, "onSensorChanged: "+ sensorEvent.values[0] + " " + sensorEvent.values[1] +" " + sensorEvent.values[2]);
            addEndtryY(sensorEvent);
            plotDataY = false;
        }
        if (plotDataZ){
//            Log.d(TAG, "onSensorChanged: "+ sensorEvent.values[0] + " " + sensorEvent.values[1] +" " + sensorEvent.values[2]);
            addEndtryZ(sensorEvent);
            plotDataZ = false;
        }




    }

    @Override
    protected void onPause() {
        super.onPause();
        if (threadX!= null){
            threadX.interrupt();
        }
        if (threadY!= null){
            threadY.interrupt();
        }
        if (threadZ!= null){
            threadZ.interrupt();
        }
        sensorManager.unregisterListener(this);
    }



    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(MainActivity.this);
        threadX.interrupt();
        threadY.interrupt();
        threadZ.interrupt();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1000:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}
