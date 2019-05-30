package com.duytry.smarttraffic;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SaveActivity extends AppCompatActivity {
    EditText fileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save);
        fileName = findViewById(R.id.fileName);
        Button btnSave = (Button) findViewById(R.id.btnSaveFile);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileName.getText().toString().equals("")){
                    Toast.makeText(SaveActivity.this, "Pls enter file name", Toast.LENGTH_SHORT).show();
                } else {
                    String txtFileName = fileName.getText().toString();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("name", txtFileName);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

            }
        });
    }



}
