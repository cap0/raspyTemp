package gg;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

class TemperatureRowBuilder {

    private LocalDateTime date;
    private double chamberTemp;
    private Sensor chamberSensor;
    private double wortTemp;
    private Sensor wortSensor;
    private Double settingTemperature;


    TemperatureRowBuilder date(String date, String datePattern){
        DateTimeFormatter formatter = StringUtils.isBlank(datePattern) ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : DateTimeFormatter.ofPattern(datePattern);
        try {
            this.date = LocalDateTime.parse(date, formatter);
        } catch (Exception e) {
            this.date = null;
        }
        return this;
    }

    TemperatureRowBuilder chamber(Sensor chamberSensor, String chamberTemp){
        try {
            this.chamberTemp = Double.parseDouble(chamberTemp);
        } catch (NumberFormatException e) {
            this.chamberTemp = Double.NaN;
        }
        this.chamberSensor = chamberSensor;
        return this;
    }

    TemperatureRowBuilder wort(Sensor wortSensor, String wortTemp){
        try {
            this.wortTemp = Double.parseDouble(wortTemp);
        } catch (NumberFormatException e) {
            this.wortTemp = Double.NaN;
        }
        this.wortSensor = wortSensor;
        return this;
    }

    TemperatureRowBuilder settings(LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        this.settingTemperature = getSettingTemperature(this.date, temperatureSettings);
        return this;
    }

    TemperatureRow build(){
        return new TemperatureRow(date, chamberTemp, chamberSensor, wortTemp, wortSensor, settingTemperature);
    }

    private Double getSettingTemperature(LocalDateTime date, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        for (Map.Entry<LocalDateTime, Double> set : temperatureSettings.entrySet()) {
            if (date.isBefore(set.getKey())){
                return set.getValue();
            }
        }

        return -1D;
    }
}
