package com.example.smartTraffic;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.support.v7.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private Intent intent;
    private Spinner spMapType;
    private Toolbar toolbar;
    private int mapType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //show the activity in full screen

        spMapType = (Spinner) findViewById(R.id.spMapType);
        try {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
        } catch (Exception e) {
            e.printStackTrace();
        }
        intent = getIntent();
        mapType = intent.getIntExtra("maptype", 1);
        String[] typesMap = {"None","Normal","Satellite", "Terrain", "Hybrid" };
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, typesMap);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spMapType.setAdapter(adapter);
        spMapType.setSelection(mapType);

        spMapType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mapType = position;

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent = new Intent(getApplicationContext(), MapsActivity.class);
                    intent.putExtra("typeResult", mapType);
                    setResult(MapsActivity.RESULT_OK,intent);
                    finish();
                }
            });
        }
    }

}

