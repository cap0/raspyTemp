import RPi.GPIO as GPIO
from time import sleep
relay_pins = [24]
led_pins = [19]
GPIO.setmode(GPIO.BCM)
GPIO.setup(relay_pins, GPIO.OUT)
GPIO.setup(led_pins, GPIO.OUT)
GPIO.output(relay_pins, 1)
GPIO.output(led_pins, 1)

GPIO.output(relay_pins, 0)# acceso
sleep(1)
GPIO.output(relay_pins, 1)#spento
sleep(3)

GPIO.cleanup()
