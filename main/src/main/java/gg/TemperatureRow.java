package gg;

import java.time.LocalDateTime;

import static gg.Util.toJsDate;

public class TemperatureRow {
    LocalDateTime date;
    Double chamberTemp;
    Sensor chamberSensor;
    Double wortTemp;
    Sensor wortSensor;
    private Double settingTemperature;

    TemperatureRow(LocalDateTime date,
                   Double chamberTemp, Sensor chamberSensor,
                   Double wortTemp, Sensor wortSensor,
                   Double settingTemperature) {
        this.date = date;
        this.chamberTemp = chamberTemp;
        this.chamberSensor = chamberSensor;
        this.wortTemp = wortTemp;
        this.wortSensor = wortSensor;
        this.settingTemperature = settingTemperature;
    }

    TemperatureRow(TemperatureRow t){
        this.date = t.date;
        this.chamberTemp = t.chamberTemp;
        this.chamberSensor = t.chamberSensor;
        this.wortTemp = t.wortTemp;
        this.wortSensor = t.wortSensor;
        this.settingTemperature = t.settingTemperature;
    }

    @Override
    public String toString() {
        return "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";
    }

    public boolean isValid(){
        return date != null && !chamberTemp.isNaN() && !wortTemp.isNaN();
    }




}
