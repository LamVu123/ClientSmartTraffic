package com.duytry.smarttraffic.ui.login;

import android.app.AlertDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.duytry.smarttraffic.MainActivity;
import com.duytry.smarttraffic.common.MySocketFactory;
import com.duytry.smarttraffic.R;

import com.duytry.smarttraffic.common.Common;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;



public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    private static String USERNAME_EVENT_SOCKET = "username";
    private static String PASSWORD_EVENT_SOCKET = "password";
    private static String LOGIN_RESULT_EVENT_SOCKET = "loginResults";
    private static String RESULT_JSON = "result";
    private static String RESULT_JSON_STATUS_OK = "true";
    private static boolean isSigningIn = false;

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loadingProgressBar;
    private SharedPreferences userInformation;

    private Socket mSocket = MySocketFactory.getInstance().getMySocket();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = ViewModelProviders.of(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);

        userInformation = getSharedPreferences(Common.PREFERENCES,MODE_PRIVATE);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginResult.observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                isSigningIn = false;
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    //go to information
                    goToInformationActivity();
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                doLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });
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
                            loginResult.postValue(new LoginResult(new LoggedInUserView(usernameEditText.getText().toString())));
                        } else {
                            loginResult.postValue(new LoginResult(R.string.login_failed));
                        }

                    }catch(JSONException e){
                        return;
                    }
                }
            });
        }
    };

    private void doLogin(String username, String password){
        isSigningIn = true;
        if(!mSocket.connected()){
            mSocket.connect();
        }
        mSocket.emit(USERNAME_EVENT_SOCKET, username);
        mSocket.emit(PASSWORD_EVENT_SOCKET, Common.md5(password));
        mSocket.on(LOGIN_RESULT_EVENT_SOCKET, onMessage_Results);
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if (isSigningIn) {
                    loginResult.postValue(new LoginResult(R.string.login_error));
                }
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, Common.CONNECTION_TIME_OUT);

    }

    private void showLoginFailed(@StringRes Integer errorString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle(R.string.login_failed_tittle_dialog);
        builder.setMessage(errorString);
        builder.setCancelable(true);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void goToInformationActivity(){
        SharedPreferences.Editor prefEditor = userInformation.edit();
        prefEditor.putString(Common.NAME_PREFERENCES_KEY, usernameEditText.getText().toString());
        prefEditor.commit();
        if(mSocket.hasListeners(LOGIN_RESULT_EVENT_SOCKET)){
            mSocket.off(LOGIN_RESULT_EVENT_SOCKET);
        }
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onDestroy() {
        if(mSocket.hasListeners(LOGIN_RESULT_EVENT_SOCKET)){
            mSocket.off(LOGIN_RESULT_EVENT_SOCKET);
        }
        if(mSocket.connected()){
            mSocket.disconnect();
        }
        super.onDestroy();
    }
}
