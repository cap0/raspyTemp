package gg.LCD;


import com.diozero.devices.HD44780Lcd;

import static com.diozero.api.I2CConstants.BUS_1;
import static com.diozero.devices.HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;

public class LCD implements ILCD{
    private HD44780Lcd lcd;

    public LCD() {
        HD44780Lcd.LcdConnection lcdConnection = new HD44780Lcd.PCF8574LcdConnection(BUS_1, DEFAULT_DEVICE_ADDRESS);
        lcd = new HD44780Lcd(lcdConnection, 16, 2);
        lcd.blinkOff();
        lcd.cursorOff();
        shutdownHook();
    }
    @Override
    public void print(String row0, String row1 ){
        lcd.clear();
        lcd.setText(0, row0);
        lcd.setText(1, row1);
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> lcd.clear()));
    }
}
