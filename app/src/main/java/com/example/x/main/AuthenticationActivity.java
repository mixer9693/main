package com.example.x.main;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class AuthenticationActivity extends AppCompatActivity implements View.OnClickListener {

    EditText login, password;
    Button button;
    TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        Log.d("VOl", "AuthenticationActivity onCreate: "+ Thread.currentThread().getId());
        login = (EditText) findViewById(R.id.login);
        password = (EditText) findViewById(R.id.password);
        button = (Button) findViewById(R.id.button);
        error = (TextView) findViewById(R.id.error);

        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        String loginVal = login.getText().toString();
        String passwordVal = password.getText().toString();

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://192.168.43.45:3000/api/auth";

        JSONObject body = new JSONObject();
        try {
            body.put("login", loginVal);
            body.put("password", passwordVal);
        } catch (JSONException e) {
            error.setText("Введены некорректный данные");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {

                        String role = null;
                        try {
                            role = (String) jsonObject.get("role");
                        } catch (JSONException e) {
                            error.setText("Запрос не выполнен");
                            return;
                        }
                        if (role.equals("driver")) {
                            Log.d("VOl", "AUTH OK: " + jsonObject.toString());
                            try {
                                Token.parse(jsonObject);
                                Token.work();

                                Intent intent = new Intent(AuthenticationActivity.this,
                                        SetPerformerActivity.class);
                                startActivityForResult(intent, 888);

                            } catch (JSONException e) {
                                Log.d("VOl", "JSONException: " + e.getMessage());
                            }
                        } else
                            error.setText("Неверный логин или пароль");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (volleyError != null && volleyError.networkResponse != null) {
                            switch (volleyError.networkResponse.statusCode) {
                                case 401:
                                    error.setText("Неверный логин или пароль");
                                    break;
                                default:
                                    error.setText("Запрос не выполнен");
                            }
                        } else
                            error.setText("Сервер не отвечает");
                    }
                });

        queue.add(jsonObjectRequest);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("VOl", "auth onActivityResult " +
                "reqCode:"+ requestCode +" resCode:" +resultCode);

        finishActivity(100);
        finish();
    }




}

