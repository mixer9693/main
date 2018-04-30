package com.example.x.main;

import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class Token {

    private static String accessToken;
    private static String refreshToken;
    private static long expiresIn;
    private static String role;
    private static int employeeId;
    private static int performerId;

    private static String urlRefresh;
    private static Timer timer;
    private static TimerTask timerTask;

    private static Context context;
    private static Set<Callback> readyListeners;
    private static List<Callback> authImpossibleListeners;

    private static Token token;

    static {
        token = new Token();

        urlRefresh = "http://192.168.43.45:3000/api/auth/refresh";
        readyListeners = new HashSet<>();
        authImpossibleListeners = new ArrayList<>();
    }

    private Token(){}

    public static Token getInstance(){return token;}


    public static String getAccessToken() {
        return accessToken;
    }

    public static int getEmployeeId(){return employeeId;}

    public static int getPerformerId(){
        return performerId;
    }

    public static void write(){
//        Log.d("VOl", "write");
        SharedPreferences sh = context.getSharedPreferences
                ("APP", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sh.edit();
        editor.putString("refreshToken", refreshToken);
        boolean com = editor.commit();
    }

    public static void read(){
//        Log.d("VOl", "read");
        SharedPreferences sh = context.getSharedPreferences
                ("APP", Context.MODE_PRIVATE);
        refreshToken = sh.getString("refreshToken", null);
        Log.d("VOl", "read refT: " + refreshToken);
    }

    public static void setContext(Context context){
        Token.context = context;
    }

    public static void parse(JSONObject jsonObject) throws JSONException {
        Log.d("VOl", "Token parse");
        accessToken = jsonObject.getString("accessToken");
        refreshToken = jsonObject.getString("refreshToken");
        role = jsonObject.getString("role");
        expiresIn = jsonObject.getLong("expiresIn");
        employeeId = jsonObject.getInt("employeeId");
        if (jsonObject.has("performerId"))
            performerId = jsonObject.getInt("performerId");

    }

    public static void set(String accessToken, String refreshToken, long expiresIn,
                           String role, int employeeId, int performerId){
        Token.accessToken = accessToken;
        Token.refreshToken = refreshToken;
        Token.expiresIn = expiresIn;
        Token.role = role;
        Token.employeeId = employeeId;
        Token.performerId = performerId;
    }

    public static void work(){
        Log.d("VOl","work");
        read();
        if (refreshToken == null){
            callAuthImpossible();
            return;
        }
        setTimeOut(getTimeBeforeExpires());
    }

    public static boolean isGood(){
        if (accessToken != null && refreshToken != null && expiresIn > 0 &&
                (getTimeBeforeExpires() > 0))
            return true;
        return false;
    }

    public static void addReadyListener(Callback callback){
        if (accessToken != null && refreshToken != null && expiresIn > 0 && getTimeBeforeExpires() > 5)
            callback.callingBack(null);
        else
            readyListeners.add(callback);
    }

    public static void addAuthImpossibleListener(Callback callback){
        if (authImpossibleListeners == null) Log.d("VOl", "auth = nu;ll");
        authImpossibleListeners.add(callback);
    }

    public static void clear(){
        accessToken = null;
        refreshToken = null;
        expiresIn = 0;
        role = null;
        employeeId = 0;
        performerId = 0;
        context = null;
    }


    private static void refresh(){
//        Log.d("VOl", "refresh: ");
        String req = null;
        try{
            req = http();
            parse(new JSONObject(req));
            if (!isGood()){
                Log.d("VOl", "refresh !isGood()");
                callAuthImpossible();
                return;
            }
            write();
            setTimeOut(getTimeBeforeExpires());
            callReady();
        } catch (JSONException |IOException e) {
            Log.d("VOl", "Не удалось обновить токен: " +e.getMessage());
            callAuthImpossible();
        }
    }

    public static long getTimeBeforeExpires(){
        Date date = new Date();
        long timeNow = (long)Math.floor(date.getTime()/1000);
        long t = expiresIn - timeNow - 7;
        return t > 0 ? t : 0;
    }

    private static void setTimeOut(long delay){
        Log.d("VOl", "setTimeOut: "+ delay);
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                refresh();
            }
        };
        timer.schedule(timerTask, delay*1000);
    }

    private static String http() throws IOException{
//        Log.d("VOl", "http: " + Thread.currentThread().getId());

        URL obj = new URL(urlRefresh);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        connection.setDoInput(true);
        connection.setRequestMethod("POST");

        String params = "refresh_token="+ refreshToken;
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.write(params.getBytes(Charset.forName("UTF-8")));
        wr.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static void callReady(){
        for (Callback call: readyListeners)
            call.callingBack(null);
        readyListeners.removeAll(readyListeners);
    }

    private static void callAuthImpossible(){
        Iterator<Callback> iterator = authImpossibleListeners.iterator();
        while (iterator.hasNext()){
            Callback callback = iterator.next();
            callback.callingBack(null);
        }

    }

}
