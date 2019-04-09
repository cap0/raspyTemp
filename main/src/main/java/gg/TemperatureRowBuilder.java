package gg;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class TemperatureRowBuilder {
    private static final Logger logger = LogManager.getLogger(TemperatureRowBuilder.class);


    private LocalDateTime date;
    private double chamberTemp;
    private double wortTemp;
    private Double settingTemperature;


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

    TemperatureRowBuilder chamber(String chamberTemp){
        try {
            this.chamberTemp = Double.parseDouble(chamberTemp);
        } catch (NumberFormatException e) {
            this.chamberTemp = Double.NaN;
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

    TemperatureRow build(){
        return new TemperatureRow(date, chamberTemp, wortTemp, settingTemperature);
    }

}
