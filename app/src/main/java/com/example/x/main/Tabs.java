package com.example.x.main;

import android.content.Intent;
import android.content.SharedPreferences;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;


import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;

import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tabs extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private CheckBox checkBox;
    private Button button;
    boolean checkError;

    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("VOl","Tabs onCreate");

        checkBox = findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(this);
//        button = findViewById(R.id.button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Token.clear();
//            }
//        });


        Log.d("VOl","Tabs before setContext");
        Token.setContext(Tabs.this);
        Token.addReadyListener(ready);
        Token.addAuthImpossibleListener(impossible);
        Token.work();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("VOl", "tabs onActivityResult: res:" + resultCode+ " req:" + requestCode);

        if (!Token.isGood() || !isOrderFinished()){
            Log.d("VOl", "Tabs onActivityResult finish ");
            finish();
        }else {
            Token.addReadyListener(ready);
        }
    }

    private boolean isOrderFinished(){
        SharedPreferences sp = getSharedPreferences("APP", MODE_PRIVATE);
        boolean result = sp.getBoolean("isOrderFinished", false);
        sp.edit().putBoolean("isOrderFinished", false).commit();
        return result;
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d("VOl", "Tabs onDestroy::");
        if (socket != null) socket.close();
        setIsFree(false, null);
    }


    private void getMyOrders(final Callback<JSONArray> callback){

        String listStringURL = "http://192.168.43.45:3000/api/performers/:per_id/movement-orders?with=order&completed=0";
        listStringURL = listStringURL.replace(":per_id", String.valueOf(Token.getPerformerId()));
//        Log.d("VOl", "listStringURL: "+ listStringURL);

        JsonArrayRequest performerListRequest = new JsonArrayRequest
                (Request.Method.GET, listStringURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        Log.d("VOl", "OK: onResponse: " + jsonArray.length());
                        if (callback != null) callback.callingBack(jsonArray);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "onErrorResponse: ");
                        if (callback != null) callback.callingBack(null);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(performerListRequest);
    }

    @Deprecated
    private void setSocketListener(){
        Log.d("VOl","setSocketListener");

        String url = "http://192.168.43.45:3000?token="+Token.getAccessToken();
        try {
            socket = IO.socket(url);

            socket.on("offer order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("VOl", "OFFER ORDER");
                    Log.d("VOl", args[0].toString());

                    try {
                        JSONObject req = new JSONObject(args[0].toString());
                        final int moveId = req.getInt("movId");

                        takeOrder(moveId, new Callback<Boolean>() {
                            @Override
                            public void callingBack(Boolean data) {
                                if (!data){
                                    Log.d("VOl", "!takeOrder");
                                    return;
                                }
                                openOrderActivity(moveId);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });

            socket.on("news b", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
//                    Log.d("VOl", "news: " + args[0].toString());
                }
            });

            socket.on("disconnect", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("VOl", "DISCONNECT");
                    socket.connect();
                }
            });

            socket.on("error", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("VOl", "SOCKET ERROR");
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            Log.d("VOl", "URISyntaxException");
            e.printStackTrace();
        }
    }

    private void openOrderActivity(int movId){
        setIsFree(false, null);
        Intent intent = new Intent(Tabs.this, OrderActivity.class);
        intent.putExtra("movId", movId);
        startActivityForResult(intent, 200);
    }

    private void setIsFree(boolean value, final Callback<Boolean> callback){
//        Log.d("VOl", "setIsFree");
        char free = !value? '0' : '1';

        String url = "http://192.168.43.45:3000/api/performers/" + Token.getPerformerId();

        JSONObject body = new JSONObject();
        try {
            body.put("free", ""+free);
        } catch (JSONException e) {
            Log.d("VOl", "setBODY: ERR" + e.getMessage());
            if (callback != null) callback.callingBack(false);
        }

        Log.d("VOl", "setBODY: " + body);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.d("VOl", "setIsFree Response");
                        if (callback != null) callback.callingBack(true);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "setIsFree onErrorResponse");
                        Log.d("VOl", volleyError.toString());
                        Log.d("VOl", volleyError.getMessage());
                        if (callback != null) callback.callingBack(false);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }


    Callback ready = new Callback() {
        @Override
        public void callingBack(Object data) {
            Log.d("VOl","ready");

            setMe();

            getMyOrders(new Callback<JSONArray>() {
                @Override
                public void callingBack(JSONArray data) {
                    if (data == null ) {
                        Log.d("VOl", "ERROR");
                        return;
                    }

                    for(int i = 0; i < data.length(); i++){
                        try {
                            JSONObject object = data.getJSONObject(i);
                            String status = object.getString("status");

                            if (!status.equals("выполнен")) {
                                Log.d("VOl", "HTTP ORDER:");
                                int movId = object.getInt("id");
                                openOrderActivity(movId);
                                return;
                            }
                        }catch (JSONException exc){return;}
                    }
                    Log.d("VOl", "no order");

                    Sockets.connect();
                    Sockets.setOfferOrdersListeners(new Callback<JSONObject>() {
                        @Override
                        public void callingBack(JSONObject data) {
                            Log.d("VOl", "sockets callback: " + data);
                            final int moveId;
                            try {
                                moveId = data.getInt("movId");
                                takeOrder(moveId, new Callback<Boolean>() {
                                    @Override
                                    public void callingBack(Boolean data) {
                                        if (!data){
                                            Log.d("VOl", "!takeOrder");
                                            return;
                                        }
                                        openOrderActivity(moveId);
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }
            });
        }
    };

    Callback impossible = new Callback() {
        @Override
        public void callingBack(Object data) {
            Log.d("VOl","impossible");
            Intent intent = new Intent(Tabs.this, AuthenticationActivity.class);
            startActivityForResult(intent, 100);
        }
    };



    private void takeOrder(int movId, final Callback<Boolean> callback){

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://192.168.43.45:3000/api/movement-orders/"+ movId;

        JSONObject body = new JSONObject();
        try {
            body.put("status", "назначен");
        } catch (JSONException e) {
            Log.e("VOl", "takeOrder: ERROR parse");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.d("VOl", "RES OK");
                        Log.d("VOl", jsonObject.toString());
                        if (callback != null) callback.callingBack(true);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "RES ERROR");
                        if (callback != null) callback.callingBack(false);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
    }

    private boolean setMovId(int movId){
        SharedPreferences sh = getSharedPreferences("APP", MODE_PRIVATE);
        return sh.edit().putInt("movId", movId).commit();
    }

    private int getMovId(){
        SharedPreferences sh = getSharedPreferences("APP", MODE_PRIVATE);
        return sh.getInt("movId", 0);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        Log.d("VOl", "onCheckedChanged");
        if (checkError){
            checkError = false;
            return;
        }
        setIsFree(isChecked, new Callback<Boolean>() {
            @Override
            public void callingBack(Boolean data) {
                if (!data) {
                    checkError = true;
                    checkBox.setChecked(!isChecked);
                }
            }
        });
    }

    private void setMe(){
        String listStringURL = "http://192.168.43.45:3000/api/performers/"+Token.getPerformerId();

        JsonArrayRequest performerRequest = new JsonArrayRequest
                (Request.Method.GET, listStringURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        Log.d("VOl", "OK: onResponse: " + jsonArray.length());
                        if (jsonArray.length() > 0){
                            try {
                                JSONObject performer = jsonArray.getJSONObject(0)
                                        .getJSONObject("performer");
                                int free = performer.getInt("free");

                                boolean isFree = free == 1 ? true: false;
                                checkError = true;
                                checkBox.setChecked(isFree);
                            }catch (JSONException exc){}
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "setMe onErrorResponse: ");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(performerRequest);
    }
}
