package com.example.x.main;

;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.example.x.main.R.layout.item_performers_list;

public class SetPerformerActivity extends AppCompatActivity {

    private TextView errorTextView = null;
    private ListView listView = null;

    private String listStringURL = "http://192.168.43.45:3000/api/performers?with=car";
    private String setStringURL = "http://192.168.43.45:3000/api/performers/:id/set";

    private Item[] items;
    private JsonArrayRequest performerListRequest;
    private AdapterView.OnItemClickListener onItemClickListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performers_list);

        Log.d("VOl", "SetPerformerActivity onCreate");

        listView = findViewById(R.id.list);
        errorTextView = findViewById(R.id.errorTextView);

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(performerListRequest);

    }

    {
        //запрос списка машин
        performerListRequest = new JsonArrayRequest
                (Request.Method.GET, listStringURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        Log.d("VOl", jsonArray.toString());
                        createViewList(jsonArray);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        errorTextView.setText("Запрос к серверу завершился с ошибкой");

                        if (volleyError != null)
                            Log.d("VOl", "err: " + volleyError.toString());
                        if (volleyError.networkResponse != null)
                            Log.d("VOl", "err: " + volleyError.networkResponse.statusCode);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        //обработчик клика по списку машин
        onItemClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d("VOl", "\nonItemClick");
//                Log.d("VOl", "perId: " + items[position].getId());

//                listView.setVisibility(View.INVISIBLE);
//                errorTextView.setText("");
                setPerformer(items[position].getId());

            }
        };

    }

    private Item[] JSONArrayToItemArray(JSONArray jsonArray){
        Item[] items = new Item[jsonArray.length()];

        int id, carrying_capacity;
        String car_brand, type_of_car, state_number;

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject car = null;
            try {
                JSONObject object = jsonArray.getJSONObject(i);
                id = object.getInt("id");

                car = object.getJSONObject("car");
                car_brand = car.getString("car_brand");
                type_of_car = car.getString("type_of_car");
                carrying_capacity = car.getInt("carrying_capacity");
                state_number = car.getString("state_number");

                items[i] = new Item(id, car_brand, type_of_car, carrying_capacity,
                        state_number);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return items;
    }


    private void createViewList(JSONArray jsonArray){
        items = JSONArrayToItemArray(jsonArray);

        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(this,
                item_performers_list, items);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onItemClickListener);
    }


    private void setPerformer(int id){
        Log.d("VOl", "setPerformer");
//        listView.setVisibility(View.INVISIBLE);


        final String url = setStringURL.replace(":id", String.valueOf(id));
        RequestQueue queue = Volley.newRequestQueue(this);

        //запрос установки исполнителя
        JsonObjectRequest setPerformerRequest = new JsonObjectRequest
                (Request.Method.POST, url, null,
        new  Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonArray) {
                errorTextView.setText("");
//                Log.d("VOl", "setRes");
//                Log.d("VOl", jsonArray.toString());
                try {
                    Token.parse(jsonArray);
                    finishActivity(888);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
//                listView.setVisibility(View.VISIBLE);
                errorTextView.setText("Не удалось выбрать автомобиль");

                Log.d("VOl", "onErrorResponse: " + url);
                if (volleyError != null)
                    Log.d("VOl", "err: " + volleyError.getMessage());
                if (volleyError.networkResponse != null)
                    Log.d("VOl", "err: " + volleyError.networkResponse.statusCode);
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + Token.getAccessToken());
                return headers;
            }
        };

        queue.add(setPerformerRequest);
    }



}
