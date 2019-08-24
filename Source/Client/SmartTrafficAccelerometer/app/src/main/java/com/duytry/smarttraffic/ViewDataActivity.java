package com.duytry.smarttraffic;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.duytry.smarttraffic.fragment.ViewDataFragment;

public class ViewDataActivity extends AppCompatActivity {

    ViewDataFragment viewDataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        viewDataFragment = (ViewDataFragment) getSupportFragmentManager().findFragmentById(R.id.view_data_fragment_2);
        viewDataFragment.initChart();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
        String fileName = intent.getStringExtra("fileName");
        fileName = fileName == null? "View Data" : fileName;
        actionBar.setTitle(fileName);

        viewDataFragment.viewDataFromString(data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
