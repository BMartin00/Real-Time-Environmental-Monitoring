package com.example.project_client;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
ChartViewScreen activity displays real-time sensor data in line charts for temperature,
humidity, and pressure. The user can toggle between different charts and see threshold lines
when thresholds are enabled in settings.
 */
public class ChartViewScreen extends AppCompatActivity implements WebSocketClientHandler.MessageListener
{
    private Button showTemperatureButton, showHumidityButton, showPressureButton;
    private LineChart temperatureChart, humidityChart, pressureChart;
    private WebSocketClientHandler webSocketClientHandler;
    private List<Entry> temperatureEntries = new ArrayList<>();
    private List<Entry> humidityEntries = new ArrayList<>();
    private List<Entry> pressureEntries = new ArrayList<>();
    private int timeIndex = 0;

    private SharedPreferences settings_database;

    private Handler handler = new Handler(); // Handler to schedule periodic updates
    private Runnable updateRunnable; // Runnable to update charts every 2 minutes

    // Buffer lists to store incoming data
    private List<Float> temperatureBuffer = new ArrayList<>();
    private List<Float> humidityBuffer = new ArrayList<>();
    private List<Float> pressureBuffer = new ArrayList<>();

    // Threshold values for the sensors
    private float minTempThreshold, maxTempThreshold;
    private float minHumThreshold, maxHumThreshold;
    private float minPresThreshold, maxPresThreshold;
    private boolean isThresholdEnabled;

    private float timeInterval;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chart_view_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize charts
        temperatureChart = findViewById(R.id.tempChart);
        humidityChart = findViewById(R.id.humidityChart);
        pressureChart = findViewById(R.id.pressureChart);

        // Setup chart configurations
        setupChart(temperatureChart, "Temperature Data");
        setupChart(humidityChart, "Humidity Data");
        setupChart(pressureChart, "Pressure Data");

        // Initialise buttons
        showTemperatureButton = findViewById(R.id.showTemperatureButton);
        showHumidityButton = findViewById(R.id.showHumidityButton);
        showPressureButton = findViewById(R.id.showPressureButton);

        // Set click listeners for buttons
        showTemperatureButton.setOnClickListener(v -> showChart(temperatureChart));
        showHumidityButton.setOnClickListener(v -> showChart(humidityChart));
        showPressureButton.setOnClickListener(view -> showChart(pressureChart));

        // Initialize WebSocket and SharedPreferences
        webSocketClientHandler = new WebSocketClientHandler();
        webSocketClientHandler.setMessageListener(this);

        settings_database = getSharedPreferences("settings_prefs", MODE_PRIVATE);

        minTempThreshold = Float.parseFloat(settings_database.getString("saved_min_temp", "0"));
        maxTempThreshold = Float.parseFloat(settings_database.getString("saved_max_temp", "100"));
        minHumThreshold = Float.parseFloat(settings_database.getString("saved_min_hum", "0"));
        maxHumThreshold = Float.parseFloat(settings_database.getString("saved_max_hum", "100"));
        minPresThreshold = Float.parseFloat(settings_database.getString("saved_min_pres", "0"));
        maxPresThreshold = Float.parseFloat(settings_database.getString("saved_max_pres", "1000"));

        timeInterval = Float.parseFloat(settings_database.getString("saved_time_interval", "1000"));

        isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);

        String savedIPAddress = settings_database.getString("saved_ip_address", "0");
        String savedPortNumber = settings_database.getString("saved_port_number", "0");

        String serverUrl = "ws://" + savedIPAddress + ":" + savedPortNumber;
        webSocketClientHandler.connectWebSocket(serverUrl);

        // Set up a Runnable to update the charts every given time interval
        updateRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                // Add only the last value from the buffer to each chart (one entry for each sensor)
                addBufferedDataToCharts();

                // Clear buffers after updating the chart
                temperatureBuffer.clear();
                humidityBuffer.clear();
                pressureBuffer.clear();

                // Schedule the next update after given time interval
                handler.postDelayed(this, (long) timeInterval);  // Correcting this to ensure it happens after given time interval.
            }
        };
        // Start the first update after given time interval
        handler.postDelayed(updateRunnable, (long) timeInterval);  // Start the given time interval here

        // Initially show the temperature chart
        showChart(temperatureChart);
    }

    /*
    This method configures a LineChart with basic settings.
     */
    private void setupChart(LineChart chart, String descriptionText)
    {
        Description description = new Description();
        description.setText(descriptionText);
        chart.setDescription(description);
        chart.setData(new LineData());
        chart.invalidate();
    }

    /*
    This method shows the specified chart while hiding the other charts.
     */
    private void showChart(LineChart chartToShow)
    {
        // Hide all charts
        temperatureChart.setVisibility(View.GONE);
        humidityChart.setVisibility(View.GONE);
        pressureChart.setVisibility(View.GONE);

        // Show the selected chart
        chartToShow.setVisibility(View.VISIBLE);
    }

    /*
    Callback for receiving new sensor data from WebSocket.
    Stores data in buffers for later chart updates.
     */
    @Override
    public void onSensorDataReceived(String temperature, String humidity, String pressure)
    {
        runOnUiThread(() ->
        {
            try
            {
                // Store the incoming data in buffers (every reading, but only one is used for each 2-minute period)
                float temperatureReading = Float.parseFloat(temperature);
                float humidityReading = Float.parseFloat(humidity);
                float pressureReading = Float.parseFloat(pressure);

                temperatureBuffer.add(temperatureReading);
                humidityBuffer.add(humidityReading);
                pressureBuffer.add(pressureReading);
            }
            catch (NumberFormatException e)
            {
                Log.e("ChartViewScreen", "Error parsing sensor data", e);
            }
        });
    }

    // Add this with your other class fields
    private final ValueFormatter twoDecimalFormatter = new ValueFormatter()
    {
        @Override
        public String getFormattedValue(float value)
        {
            return String.format(Locale.US, "%.2f", value);
        }
    };

    /*
    Adds the latest buffered data points to each chart and updates the display.
     */
    private void addBufferedDataToCharts()
    {
        if (temperatureEntries.isEmpty())
        {
            temperatureEntries.add(new Entry(0, temperatureBuffer.get(temperatureBuffer.size() - 1)));
            temperatureEntries.add(new Entry(timeIndex, temperatureBuffer.get(temperatureBuffer.size() - 1)));
        }

        // Only add the latest data from the buffer for each sensor (temperature, humidity, pressure)
        if (!temperatureBuffer.isEmpty())
        {
            temperatureEntries.add(new Entry(timeIndex, temperatureBuffer.get(temperatureBuffer.size() - 1)));
        }
        if (!humidityBuffer.isEmpty())
        {
            humidityEntries.add(new Entry(timeIndex, humidityBuffer.get(humidityBuffer.size() - 1)));
        }
        if (!pressureBuffer.isEmpty())
        {
            pressureEntries.add(new Entry(timeIndex, pressureBuffer.get(pressureBuffer.size() - 1)));
        }
        timeIndex++;

        // Create and update LineDataSets for sensor data
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperature");
        LineDataSet humidityDataSet = new LineDataSet(humidityEntries, "Humidity");
        LineDataSet pressureDataSet = new LineDataSet(pressureEntries, "Pressure");

        temperatureDataSet.setValueFormatter(twoDecimalFormatter); // Apply formatter
        temperatureDataSet.setColor(Color.GREEN);
        temperatureDataSet.setLineWidth(4f);
        temperatureDataSet.setCircleRadius(5f);
        temperatureDataSet.setCircleHoleRadius(3.5f);
        temperatureDataSet.setValueTextSize(10f);

        humidityDataSet.setValueFormatter(twoDecimalFormatter); // Apply formatter
        humidityDataSet.setColor(Color.GREEN);
        humidityDataSet.setLineWidth(4f);
        humidityDataSet.setCircleRadius(5f);
        humidityDataSet.setCircleHoleRadius(3.5f);
        humidityDataSet.setValueTextSize(10f);

        pressureDataSet.setValueFormatter(twoDecimalFormatter); // Apply formatter
        pressureDataSet.setColor(Color.GREEN);
        pressureDataSet.setLineWidth(4f);
        pressureDataSet.setCircleRadius(5f);
        pressureDataSet.setCircleHoleRadius(3.5f);
        pressureDataSet.setValueTextSize(10f);

        LineDataSet temperatureMinThresholdSet;
        LineDataSet temperatureMaxThresholdSet;
        LineDataSet humidityMinThresholdSet;
        LineDataSet humidityMaxThresholdSet;
        LineDataSet pressureMinThresholdSet;
        LineDataSet pressureMaxThresholdSet;

        LineData temperatureData;
        LineData humidityData;
        LineData pressureData;

        if (isThresholdEnabled)
        {
            // Create and add threshold lines as separate data sets
            temperatureMinThresholdSet = createThresholdLine(minTempThreshold);
            temperatureMaxThresholdSet = createThresholdLine(maxTempThreshold);
            humidityMinThresholdSet = createThresholdLine(minHumThreshold);
            humidityMaxThresholdSet = createThresholdLine(maxHumThreshold);
            pressureMinThresholdSet = createThresholdLine(minPresThreshold);
            pressureMaxThresholdSet = createThresholdLine(maxPresThreshold);

            // Create LineData for all data sets
            temperatureData = new LineData(temperatureDataSet, temperatureMinThresholdSet, temperatureMaxThresholdSet);
            humidityData = new LineData(humidityDataSet, humidityMinThresholdSet, humidityMaxThresholdSet);
            pressureData = new LineData(pressureDataSet, pressureMinThresholdSet, pressureMaxThresholdSet);
        }
        else
        {
            // Create LineData for all data sets
            temperatureData = new LineData(temperatureDataSet);
            humidityData = new LineData(humidityDataSet);
            pressureData = new LineData(pressureDataSet);
        }

        // Update the charts
        temperatureChart.setData(temperatureData);
        humidityChart.setData(humidityData);
        pressureChart.setData(pressureData);

        temperatureChart.invalidate();
        humidityChart.invalidate();
        pressureChart.invalidate();
    }

    /*
    Creates horizontal threshold lines at the specified values.
     */
    private LineDataSet createThresholdLine(float thresholdValue)
    {
        List<Entry> thresholdEntries = new ArrayList<>();
        thresholdEntries.add(new Entry(0, thresholdValue)); // Start of the line
        thresholdEntries.add(new Entry(timeIndex, thresholdValue)); // End of the line

        LineDataSet thresholdSet = new LineDataSet(thresholdEntries, null);
        thresholdSet.setColor(Color.RED);
        thresholdSet.setLineWidth(2f);
        thresholdSet.setDrawCircles(false);
        thresholdSet.setDrawValues(false);
        thresholdSet.setHighlightEnabled(false);
        thresholdSet.setForm(Legend.LegendForm.NONE); // Remove color indicator from legend
        return thresholdSet;
    }

    /*
    Method for connection error between the client and the server.
     */
    @Override
    public void onConnectionError(String errorMessage)
    {

    }

    /*
    Called when the WebSocketClientHandler updates the connection status.
    Runs on the UI thread to safely update button visibility based on status.
     */
    @Override
    public void onConnectionStatusChanged(boolean isConnected)
    {

    }

    /*
    This method disconnects from the WebSocket server and stops periodic updates when
    the user closes the activity.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (webSocketClientHandler != null)
        {
            webSocketClientHandler.disconnectWebSocket();
        }
        // Stop the periodic update when the activity is destroyed
        handler.removeCallbacks(updateRunnable);  // Remove callback to stop periodic updates
    }

//    @Override
//    protected void onPause()
//    {
//        super.onPause();
//        Intent i = new Intent(ChartViewScreen.this, MainActivity.class);
//        startActivity(i);
//    }
}
