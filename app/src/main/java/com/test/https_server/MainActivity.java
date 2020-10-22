package com.test.https_server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;

import fi.iki.elonen.NanoHTTPD;

import static java.net.HttpURLConnection.HTTP_OK;

public class MainActivity extends AppCompatActivity {
    Context context=this;
    final String TAG="HTTPS_SERVER";

    private static final int PORT = 8000;
    final String hostname="localhost";
    private TextView hello;
    private MyHTTPD server;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hello = (TextView) findViewById(R.id.hello);
    }

    @Override
    public void onResume(){
        super.onResume();

        TextView textIpaddr = (TextView) findViewById(R.id.ipaddr);

        String ipAddress = getLocalIpAddress();
        textIpaddr.setText("Please access! http://" + ipAddress + ":" + PORT);

        try {
            server = new MyHTTPD();
            Log.d(TAG, "server start...");
            server.myStart();
        } catch (IOException e) {
            Log.d(TAG, "server start failed: "+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

/*
        if (server != null) {
            Log.d(TAG, "server stop...");
            server.stop();
        }
*/
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface)en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress)enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Exception", ex.toString());
        }
        return null;
    }

    /*
        > keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048 -ext SAN=DNS:localhost,IP:127.0.0.1  -validity 9999

        public class HttpsExample  extends NanoHTTPD {

            public HttpsExample() throws IOException {
                super(8080);
                makeSecure(NanoHTTPD.makeSSLSocketFactory(
                  "/keystore.jks", "password".toCharArray()), null);
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            }

            // main and serve methods
        }
     */
    private class MyHTTPD extends NanoHTTPD {

        public MyHTTPD() throws IOException {
            super(hostname, PORT);
            Log.d(TAG, "makeSecure...");
            KeyStore keyStore=null;
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
// keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.bks -storepass password -validity 9999 -keysize 2048 -ext SAN=DNS:localhost,IP:127.0.0.1  -validity 9999
                InputStream keyStoreStream = context.getAssets().open("keystore.bks");
                keyStore.load(keyStoreStream, "password".toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, "myCertificatePass".toCharArray());
                makeSecure(NanoHTTPD.makeSSLSocketFactory(keyStore, keyManagerFactory), null);
            }catch(Exception ex){
                Log.d(TAG,"keystore opperations: "+ex.getMessage());
            }
            //makeSecure(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), null);

        }

        void myStart(){
            Log.d(TAG, "start...");
            try {
                this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            }catch(Exception ex){
                Log.d(TAG,"start failed: "+ex.getMessage());
            }
        }
        @Override
        public Response serve(String uri, Method method, Map<String,String> headers, Map<String, String> parms, Map<String, String> files) {
            Log.d(TAG, "respose...");
            final StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> kv : headers.entrySet())
                buf.append(kv.getKey() + " : " + kv.getValue() + "\n");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    hello.setText(buf);
                }
            });

            final String html = "<html><head><head><body><h1>Hello, World</h1></body></html>";
            Log.d(TAG, "send response...");
            return newFixedLengthResponse(html);
//            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, html);
        }
    }
}