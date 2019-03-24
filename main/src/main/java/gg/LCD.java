package gg;


import com.diozero.devices.HD44780Lcd;
import com.diozero.util.SleepUtil;

import static com.diozero.api.I2CConstants.BUS_1;
import static com.diozero.devices.HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;

public class LCD {

    private HD44780Lcd lcd;

    public LCD() {
        HD44780Lcd.LcdConnection lcdConnection = new HD44780Lcd.PCF8574LcdConnection(BUS_1, DEFAULT_DEVICE_ADDRESS);
        lcd = new HD44780Lcd(lcdConnection, 16, 2);
    }

    public void print0(String row0 ){
        lcd.setText(0, row0);
    }

    public void print1(String row1 ){
        lcd.setText(1, row1);
    }

    public void print(String row0, String row1 ){
        lcd.setText(0, row0);
        lcd.setText(1, row1);
    }

    public void writeByChar(String s) {
        lcd.clear();
        for (byte b : s.getBytes()) {
            lcd.addText(b);
            SleepUtil.sleepSeconds(.2);
        }
    }

}
