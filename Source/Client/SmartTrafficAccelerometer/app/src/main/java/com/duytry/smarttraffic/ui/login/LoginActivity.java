package com.duytry.smarttraffic.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.duytry.smarttraffic.BuildConfig;
import com.duytry.smarttraffic.InformationActivity;
import com.duytry.smarttraffic.R;
import com.duytry.smarttraffic.common.Common;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class LoginActivity extends AppCompatActivity {

    private static String USERNAME_CAN_NOT_EMPTY = "Username cannot empty";
    private static String PASSWORD_CAN_NOT_EMPTY = "Password cannot empty";
    private static String LOGIN_SUCCESS_MESSAGE = "You are logged in";
    private static String LOGIN_FAIL_MESSAGE = "Log in fail, please check your username and password";
    private static String USERNAME_EVENT_SOCKET = "username";
    private static String PASSWORD_EVENT_SOCKET = "password";
    private static String LOGIN_RESULT_EVENT_SOCKET = "loginResults";
    private static String RESULT_JSON = "result";
    private static String RESULT_JSON_STATUS_OK = "true";

    private SharedPreferences userInformation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        userInformation = getSharedPreferences(Common.PREFERENCES,MODE_PRIVATE);

        mSocket.connect();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);

                //get value
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                //validate
                if(TextUtils.isEmpty(username)){
                    Toast.makeText(LoginActivity.this, USERNAME_CAN_NOT_EMPTY, Toast.LENGTH_LONG).show();
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    Toast.makeText(LoginActivity.this, PASSWORD_CAN_NOT_EMPTY, Toast.LENGTH_LONG).show();
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                    return;
                }

                SharedPreferences.Editor prefEditor = userInformation.edit();
                prefEditor.putString(Common.NAME_PREFERENCES_KEY, username);
                prefEditor.commit();

                mSocket.emit(USERNAME_EVENT_SOCKET, username);
                mSocket.emit(PASSWORD_EVENT_SOCKET, password);

                mSocket.on(LOGIN_RESULT_EVENT_SOCKET, onMessage_Results);

            }
        });



    }


    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(BuildConfig.LoginIp);
        } catch (URISyntaxException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private Emitter.Listener onMessage_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    String result;
                    try{
                        result = data.getString(RESULT_JSON);
                        if(result != null && result.equals(RESULT_JSON_STATUS_OK)){
                            Toast.makeText(getApplicationContext(), LOGIN_SUCCESS_MESSAGE, Toast.LENGTH_SHORT).show();
                            mSocket.disconnect();
                            mSocket.off();

                            goToInformationActivity();
                        } else {
                            Toast.makeText(getApplicationContext(), LOGIN_FAIL_MESSAGE, Toast.LENGTH_SHORT).show();
                        }

                    }catch(JSONException e){
                        return;
                    }
                }
            });
        }
    };

    private void goToInformationActivity(){
        Intent intent = new Intent(getApplicationContext(), InformationActivity.class);
        startActivity(intent);
    }

}
