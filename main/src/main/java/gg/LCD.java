package gg;


import com.diozero.devices.HD44780Lcd;
import com.diozero.util.SleepUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.diozero.api.I2CConstants.BUS_1;
import static com.diozero.devices.HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;

public class LCD {
    private static final Logger logger = LogManager.getLogger(LCD.class);

    private HD44780Lcd lcd;

    LCD() {
        HD44780Lcd.LcdConnection lcdConnection = new HD44780Lcd.PCF8574LcdConnection(BUS_1, DEFAULT_DEVICE_ADDRESS);
        lcd = new HD44780Lcd(lcdConnection, 16, 2);
        lcd.blinkOff();
        lcd.cursorOff();
        shutdownHook();
    }

    void print0(String row0){
        lcd.setText(0, pad(row0));
    }

    void print1(String row1){
        lcd.setText(1, pad(row1));
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

    private String pad(String row0) {
        return StringUtils.rightPad(row0,16, " ");
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(200);
                lcd.clear();

            } catch (InterruptedException e) {
                logger.error(e);
            }
        }));
    }
}
