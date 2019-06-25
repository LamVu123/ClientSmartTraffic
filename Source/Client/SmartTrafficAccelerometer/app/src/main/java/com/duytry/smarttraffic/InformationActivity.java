package com.duytry.smarttraffic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.duytry.smarttraffic.common.Common;

public class InformationActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextRoad;
    private Button buttonStart;

    static final String MESSAGE_NAME_CAN_NOT_EMPTY = "Name can not empty";
    static final String MESSAGE_ROAD_CAN_NOT_EMPTY = "Road can not empty";
    private SharedPreferences userInformation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        //init
        this.editTextName = (EditText) findViewById(R.id.editText_name);
        this.editTextRoad = (EditText) findViewById(R.id.editText_road);
        this.buttonStart = (Button) findViewById(R.id.btn_start);

        userInformation = getSharedPreferences(Common.PREFERENCES,MODE_PRIVATE);
        String name = userInformation.getString(Common.NAME_PREFERENCES_KEY, "");
        String road = userInformation.getString(Common.ROAD_PREFERENCES_KEY, "");
        if(!TextUtils.isEmpty(name)){
            this.editTextName.setText(name);
        }
        if(!TextUtils.isEmpty(road)){
            this.editTextRoad.setText(road);
        }


        //set action onclick
        View.OnClickListener startListener = new View.OnClickListener(){
            public void  onClick  (View  v){
                //get value
                String name = editTextName.getText().toString();
                String road = editTextRoad.getText().toString();

                //validate
                if(TextUtils.isEmpty(name)){
                    Toast.makeText(InformationActivity.this, MESSAGE_NAME_CAN_NOT_EMPTY, Toast.LENGTH_LONG).show();
                    return;
                }
                if(TextUtils.isEmpty(road)){
                    Toast.makeText(InformationActivity.this, MESSAGE_ROAD_CAN_NOT_EMPTY, Toast.LENGTH_LONG).show();
                    return;
                }

                //save information
                SharedPreferences.Editor prefEditor = userInformation.edit();
                prefEditor.putString(Common.NAME_PREFERENCES_KEY, name);
                prefEditor.putString(Common.ROAD_PREFERENCES_KEY, road);
                prefEditor.commit();

                //direct to MainActivity
                Intent intent = new Intent(InformationActivity.this, MainActivity.class);
                startActivity(intent);
            }
        };
        this.buttonStart.setOnClickListener(startListener);
    }



}
