package com.example.x.main;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Sockets {

    private static Socket socket;

    public static void setOfferOrdersListeners(Callback<JSONObject> callback){
        offerOrdersListeners.add(callback);
    }

    private static void callOfferOrder(final JSONObject object){
        Iterator<Callback<JSONObject>> iterator = offerOrdersListeners.iterator();
        while (iterator.hasNext()){
            Callback callback = iterator.next();
            callback.callingBack(object);
        }
    }

    private static List<Callback<JSONObject>> offerOrdersListeners = new ArrayList<>();

    public static void connect(){
        Log.d("VOl", "SOCKETS CONNECT");

        String url = "http://192.168.43.45:3000?token="+Token.getAccessToken();
        Log.d("VOl","soK: " +url);
        try {
            socket = IO.socket(url);

            socket.on("offer order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("VOl", "OFFER ORDER");
                    Log.d("VOl", args[0].toString());

                    try {
                        JSONObject req = new JSONObject(args[0].toString());
                        callOfferOrder(req);
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
                    Sockets.socket.close();
                    Sockets.socket = null;
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connect();
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

    public static void close(){
        if (socket != null)
            socket.close();
    }

}
