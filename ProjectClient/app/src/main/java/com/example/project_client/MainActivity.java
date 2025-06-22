package com.example.project_client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/*
Main Activity class showing the real-time data from Raspberry Pi Server
Using Sense Hat Temperature, Humidity, and Pressure Sensors.
 */
public class MainActivity extends AppCompatActivity implements WebSocketClientHandler.MessageListener
{
    private WebSocketClientHandler webSocketClientHandler;
    private TextView realTimeTemp, realTimeHum, realTimePres, errorMessageText;
    private TextView tempThreshold, humThreshold, presThreshold;

    private ImageView temperatureCheck, humidityCheck, pressureCheck;
    private Button viewChartsButton;
    private ImageView settingsImage;

    private String sendingTemperature, sendingHumidity, sendingPressure;

    private SharedPreferences settings_database;

    private static final long NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000;  // 5 minutes cooldown in milliseconds

    private long lastNotificationTimeTemp = 0;
    private long lastNotificationTimeHum = 0;
    private long lastNotificationTimePres = 0;

    private boolean isFirstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialise SharedPreferences
        settings_database = getSharedPreferences("settings_prefs", MODE_PRIVATE);

        // Initialise TextViews
        realTimeTemp = findViewById(R.id.realTimeTemp);
        realTimeHum = findViewById(R.id.realTimeHum);
        realTimePres = findViewById(R.id.realTimePres);
        errorMessageText = findViewById(R.id.errorMessageText);  // Add a TextView to show error messages

        tempThreshold = findViewById(R.id.tempTreshold);
        humThreshold = findViewById(R.id.humTreshold);
        presThreshold = findViewById(R.id.presTreshold);

        temperatureCheck = findViewById(R.id.temperatureCheck);
        humidityCheck = findViewById(R.id.humidityCheck);
        pressureCheck = findViewById(R.id.pressureCheck);

        viewChartsButton = findViewById(R.id.viewChartsButton);
        settingsImage = findViewById(R.id.settingsImage);

        // Setup WebSocket client
        webSocketClientHandler = new WebSocketClientHandler();
        webSocketClientHandler.setMessageListener(this);

        String savedIPAddress = settings_database.getString("saved_ip_address", "0");
        String savedPortNumber = settings_database.getString("saved_port_number", "0");
        String serverUrl = "ws://" + savedIPAddress + ":" + savedPortNumber;
//        Log.e("WebSocket", serverUrl);
        webSocketClientHandler.connectWebSocket(serverUrl);

        viewChartsButton.setOnClickListener(view ->
        {
            Intent i = new Intent(MainActivity.this, ChartViewScreen.class);
            startActivity(i);
        });

        settingsImage.setOnClickListener(view ->
        {
            Intent i = new Intent(MainActivity.this, SettingsScreen.class);
            startActivity(i);
        });

        temperatureCheck.setOnClickListener(view ->
        {
            boolean isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);
            if (!isThresholdEnabled)
            {
                String message = "TEMP_THRESHOLDS_DISABLED:" + sendingTemperature;
                sendMessageIfConnected(message);
                return;
            }

            String savedMinTempString = settings_database.getString("saved_min_temp", "0");
            String savedMaxTempString = settings_database.getString("saved_max_temp", "100");

            double savedMinTemp = Double.parseDouble(savedMinTempString);
            double savedMaxTemp = Double.parseDouble(savedMaxTempString);
            double tempValue = Double.parseDouble(sendingTemperature);

            String message;// Use the method to safely send message
            if(tempValue < savedMinTemp || tempValue > savedMaxTemp)
            {
                message = "TEMP_OUT_THRESHOLD:" + sendingTemperature;
            }
            else
            {
                message = "TEMP_IN_THRESHOLD:" + sendingTemperature;
            }
            sendMessageIfConnected(message);  // Use the method to safely send message
        });

        humidityCheck.setOnClickListener(view ->
        {
            boolean isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);
            if (!isThresholdEnabled)
            {
                String message = "HUM_THRESHOLDS_DISABLED:" + sendingHumidity;
                sendMessageIfConnected(message);
                return;
            }

            String savedMinHumString = settings_database.getString("saved_min_hum", "0");
            String savedMaxHumString = settings_database.getString("saved_max_hum", "100");

            double savedMinHum = Double.parseDouble(savedMinHumString);
            double savedMaxHum = Double.parseDouble(savedMaxHumString);
            double humValue = Double.parseDouble(sendingHumidity);

            String message;// Use the method to safely send message
            if(humValue < savedMinHum || humValue > savedMaxHum)
            {
                message = "HUM_OUT_THRESHOLD:" + sendingHumidity;
            }
            else
            {
                message = "HUM_IN_THRESHOLD:" + sendingHumidity;
            }
            sendMessageIfConnected(message);  // Use the method to safely send message
        });

        pressureCheck.setOnClickListener(view ->
        {
            boolean isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);
            if (!isThresholdEnabled)
            {
                String message = "PRES_THRESHOLDS_DISABLED:" + sendingPressure;
                sendMessageIfConnected(message);
                return;
            }

            String savedMinPresString = settings_database.getString("saved_min_pres", "0");
            String savedMaxPresString = settings_database.getString("saved_max_pres", "100");

            double savedMinPres = Double.parseDouble(savedMinPresString);
            double savedMaxPres = Double.parseDouble(savedMaxPresString);
            double presValue = Double.parseDouble(sendingPressure);

            String message;// Use the method to safely send message
            if(presValue < savedMinPres || presValue > savedMaxPres)
            {
                message = "PRES_OUT_THRESHOLD:" + sendingPressure;
            }
            else
            {
                message = "PRES_IN_THRESHOLD:" + sendingPressure;
            }
            sendMessageIfConnected(message);  // Use the method to safely send message
        });
    }

    /*
    Method checks if the server is connected before sending a message to the server.
     */
    private void sendMessageIfConnected(String message)
    {
        // Check if WebSocket is connected before sending the message
        if (webSocketClientHandler != null && webSocketClientHandler.isConnected())
        {
            webSocketClientHandler.sendMessage(message);
        }
        else
        {
            // Show an error message if the WebSocket is not connected
            errorMessageText.setText("Unable to send data. Server is not connected.");
            errorMessageText.setVisibility(View.VISIBLE);  // Optionally, show the error message
        }
    }

    /*
    Method receiving the data from the server and displaying it to the user.
     */
    @Override
    public void onSensorDataReceived(final String temperature, final String humidity, final String pressure)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                sendingTemperature = temperature;
                sendingHumidity = humidity;
                sendingPressure = pressure;

                realTimeTemp.setText(String.format("Temperature: %s °C", temperature));
                realTimeHum.setText(String.format("Humidity: %s %%", humidity));
                realTimePres.setText(String.format("Pressure: %s hPa", pressure));

                // Update the threshold values and change colors
                updateThresholdValues(temperature, humidity, pressure);
            }
        });
    }

    /*
    Method for disconnecting from the server if user closes the app.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        webSocketClientHandler.disconnectWebSocket();
    }

    /*
    Method resetting the values of the cooldown timer, first run, and for displaying the threshold
    values if the user navigates back to the main activity from another activity.
     */
    protected void onResume()
    {
        super.onResume();
        resetNotificationTimestamps();  // Reset cooldown timers
        updateThresholdValues(sendingTemperature, sendingHumidity, sendingPressure);
        isFirstRun = true;
    }

    /*
    Method showing error message, and displaying the data in blue and not showing any values
    for the data if the client cannot connect to the server.
     */
    @Override
    public void onConnectionError(final String errorMessage)
    {
        runOnUiThread(() ->
        {
            // Show error message and default values
            errorMessageText.setText(errorMessage);
            realTimeTemp.setText("Temperature: -- °C");
            realTimeHum.setText("Humidity: -- %");
            realTimePres.setText("Pressure: -- hPa");

            // Optionally, you can set the text color to red
            realTimeTemp.setTextColor(Color.BLUE);
            realTimeHum.setTextColor(Color.BLUE);
            realTimePres.setTextColor(Color.BLUE);
            webSocketClientHandler.disconnectWebSocket();
        });
    }

    private void updateThresholdValues(String temperature, String humidity, String pressure)
    {
        if (isFirstRun)
        {
            isFirstRun = false;
            return;
        }

        // Get the state of the threshold enabled setting from SharedPreferences
        boolean isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);

        // If the threshold is enabled, display the threshold values
        if (isThresholdEnabled)
        {
            String savedMinTemp = settings_database.getString("saved_min_temp", "0");
            String savedMaxTemp = settings_database.getString("saved_max_temp", "100");
            String savedMinHum = settings_database.getString("saved_min_hum", "0");
            String savedMaxHum = settings_database.getString("saved_max_hum", "100");
            String savedMinPres = settings_database.getString("saved_min_pres", "0");
            String savedMaxPres = settings_database.getString("saved_max_pres", "1000");

            // Concatenate min and max values with a comma separator
            tempThreshold.setText("Temperature: " + savedMinTemp + "°C, " + savedMaxTemp + "°C");
            humThreshold.setText("Humidity: " + savedMinHum + "%, " + savedMaxHum + "%");
            presThreshold.setText("Pressure: " + savedMinPres + "hPa, " + savedMaxPres + "hPa");

            // Set text color based on the threshold condition for the sensor readings
            setReadingColor(realTimeTemp, temperature, savedMinTemp, savedMaxTemp);
            setReadingColor(realTimeHum, humidity, savedMinHum, savedMaxHum);
            setReadingColor(realTimePres, pressure, savedMinPres, savedMaxPres);

            // Send notifications if the readings are outside the threshold
            checkAndNotify("Temperature", temperature, savedMinTemp, savedMaxTemp);
            checkAndNotify("Humidity", humidity, savedMinHum, savedMaxHum);
            checkAndNotify("Pressure", pressure, savedMinPres, savedMaxPres);

            // Make the threshold card visible
            //findViewById(R.id.cardThresholds).setVisibility(View.VISIBLE);
        }
        else
        {
            // If threshold is off, set values to '--'
            tempThreshold.setText("Temperature: --°C, --°C");
            humThreshold.setText("Humidity: --%, --%");
            presThreshold.setText("Pressure: --hPa, --hPa");

            // Reset text color to default (gray)
            realTimeTemp.setTextColor(Color.GRAY);
            realTimeHum.setTextColor(Color.GRAY);
            realTimePres.setTextColor(Color.GRAY);

            // Hide the threshold card if "Off"
            //findViewById(R.id.cardThresholds).setVisibility(View.GONE);
        }
    }

    /*
    This method sets the color based on the sensor data and thresholds.
     */
    private void setReadingColor(TextView textView, String reading, String minThreshold, String maxThreshold)
    {
        try
        {
            double readingValue = Double.parseDouble(reading);
            double min = Double.parseDouble(minThreshold);
            double max = Double.parseDouble(maxThreshold);

            if (readingValue < min || readingValue > max)
            {
                // Out of threshold range, set text color to red
                textView.setTextColor(Color.rgb(230, 0, 0));
            }
            else
            {
                // Within threshold range, set text color to green
                textView.setTextColor(Color.rgb(0, 180, 0));
            }
        }
        catch (NumberFormatException e)
        {
            // If parsing fails, set text color to default (magenta)
            textView.setTextColor(Color.MAGENTA);
        }
    }

    /*
    This method handles all the readings from the server. For each data reading it checks
    if it is under or above the threshold. If data is out of the thresholds it creates a notification
    message.
    The method also handles the cooldown timer. It sends notification every given time instead of
    every second. If the cooldown timer is not expired it will not send a notification.
     */
    private void checkAndNotify(String sensorType, String value, String minThreshold, String maxThreshold)
    {
        try
        {
            double readingValue = Double.parseDouble(value);
            double min = Double.parseDouble(minThreshold);
            double max = Double.parseDouble(maxThreshold);

            long currentTime = SystemClock.elapsedRealtime();

            switch (sensorType)
            {
                // Notification for the temperature.
                case "Temperature":
                    if (currentTime - lastNotificationTimeTemp < NOTIFICATION_COOLDOWN_MS)
                    {
                        return;  // Don't send a notification if we're still in cooldown for temperature
                    }

                    if (readingValue < min)
                    {
                        String message = sensorType + " is below threshold! (" + value + ")";
                        NotificationHelper.showNotificationTemperature(MainActivity.this, sensorType + " Below Threshold", message);
                    }
                    else if (readingValue > max)
                    {
                        String message = sensorType + " is above threshold! (" + value + ")";
                        NotificationHelper.showNotificationTemperature(MainActivity.this, sensorType + " Above Threshold", message);
                    }
                    lastNotificationTimeTemp = currentTime;  // Update the last notification time for temperature

                    break;

                // Notification for the humidity.
                case "Humidity":
                    if (currentTime - lastNotificationTimeHum < NOTIFICATION_COOLDOWN_MS)
                    {
                        return;  // Don't send a notification if we're still in cooldown for humidity
                    }

                    if (readingValue < min)
                    {
                        String message = sensorType + " is below threshold! (" + value + ")";
                        NotificationHelper.showNotificationHumidity(MainActivity.this, sensorType + " Below Threshold", message);
                    }
                    else if (readingValue > max)
                    {
                        String message = sensorType + " is above threshold! (" + value + ")";
                        NotificationHelper.showNotificationHumidity(MainActivity.this, sensorType + " Above Threshold", message);
                    }
                    lastNotificationTimeHum = currentTime;  // Update the last notification time for humidity

                    break;

                // Notification for the pressure.
                case "Pressure":
                    if (currentTime - lastNotificationTimePres < NOTIFICATION_COOLDOWN_MS)
                    {
                        return;  // Don't send a notification if we're still in cooldown for pressure
                    }

                    if (readingValue < min)
                    {
                        String message = sensorType + " is below threshold! (" + value + ")";
                        NotificationHelper.showNotificationPressure(MainActivity.this, sensorType + " Below Threshold", message);
                    }
                    else if (readingValue > max)
                    {
                        String message = sensorType + " is above threshold! (" + value + ")";
                        NotificationHelper.showNotificationPressure(MainActivity.this, sensorType + " Above Threshold", message);
                    }
                    lastNotificationTimePres = currentTime;  // Update the last notification time for pressure

                    break;
            }
        }
        catch (NumberFormatException e)
        {
            // Handle any parsing errors here
        }
    }

    /*
    Updates the visibility of sensor buttons (temperature, humidity, pressure)
    based on the WebSocket connection status.
    If connected, buttons are visible and clickable.
    If not connected, buttons are hidden to prevent user interaction.
     */
    private void updateSensorButtonsVisibility(boolean isConnected)
    {
        if (isConnected)
        {
            pressureCheck.setEnabled(true);
            temperatureCheck.setEnabled(true);
            humidityCheck.setEnabled(true);
        }
        else
        {
            pressureCheck.setEnabled(false);
            temperatureCheck.setEnabled(false);
            humidityCheck.setEnabled(false);
        }
    }

    /*
    Called when the WebSocketClientHandler updates the connection status.
    Runs on the UI thread to safely update button visibility based on status.
     */
    @Override
    public void onConnectionStatusChanged(boolean isConnected)
    {
        runOnUiThread(() -> updateSensorButtonsVisibility(isConnected));
    }

    /*
    This method resets the cooldown timer when the user returns to the main activity
    from another activity.
     */
    private void resetNotificationTimestamps()
    {
        lastNotificationTimeTemp = 0;
        lastNotificationTimeHum = 0;
        lastNotificationTimePres = 0;
    }
}
