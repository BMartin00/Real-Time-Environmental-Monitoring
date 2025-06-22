package com.example.project_client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import android.util.Log;

import org.json.JSONObject;  // Import for JSON parsing

import java.net.URI;
import java.net.URISyntaxException;

/*
WebSocketClientHandler manages the WebSocket connection to the server,
handles incoming sensor data messages, and provides connection status.
It parses JSON data from the server and notifies listeners about
new sensor data or connection errors.
 */
public class WebSocketClientHandler
{
    private WebSocketClient webSocketClient;
    private MessageListener messageListener;
    private boolean isConnected = false;

    /*
    Interface for receiving WebSocket events and sensor data updates.
     */
    public interface MessageListener
    {
        /*
        Called when new sensor data is received from the server.
         */
        void onSensorDataReceived(String temperature, String humidity, String pressure);

        /*
        Called when a connection error occurs.
         */
        void onConnectionError(String errorMessage);

        /*
        Called when the WebSocket connection status changes.
        This is used to update the UI to enable/disable sensor interaction.
         */
        void onConnectionStatusChanged(boolean isConnected);
    }

    /*
    Sets the listener for WebSocket events and data updates.
     */
    public void setMessageListener(MessageListener listener)
    {
        this.messageListener = listener;
    }

    /*
    Establishes a WebSocket connection to the specified server URL.
     */
    public void connectWebSocket(String serverUrl)
    {
        try
        {
            URI uri = new URI(serverUrl);
            webSocketClient = new WebSocketClient(uri)
            {
                /*
                Called when the WebSocket successfully connects to the server.
                Sets the internal `isConnected` flag to true and notifies the listener (Activity)
                to update the UI (e.g. show the sensor buttons).
                 */
                @Override
                public void onOpen(ServerHandshake handshakeData)
                {
                    isConnected = true;
//                    Log.d("WebSocket", "Connected to WebSocket server");
                    if (messageListener != null)
                    {
                        messageListener.onConnectionStatusChanged(true);
                    }
                }

                /*
                Called when a message is received from the server.
                 */
                @Override
                public void onMessage(String message)
                {
//                    Log.d("WebSocket", "Received data: " + message);
                    processSensorData(message);
                }

                /*
                Called when the WebSocket connection is closed (by server or client).
                Notifies the listener that connection is lost.
                 */
                @Override
                public void onClose(int code, String reason, boolean remote)
                {
                    isConnected = false;
//                    Log.d("WebSocket", "Closed with reason: " + reason);
                    if (messageListener != null)
                    {
                        messageListener.onConnectionError("Server connection lost. Please try again.");
                        messageListener.onConnectionStatusChanged(false);
                    }
                }

                /*
                Called when there is a WebSocket error.
                Sets `isConnected` to false and notifies the listener to update UI accordingly.
                 */
                @Override
                public void onError(Exception ex)
                {
                    isConnected = false;
                    if (messageListener != null)
                    {
                        messageListener.onConnectionStatusChanged(false);
                    }
                }
            };

            webSocketClient.connect();
        }
        catch (URISyntaxException e)
        {
            isConnected = false;
            e.printStackTrace();
            if (messageListener != null)
            {
                messageListener.onConnectionError("Invalid server URL.");
            }
        }
    }

    /*
    Parses JSON sensor data and notifies the listener.
     */
    private void processSensorData(String message)
    {
        try
        {
            JSONObject jsonObject = new JSONObject(message);
            String temperature = String.format("%.2f", jsonObject.getDouble("temperature"));
            String humidity = String.format("%.2f", jsonObject.getDouble("humidity"));
            String pressure = String.format("%.2f", jsonObject.getDouble("pressure"));

            if (messageListener != null)
            {
                messageListener.onSensorDataReceived(temperature, humidity, pressure);
            }
        }
        catch (Exception e)
        {
            Log.e("WebSocket", "Error parsing JSON: " + e.getMessage());
            if (messageListener != null)
            {
                messageListener.onConnectionError("Error parsing sensor data.");
            }
        }
    }

   /*
   Closes the WebSocket connection if it exists.
    */
    public void disconnectWebSocket()
    {
        if (webSocketClient != null)
        {
            webSocketClient.close();
            isConnected = false;
        }
    }

    /*
    Sends a message through the WebSocket connection.
     */
    public void sendMessage(String message)
    {
        if (webSocketClient != null)
        {
            webSocketClient.send(message);
        }
    }

    /*
    Checks if the WebSocket is currently connected.
     */
    public boolean isConnected()
    {
        return isConnected;
    }
}
