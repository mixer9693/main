package com.example.x.main;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Order{

    private int movId, orderId, phone, leaseTerm, value;
    private String from, to, when, description;
    private byte indexStatus;
    private boolean isFinish;
    private List<Callback> finishListeners = new ArrayList<>();

    private String[] statusArray = {"назначен", "прибыл", "в пути", "разгрузка",
            "оплата", "выполнен", ""};

    public Order(int movId, int orderId, String status, String from, String to,
                 String when, String description, int phone, int leaseTerm, int value){
        this.movId = movId;
        this.orderId = orderId;
        this.from = from;
        this.to = to;
        this.when = when;
        this.description = description;
        this.phone = phone;
        this.leaseTerm = leaseTerm;
        this.value = value;
        setStatus(status);
    }

    private void setStatus(String status){
        for (int i = 0; i < statusArray.length; i++){
            if (status.equals(statusArray[i])){
                indexStatus = (byte)i;
                return;
            }
        }
    }

    public static Order parse(JSONObject object) throws JSONException {

        int movId = object.getInt("id");
        int orderId = object.getInt("order_id");
        String status = object.getString("status");

        JSONObject order = object.getJSONObject("order");
        String from = order.getString("from");
        String to = order.getString("to");
        String when = order.get("when").toString();
        int phone = order.getInt("phone");
        String description = order.getString("description");
        int leaseTerm = order.getInt("lease_term");
        int value = order.getInt("value");

        return new Order(movId, orderId, status, from, to, when, description, phone,
                leaseTerm, value);
    }

    public void setIsFinishListener(Callback callback){
        if (isFinish)
            callback.callingBack(null);
        else if (callback != null)
            finishListeners.add(callback);
    }

    private void callFinish(){
        Log.d("VOl", "callFinish");
        for (Callback c: finishListeners)
            c.callingBack(null);
    }

    public int getMovId(){
        return movId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getLeaseTerm() {
        return leaseTerm;
    }

    public int getPhone() {
        return phone;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getWhen() {
        return when;
    }

    public String getStatus() {
        return statusArray[indexStatus];
    }

    public String getNextStatus(){
        if (!isFinish)
            return statusArray[indexStatus + 1];
        return "";
    }

    public void nextStatus(Context context, final Callback<String> callback){
        if (isFinish) return;

        String url = "http://192.168.43.45:3000/api/movement-orders/" + movId;

        JSONObject body = new JSONObject();
        try {
            body.put("status", statusArray[indexStatus+1]);
        } catch (JSONException e) {}

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.d("VOl", "setOrderStatus Response");
                        indexStatus++;
                        if (indexStatus >= statusArray.length - 2){
                            isFinish = true;
                            callFinish();
                        }
                        if (callback != null) callback.callingBack(statusArray[indexStatus]);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d("VOl", "setOrderStatus onErrorResponse");
                        if (callback != null) callback.callingBack(statusArray[indexStatus]);
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(jsonObjectRequest);
    }
}
