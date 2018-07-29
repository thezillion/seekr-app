package com.prince.root.seekr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    WifiManager mainWifiObj;
    WifiScanReceiver wifiReciever;
    String wifis[];
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    private OkHttpClient client;
    String wifiNo = "0";

    private final int interval = 1000; // 1 Second
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable(){
        public void run() {
            TextView connectedTickerMessage = (TextView) findViewById(R.id.connectedTickerMessage);
            connectedTickerMessage.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        this.setContentView(R.layout.activity_main);

        registerReceiver(wifiReciever, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        Button connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToSeekr();
            }
        });

        mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReciever = new WifiScanReceiver();
        mainWifiObj.startScan();
    }

    protected void connectToSeekr() {
        Log.d("Hehe", "Connecting");
        client = new OkHttpClient();
        Log.d("Hehe", wifiNo);
        Request request = new Request.Builder().url("ws://192.168."+wifiNo+".1").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        handler.postAtTime(runnable, System.currentTimeMillis()+interval);
        handler.postDelayed(runnable, interval);
    }

    protected void onPause() {
//        unregisterReceiver(wifiReciever);
        super.onPause();
    }

    protected void onResume() {
//        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    List<ScanResult> wifiScanList;

    class WifiScanReceiver extends BroadcastReceiver {


        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            Log.d("Hello", "World");

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

            ConnectivityManager conMan = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (wifiNo != "0" && netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Request request = new Request.Builder().url("ws://192.168." + wifiNo + ".1").build();
                EchoWebSocketListener listener = new EchoWebSocketListener();
                WebSocket ws = client.newWebSocket(request, listener);
            } else if (wifiNo == "0")
                Log.d("WifiReceiver", "Don't have Wifi Connection");


        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // TODO: What you want to do when it works or maybe .PERMISSION_DENIED if it works better
            wifiScanList = mainWifiObj.getScanResults();
            doRestConnection();
        }
    }

    protected void doRestConnection() {
        wifis = new String[wifiScanList.size()];
        for(int i = 0; i < wifiScanList.size(); i++){
            wifis[i] = ((wifiScanList.get(i)).toString());
        }
        String filtered[] = new String[wifiScanList.size()];

        int counter = 0;
        for (String eachWifi : wifis) {
            String[] temp = eachWifi.split(",");

            filtered[counter] = temp[0].substring(5).trim();//+"\n" + temp[2].substring(12).trim()+"\n" +temp[3].substring(6).trim();//0->SSID, 2->Key Management 3-> Strength
            Log.d("Hehe", filtered[counter]);
            if (filtered[counter].length() >= 5) {
                if (filtered[counter].substring(0, 5).equals("SEEKR") == true) {
                    finallyConnect("12345678", filtered[counter]);
                    break;
                }

            }

            counter++;
        }
    }

    private void finallyConnect(String networkPass, String networkSSID) {
        wifiNo = networkSSID.substring(5);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", networkSSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", networkPass);

        // remember id
        int netId = mainWifiObj.addNetwork(wifiConfig);
        mainWifiObj.disconnect();
        mainWifiObj.enableNetwork(netId, true);
        mainWifiObj.reconnect();

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"\"" + networkSSID + "\"\"";
        conf.preSharedKey = "\"" + networkPass + "\"";
        mainWifiObj.addNetwork(conf);

//        registerReceiver(wifiReciever, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

//        ConnectivityManager conMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
////        while (netInfo == null || netInfo.getType() != ConnectivityManager.TYPE_WIFI) System.out.println(netInfo.getType());
//        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//            Request request = new Request.Builder().url("ws://192.168."+wifiNo+".1").build();
//            EchoWebSocketListener listener = new EchoWebSocketListener();
//            WebSocket ws = client.newWebSocket(request, listener);
//        } else
//            Log.d("WifiReceiver", "Don't have Wifi Connection");
    }

    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d("Hehe", "Balooney");
            webSocket.send("Hello, it's SSaurel !");
            webSocket.send("What's up ?");
            webSocket.send(ByteString.decodeHex("deadbeef"));
            webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d("WS", "Receiving : " + text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.d("WS", "Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.d("WS", "Closing : " + code + " / " + reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d("WS","Error : " + t.getMessage());
        }
    }
}