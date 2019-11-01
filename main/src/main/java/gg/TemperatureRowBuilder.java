package gg;

import gg.TemperatureSetting.TemperatureSettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class TemperatureRowBuilder {
    private static final Logger logger = LogManager.getLogger(TemperatureRowBuilder.class);

    private LocalDateTime date;
    private double roomTemp;
    private double wortTemp;
    private Double settingTemperature;
    private int status;

    TemperatureRowBuilder date(String date, String datePattern){
        DateTimeFormatter formatter = StringUtils.isBlank(datePattern) ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : DateTimeFormatter.ofPattern(datePattern);
        try {
            this.date = LocalDateTime.parse(date, formatter);
        } catch (Exception e) {
            logger.error(e);
            this.date = null;
        }
        return this;
    }

    TemperatureRowBuilder room(String roomTemp){
        try {
            this.roomTemp = Double.parseDouble(roomTemp);
        } catch (NumberFormatException e) {
            this.roomTemp = Double.NaN;
        }
        return this;
    }

    TemperatureRowBuilder wort(String wortTemp){
        try {
            this.wortTemp = Double.parseDouble(wortTemp);
        } catch (NumberFormatException e) {
            this.wortTemp = Double.NaN;
        }
        return this;
    }

    TemperatureRowBuilder settings(TemperatureSettings temperatureSettings) {
        this.settingTemperature = temperatureSettings.getTemperatureSettingsValueForDate(this.date);
        return this;
    }

    TemperatureRowBuilder status(int status) {
        this.status = status;
        return this;
    }

    TemperatureRow build(){
        return new TemperatureRow(date, roomTemp, wortTemp, settingTemperature, status);
    }

}
