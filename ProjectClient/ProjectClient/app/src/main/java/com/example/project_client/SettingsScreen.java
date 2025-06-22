package com.example.project_client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/*
Settings Screen lets the user change the threshold values for temperature, humidity, and pressure.
Change the time interval for the charts, and set the IP Address and Port Number of the server.
 */
public class SettingsScreen extends AppCompatActivity
{
    private Button updateButton;

    private TextView minTemp, maxTemp, minHum, maxHum, minPres, maxPres;
    private TextView timeInterval;
    private TextView enteredIP, enteredPort;

    private RadioGroup thresholdSelectGroup;
    private RadioButton onButton, offButton;

    private SharedPreferences settings_database;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        updateButton = findViewById(R.id.updateButton);

        minTemp = findViewById(R.id.minTemp);
        maxTemp = findViewById(R.id.maxTemp);
        minHum = findViewById(R.id.minHum);
        maxHum = findViewById(R.id.maxHum);
        minPres = findViewById(R.id.minPres);
        maxPres = findViewById(R.id.maxPres);
        timeInterval = findViewById(R.id.timeInterval);
        enteredIP = findViewById(R.id.enteredIP);
        enteredPort = findViewById(R.id.enteredPort);

        thresholdSelectGroup = findViewById(R.id.thresholdSelectGroup);
        onButton = findViewById(R.id.onButton);
        offButton = findViewById(R.id.offButton);

        // Load SharedPreferences
        settings_database = getApplicationContext().getSharedPreferences("settings_prefs", MODE_PRIVATE);

        // Load saved values
        minTemp.setText(settings_database.getString("saved_min_temp", ""));
        maxTemp.setText(settings_database.getString("saved_max_temp", ""));
        minHum.setText(settings_database.getString("saved_min_hum", ""));
        maxHum.setText(settings_database.getString("saved_max_hum", ""));
        minPres.setText(settings_database.getString("saved_min_pres", ""));
        maxPres.setText(settings_database.getString("saved_max_pres", ""));
        timeInterval.setText(settings_database.getString("saved_time_interval", ""));
        enteredIP.setText(settings_database.getString("saved_ip_address", ""));
        enteredPort.setText(settings_database.getString("saved_port_number", ""));

        // Load threshold setting
        boolean isThresholdEnabled = settings_database.getBoolean("threshold_enabled", false);
        if (isThresholdEnabled)
        {
            onButton.setChecked(true);
        }
        else
        {
            offButton.setChecked(true);
        }

        // Enable/Disable input fields based on threshold state
        toggleThresholdInputs(isThresholdEnabled);

        // Handle radio button changes
        thresholdSelectGroup.setOnCheckedChangeListener((group, checkedId) ->
        {
            boolean enabled = checkedId == R.id.onButton;
            toggleThresholdInputs(enabled);
        });

        updateButton.setOnClickListener(view ->
        {
            SharedPreferences.Editor editor = settings_database.edit();

            String minTempValue = minTemp.getText().toString();
            String maxTempValue = maxTemp.getText().toString();
            String minHumValue = minHum.getText().toString();
            String maxHumValue = maxHum.getText().toString();
            String minPresValue = minPres.getText().toString();
            String maxPresValue = maxPres.getText().toString();

            String enteredTimeInterval = timeInterval.getText().toString();

            String enteredIPText = enteredIP.getText().toString();
            String enteredPortText = enteredPort.getText().toString();

            try
            {
                double minTempNum = Double.parseDouble(minTempValue);
                double maxTempNum = Double.parseDouble(maxTempValue);
                double minHumNum = Double.parseDouble(minHumValue);
                double maxHumNum = Double.parseDouble(maxHumValue);
                double minPresNum = Double.parseDouble(minPresValue);
                double maxPresNum = Double.parseDouble(maxPresValue);
                Double.parseDouble(enteredTimeInterval);

                // Ensure min values are less than max values
                if (minTempNum >= maxTempNum || minHumNum >= maxHumNum || minPresNum >= maxPresNum)
                {
                    Toast.makeText(SettingsScreen.this, "The minimum value cannot be bigger or equal than the maximum value!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Temperature validation (-30 to 105°C)
                if (minTempNum < -30 || minTempNum > 105 || maxTempNum > 105)
                {
                    Toast.makeText(SettingsScreen.this, "Temperature has to be between -30°C and 105°C!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Humidity validation (0% to 100%)
                if (minHumNum < 0 || minHumNum > 100 || maxHumNum > 100)
                {
                    Toast.makeText(SettingsScreen.this, "Humidity has to be between 0% and 100%!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Pressure validation (260 to 1260 hPa)
                if (minPresNum < 260 || minPresNum > 1260 || maxPresNum > 1260)
                {
                    Toast.makeText(SettingsScreen.this, "Pressure has to be between 260hPa and 1260hPa!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (enteredIPText.isEmpty())
                {
                    Toast.makeText(SettingsScreen.this, "You have to enter an IP Address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (enteredPortText.isEmpty())
                {
                    Toast.makeText(SettingsScreen.this, "You have to enter a Port!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If all validations pass, save the settings
                editor.putString("saved_min_temp", minTempValue);
                editor.putString("saved_max_temp", maxTempValue);
                editor.putString("saved_min_hum", minHumValue);
                editor.putString("saved_max_hum", maxHumValue);
                editor.putString("saved_min_pres", minPresValue);
                editor.putString("saved_max_pres", maxPresValue);
                editor.putString("saved_time_interval", enteredTimeInterval);
                editor.putString("saved_ip_address", enteredIPText);
                editor.putString("saved_port_number", enteredPortText);

                // Save threshold state
                boolean isEnabled = onButton.isChecked();
                editor.putBoolean("threshold_enabled", isEnabled);

                editor.apply();

                Toast.makeText(SettingsScreen.this, "Settings Updated!", Toast.LENGTH_SHORT).show();
            }
            catch (NumberFormatException e)
            {
                Toast.makeText(SettingsScreen.this, "Invalid input! Please enter valid numbers.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
    This method turns on or off the input text fields if the user turns on or off the thresholds.
     */
    private void toggleThresholdInputs(boolean enabled)
    {
        minTemp.setEnabled(enabled);
        maxTemp.setEnabled(enabled);
        minHum.setEnabled(enabled);
        maxHum.setEnabled(enabled);
        minPres.setEnabled(enabled);
        maxPres.setEnabled(enabled);
    }
}