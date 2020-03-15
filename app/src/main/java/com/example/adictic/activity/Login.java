package com.example.adictic.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.adictic.R;
import com.example.adictic.TodoApp;
import com.example.adictic.entity.User;
import com.example.adictic.entity.UserLogin;
import com.example.adictic.rest.TodoApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// This is the Login fragment where the user enters the username and password and
// then a RESTResponder_RF is called to check the authentication
public class Login extends AppCompatActivity {

    TodoApi mTodoService;
    static Login login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        login = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mTodoService = ((TodoApp)this.getApplication()).getAPI();

        Button b_log = (Button)findViewById(R.id.login_button);
        TextView b_reg = (TextView)findViewById(R.id.TV_register);
        // This is the listener that will be used when the user presses the "Login" button
        b_log.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText u = (EditText) Login.this.findViewById(R.id.login_username);
                EditText p = (EditText) Login.this.findViewById(R.id.login_password);
                RadioButton tutor = (RadioButton)findViewById(R.id.RB_tutor);
                RadioButton tutelat = (RadioButton)findViewById(R.id.RB_tutelat);

                TextView noTypeDevice = (TextView)findViewById(R.id.TV_noTypeDevice);

                final String[] token = new String[1];

                noTypeDevice.setVisibility(View.GONE);

                // Firebase token
                final String TAG = "Firebase Token: ";
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "getInstanceId failed", task.getException());
                                    return;
                                }

                                // Get new Instance ID token
                                token[0] = task.getResult().getToken();

                                // Log and toast
                                Log.d(TAG, token[0]);
                            }
                        });

                if(tutor.isChecked()) Login.this.checkCredentials(u.getText().toString(), p.getText().toString(),1, token[0]);
                else if(tutelat.isChecked()){
                    Login.this.checkCredentials(u.getText().toString(), p.getText().toString(),0, token[0]);
                }
                else{
                    noTypeDevice.setVisibility(View.VISIBLE);
                }
            }
        });

        // This is the listener that will be used when the user presses the "Register" button
        b_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login.this.startActivity(new Intent(Login.this, Register.class));
            }
        });
    }

    // This method is called when the "Login" button is pressed in the Login fragment
    public void checkCredentials(String username, String password, Integer tutor, String token) {
        UserLogin ul = new UserLogin();
        ul.username = username;
        ul.password = password;
        ul.tutor = tutor;
        ul.token = token;

        Call<User> call = mTodoService.login(ul);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Login.this.startActivity(new Intent(Login.this, MainActivity.class));
                    Login.this.finish();
                } else {
                    Toast toast = Toast.makeText(Login.this, getString(R.string.error_noLogin), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast toast = Toast.makeText(Login.this, getString(R.string.error_noLogin), Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public static Login getInstance(){
        return login;
    }

}