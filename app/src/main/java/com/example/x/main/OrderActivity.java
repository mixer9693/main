package com.example.x.main;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OrderActivity extends AppCompatActivity implements View.OnClickListener {

    private Order order;
    private TextView movId, status, from, to, when, description,
            phone, leaseTerm, value;
    private Button btnStatus;
    int movemId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        Log.d("VOl", "OrderActivity: onCreate");

        movemId = getIntent().getIntExtra("movId", 0);
        Log.d("VOl", "movId: " + movemId);

        movId = findViewById(R.id.mov_id);
        status = findViewById(R.id.status);
        from = findViewById(R.id.from);
        to = findViewById(R.id.to);
        when = findViewById(R.id.when);
        description = findViewById(R.id.description);
        phone = findViewById(R.id.phone);
        leaseTerm = findViewById(R.id.lease_term);
        value = findViewById(R.id.value);

        btnStatus = findViewById(R.id.btn_status);
        btnStatus.setOnClickListener(OrderActivity.this);

        Log.d("VOl", "before getOrder");
        getOrder(movemId, new Callback<JSONObject>() {
            @Override
            public void callingBack(JSONObject data) {
                Log.d("VOl", "call:");
                try {
                    order = Order.parse(data);
                    setOrderView();

                    order.setIsFinishListener(new Callback() {
                        @Override
                        public void callingBack(Object data) {
                            Log.d("VOl","OrderActivity IsFinishListener");
                            setOrderFinished();
                            finishActivity(200);
                            finish();
                        }
                    });
                } catch (JSONException e) {
                    Log.d("VOl", "!Order.parse(data):");
                }
            }
        });




    }

    protected void onResume(){
        super.onResume();
    }


    private void setOrderView(){

        movId.setText(String.valueOf(order.getMovId()));
        status.setText(String.valueOf(order.getStatus()));
        from.setText(String.valueOf(order.getFrom()));
        to.setText(String.valueOf(order.getTo()));
        when.setText(String.valueOf(order.getWhen()));
        phone.setText(String.valueOf(order.getPhone()));
        leaseTerm.setText(String.valueOf(order.getLeaseTerm()));
        value.setText(String.valueOf(order.getValue()));
        description.setText(String.valueOf(order.getDescription()));
        btnStatus.setText(String.valueOf(order.getNextStatus()));

        Log.d("VOl", "value: " + order.getValue());
    }


    @Override
    public void onClick(View v) {
        btnStatus.setEnabled(false);

        order.nextStatus(OrderActivity.this, new Callback<String>() {
            @Override
            public void callingBack(String data) {
               btnStatus.setEnabled(true);
               setOrderView();
            }
        });
    }

    private void getOrder(int movId, final Callback<JSONObject> callback){
        String url = "http://192.168.43.45:3000/api/performers/"
        + Token.getPerformerId()+"/movement-orders/"+movId+"?with=order";
        Log.d("VOl", url);

        JsonArrayRequest getOrderRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        Log.d("VOl", "getOrder OK");
                        Log.d("VOl", jsonArray.toString());
                        if (callback != null){
                            try {
                                JSONObject object = jsonArray.getJSONObject(0);
                                callback.callingBack(object);
                            } catch (JSONException e) {
                                Log.d("VOl", "Error JSON PARSE");
                                callback.callingBack(null);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "getOrder ERROR");
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
        queue.add(getOrderRequest);

    }

    private boolean setOrderFinished(){
        SharedPreferences sh = getSharedPreferences("APP", MODE_PRIVATE);
        return sh.edit().putBoolean("isOrderFinished", true).commit();
    }
}
