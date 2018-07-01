package gg;

import java.time.LocalDateTime;

import static gg.Util.toJsDate;

public class TemperatureRow {
    LocalDateTime date;
    Double chamberTemp;
    String chamberSensorName;
    Double wortTemp;
    String wortSensorName;
    private Double settingTemperature;

    TemperatureRow(LocalDateTime date,
                   Double chamberTemp, String chamberSensorName,
                   Double wortTemp, String wortSensorName,
                   Double settingTemperature) {
        this.date = date;
        this.chamberTemp = chamberTemp;
        this.chamberSensorName = chamberSensorName;
        this.wortTemp = wortTemp;
        this.wortSensorName = wortSensorName;
        this.settingTemperature = settingTemperature;
    }

    TemperatureRow(TemperatureRow t){
        this.date = t.date;
        this.chamberTemp = t.chamberTemp;
        this.chamberSensorName = t.chamberSensorName;
        this.wortTemp = t.wortTemp;
        this.wortSensorName = t.wortSensorName;
        this.settingTemperature = t.settingTemperature;
    }

    @Override
    public String toString() { //TODO make another method
        return "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";
    }

}
