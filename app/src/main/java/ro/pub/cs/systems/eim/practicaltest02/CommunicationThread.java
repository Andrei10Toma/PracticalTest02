package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Time;
import java.sql.Timestamp;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {
    private final ServerThread serverThread;
    private final Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }

        try {
            BufferedReader reader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            String pageSourceCode = "";

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (pokemon name)");
            String coinValue = reader.readLine();
            if (coinValue == null || coinValue.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (pokemon name)");
                return;
            }

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
            if (serverThread.getData(coinValue) != null && System.currentTimeMillis() - serverThread.getData(coinValue).updateTimestamp < 10000) {
                Log.d(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                CoinInformation coinInformation = serverThread.getData(coinValue);
                if (System.currentTimeMillis() - coinInformation.updateTimestamp < 10000) {
                    printWriter.println(coinInformation.value);
                    printWriter.flush();
                    return;
                }
            } else {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + "EUR.json");
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    pageSourceCode = EntityUtils.toString(httpGetEntity);
                }
                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else Log.i(Constants.TAG, pageSourceCode);
                JSONObject content = new JSONObject(pageSourceCode);
                JSONObject timeContent = content.getJSONObject("time");
                JSONObject usdContent = content.getJSONObject("bpi").getJSONObject("USD");
                JSONObject eurContent = content.getJSONObject("bpi").getJSONObject("EUR");
                Log.d(Constants.TAG, timeContent.getString("updated"));
                Log.d(Constants.TAG, usdContent.getString("rate"));
                Log.d(Constants.TAG, eurContent.getString("rate"));

                if (coinValue.equals("USD")) {
                    printWriter.println(usdContent.getString("rate"));
                    printWriter.flush();
                    CoinInformation coinInformation = new CoinInformation(usdContent.getString("rate"), System.currentTimeMillis());
                    serverThread.setData(coinValue, coinInformation);
                } else if (coinValue.equals("EUR")) {
                    printWriter.println(eurContent.getString("rate"));
                    printWriter.flush();
                    CoinInformation coinInformation = new CoinInformation(usdContent.getString("rate"), System.currentTimeMillis());
                    serverThread.setData(coinValue, coinInformation);
                } else {
                    printWriter.println("Invalid coin");
                    printWriter.flush();
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + e.getMessage());
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
