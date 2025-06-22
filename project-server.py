import asyncio
import websockets
from sense_hat import SenseHat

sense = SenseHat()

red = [255, 0, 0]
green = [0, 255, 0]
blue = [0, 0, 80]
grey = [128, 128, 128]

# Function to get sensor readings
def get_sensor_data():
    temperature = round(sense.get_temperature(), 2)
    humidity = round(sense.get_humidity(), 2)
    pressure = round(sense.get_pressure(), 2)
    
    # Create a JSON-formatted string to send multiple values
    data = f'{{"temperature": {temperature}, "humidity": {humidity}, "pressure": {pressure}}}'
    return data

# Function to send sensor data every second
async def send_data(websocket):
    print("\nClient Connected!\n")	# Debugging output
    
    while True:
        sensor_data = get_sensor_data()
        await websocket.send(sensor_data)
#         print(f"Sent data: {sensor_data}")	# Debugging output
        await asyncio.sleep(1)  # Send data every second

# Function to handle specific client commands
async def get_data(websocket):
    while True:
        message = await websocket.recv()  # Wait for client commands
#         print(f"Received message: {message}")	# Debugging output

        try:
            if message.startswith("TEMP_IN_THRESHOLD:"):
                temp_value = message.split(":")[1]
                sense.show_message(f"{temp_value} C", text_colour=green, back_colour=blue)
#                 print(f"Temperature: {temp_value} C")	# Debugging output
            elif message.startswith("TEMP_OUT_THRESHOLD:"):
                temp_value = message.split(":")[1]
                sense.show_message(f"{temp_value} C", text_colour=red, back_colour=blue)
#                 print(f"Temperature: {temp_value} C")	# Debugging output
            elif message.startswith("TEMP_THRESHOLDS_DISABLED:"):
                temp_value = message.split(":")[1]
                sense.show_message(f"{temp_value} C", text_colour=grey, back_colour=blue)
#                 print(f"Temperature: {temp_value} C")	# Debugging output
                
            if message.startswith("HUM_IN_THRESHOLD:"):
                hum_value = message.split(":")[1]
                sense.show_message(f"{hum_value} %", text_colour=green, back_colour=blue)
#                 print(f"Humidity: {hum_value} %")	# Debugging output
            elif message.startswith("HUM_OUT_THRESHOLD:"):
                hum_value = message.split(":")[1]
                sense.show_message(f"{hum_value} %", text_colour=red, back_colour=blue)
#                 print(f"Humidity: {hum_value} %")	# Debugging output
            elif message.startswith("HUM_THRESHOLDS_DISABLED:"):
                hum_value = message.split(":")[1]
                sense.show_message(f"{hum_value} %", text_colour=grey, back_colour=blue)
#                 print(f"Humidity: {hum_value} %")	# Debugging output
                
                
            if message.startswith("PRES_IN_THRESHOLD:"):
                pres_value = message.split(":")[1]
                sense.show_message(f"{pres_value} hPa", text_colour=green, back_colour=blue)
#                 print(f"Pressure: {pres_value} hPa")	# Debugging output
            elif message.startswith("PRES_OUT_THRESHOLD:"):
                pres_value = message.split(":")[1]
                sense.show_message(f"{pres_value} hPa", text_colour=red, back_colour=blue)
#                 print(f"Pressure: {pres_value} hPa")	# Debugging output
            elif message.startswith("PRES_THRESHOLDS_DISABLED:"):
                pres_value = message.split(":")[1]
                sense.show_message(f"{pres_value} hPa", text_colour=grey, back_colour=blue)
#                 print(f"Pressure: {pres_value} hPa")	# Debugging output
                
            sense.clear()
        except Exception as e:
            print(f"Unknown message format: {e}")

# WebSocket server that runs both tasks concurrently
async def handle_client(websocket):
    try:
        # Run both tasks concurrently
        await asyncio.gather(
            send_data(websocket),  # Send sensor data periodically
            get_data(websocket)  # Listen for commands from the client
        )
    except websockets.exceptions.ConnectionClosed as e:
        print("\nClient Disconnected")
    finally:
        print("Connection Closed!")

# Start the WebSocket server
async def start_server():
    ip = input("Enter the IP Address: ")
    port = input("Enter the Port Number: ")
    
    # Start the WebSocket server and handle incoming connections
    server = await websockets.serve(handle_client, ip, port)
    print(f"Server started on {ip}:{port}")
    await server.wait_closed()

# Run the server
if __name__ == "__main__":
    asyncio.run(start_server())


