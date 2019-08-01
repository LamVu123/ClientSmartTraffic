package com.duytry.smarttraffic.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.duytry.smarttraffic.R;
import com.duytry.smarttraffic.common.Common;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class ViewDataFragment extends Fragment {

    private LineChart mChartX;
    private LineChart mChartY;
    private LineChart mChartZ;

    private int mVisibleXRangeMaximum = 600;
    FragmentActivity listener;

    // This event fires 1st, before creation of fragment or any views
    // The onAttach method is called when the Fragment instance is associated with an Activity.
    // This does not mean the Activity is fully initialized.
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            this.listener = (FragmentActivity) context;
        }
    }

    // This method is called when the fragment is no longer connected to the Activity
    // Any references saved in onAttach should be nulled out here to prevent memory leaks.
    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_data, container, false);
    }

    /*
    init chart graph for accelerometer status view
     */
    public void initChart(){
        mChartX = (LineChart) listener.findViewById(R.id.view_chart_x);
        mChartX.getDescription().setText("Real time accelerometer Data Plot X");
        mChartX.setTouchEnabled(true);
        mChartX.setDragEnabled(true);
        mChartX.setScaleEnabled(true);
        mChartX.setDrawGridBackground(true);
        mChartX.setPinchZoom(true);
        mChartX.setBackgroundColor(Color.WHITE);

        mChartY = (LineChart) listener.findViewById(R.id.view_chart_y);
        mChartY.getDescription().setText("Real time accelerometer Data Plot Y");
        mChartY.setTouchEnabled(true);

        mChartY.setDragEnabled(true);
        mChartY.setScaleEnabled(true);
        mChartY.setDrawGridBackground(true);
        mChartY.setPinchZoom(true);
        mChartY.setBackgroundColor(Color.WHITE);

        mChartZ = (LineChart) listener.findViewById(R.id.view_chart_z);
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

    /**
     * add data to graph viewer
     * @param event
     * @param axe
     */
    public void addEndtry(SensorEvent event, int axe) {
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

    public void viewDataFromString(String data){
        String[] lines = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            lines = data.split(System.lineSeparator());
        } else {
            lines = data.split(System.getProperty("line.separator"));
        }
        try {
            int pointNumber = Integer.parseInt(lines[0]);
        } catch (NumberFormatException e){
            showDialogFileError();
        }

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

        for ( int i = 1; i < lines.length; i++) {
            String[] arrValues = lines[i].split(Common.SPACE_CHARACTER);
            if(arrValues.length != 8){
                showDialogFileError();
            }
            dataX.addEntry(new Entry(setX.getEntryCount(), Float.parseFloat(arrValues[1])+5), 0);
            dataY.addEntry(new Entry(setY.getEntryCount(), Float.parseFloat(arrValues[2])+5), 0);
            dataZ.addEntry(new Entry(setZ.getEntryCount(), Float.parseFloat(arrValues[3])+5), 0);
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
    }

    private void showDialogFileError(){
        AlertDialog.Builder builder = new AlertDialog.Builder(listener);
        builder.setMessage(R.string.wrong_file_dialog_message)
                .setTitle(R.string.wrong_file_dialog_tittle)
                .setNeutralButton(R.string.ok_button, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public LineChart getmChartX() {
        return mChartX;
    }

    public void setmChartX(LineChart mChartX) {
        this.mChartX = mChartX;
    }

    public LineChart getmChartY() {
        return mChartY;
    }

    public void setmChartY(LineChart mChartY) {
        this.mChartY = mChartY;
    }

    public LineChart getmChartZ() {
        return mChartZ;
    }

    public void setmChartZ(LineChart mChartZ) {
        this.mChartZ = mChartZ;
    }

    public FragmentActivity getListener() {
        return listener;
    }

    public void setListener(FragmentActivity listener) {
        this.listener = listener;
    }
}
