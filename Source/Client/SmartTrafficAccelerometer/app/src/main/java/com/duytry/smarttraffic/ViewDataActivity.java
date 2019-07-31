package com.duytry.smarttraffic;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.duytry.smarttraffic.fragment.ViewDataFragment;

public class ViewDataActivity extends AppCompatActivity {

    ViewDataFragment viewDataFragment;
    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        backButton = findViewById(R.id.btn_back_from_view_data);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        viewDataFragment = (ViewDataFragment) getSupportFragmentManager().findFragmentById(R.id.view_data_fragment_2);
        viewDataFragment.initChart();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        viewDataFragment.viewDataFromString(data);
    }
}
