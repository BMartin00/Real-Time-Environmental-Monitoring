# Sense HAT Real-Time Monitoring with Android App  

## 📌 Project Overview  
This project demonstrates real-time environmental monitoring using a **Raspberry Pi with a Sense HAT** and an **Android mobile application**.  
The Raspberry Pi collects **temperature, humidity, and pressure** data from the Sense HAT sensors and sends it to the Android app via **WebSocket communication**.  

The Android app displays real-time sensor values, visualizes them in charts, and provides **threshold-based alerts** through notifications when sensor values go out of range.  

---

## ⚙️ Features  
- 🌡️ **Real-time data monitoring** (temperature, humidity, pressure)  
- 📊 **Interactive charts** for visualizing sensor values  
- 🔔 **Threshold-based notifications** for alerts (above/below limits)  
- ⚡ **WebSocket communication** between Raspberry Pi and Android app  
- ⚙️ **Customizable thresholds and server settings** using SharedPreferences  
- 📱 User-friendly mobile interface  

---

## 🖥️ System Architecture  
1. **Raspberry Pi + Sense HAT**  
   - Runs a Python WebSocket server.  
   - Collects sensor data (temperature, humidity, pressure).  
   - Sends sensor values to connected Android clients every second.  
   - Responds to threshold messages from the Android app and displays values on the LED matrix.  

2. **Android App**  
   - Connects to the Raspberry Pi WebSocket server using a stored IP address and port.  
   - Displays real-time sensor data in text and charts.  
   - Provides settings to update thresholds, IP, and port.  
   - Sends alerts to the Pi (to display on LED) when thresholds are exceeded.  
   - Push notifications for out-of-range readings.  

---

## 🛠️ Technologies Used  
- **Hardware**: Raspberry Pi, Sense HAT  
- **Programming**:  
  - Python (server-side, WebSocket, Sense HAT API)  
  - Java (Android app development in Android Studio)  
- **Communication Protocol**: WebSocket  
- **Android Components**:  
  - SharedPreferences (settings storage)  
  - Notifications API  
  - MPAndroidChart (graph visualization)  

---

## 📂 Project Structure
- /Main Branch
  - project-server.py

- /ProjectClient
  - **MainActivity.java** # Displays real-time values and thresholds
  - **ChartViewScreen.java** # Graph view for sensor data
  - **SettingsScreen.java** # Update thresholds, IP, and port
  - **WebSocketClientHandler.java** # Handles WebSocket connection
  - **NotificationHelper.java** # Creates notifications
  - **res/layout/** # XML UI layouts

---

## 🚀 Setup Instructions
### 🔹 Raspberry Pi
- Install required Python packages: pip install websockets sense-hat asyncio
- Run the WebSocket server: python3 project-server.py
- Note the Pi’s IP address and port (default: 8765)

### 🔹 Android App 
- Open the Android project in Android Studio.
- Update the IP address and port in the app settings.
- Run the app on an emulator or physical device (same network as the Pi).

---

## 📊 Example Usage
- Launch the Python server on Raspberry Pi.
- Open the Android app → set IP & port → go back to main screen.
- Monitor real-time values.
- Configure thresholds in the settings menu.
- Receive alerts if readings are outside thresholds.

---

## 🔮 Future Enhancements
- 📦 Database integration for storing historical sensor data.
- 📑 Report generation (CSV/PDF export of logs).
- 📡 Cloud integration (send sensor data to cloud services like AWS/Google Cloud).
- 🔋 Energy optimization for mobile app and Pi.

---

## 👨‍💻 Author

Developed as part of a Connected Devices project integrating IoT hardware with mobile applications.  
Real-Time-Environmental-Monitoring  
Software Development for Connected Devices Year 2 Semester 2 Project
