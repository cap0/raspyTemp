#!/usr/bin/python
import sys
import time
import requests
import Adafruit_DHT
from datetime import datetime

# Parse command line parameters.
sensor_args = { '11': Adafruit_DHT.DHT11,
                '22': Adafruit_DHT.DHT22,
                '2302': Adafruit_DHT.AM2302 }
if len(sys.argv) == 3 and sys.argv[1] in sensor_args:
    sensor = sensor_args[sys.argv[1]]
    pin = sys.argv[2]
else:
    print('Usage: sudo ./Adafruit_DHT.py [11|22|2302] <GPIO pin number>')
    print('Example: sudo ./Adafruit_DHT.py 2302 4 - Read from an AM2302 connected to GPIO pin #4')
    sys.exit(1)

# Try to grab a sensor reading.  Use the read_retry method which will retry up
# to 15 times to get a sensor reading (waiting 2 seconds between each retry).

f = open("/home/pi/Adafruit_Python_DHT/examples/humidityTemperature.txt", "a")
while (True):
  humidity, temperature = Adafruit_DHT.read_retry(sensor, pin)
  now = datetime.now().isoformat()
  if humidity is not None and temperature is not None:
    print('Time:'+str(now)+' Temp={0:0.1f}*  Humidity={1:0.1f}%'.format(temperature, humidity))
    f.write("time:" + str(now)+" h:" + str(humidity) + " t:" + str(temperature) + "\n"  )
    f.flush()
    requests.get("https://api.thingspeak.com/update?api_key=8J0P4RTTODPU4HTW&field1="+str(humidity)+ "&field2=" +str(temperature))
  else:
    print('Failed to get reading. Try again!')
  time.sleep(600)
