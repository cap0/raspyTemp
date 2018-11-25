package gg;

import java.time.LocalDateTime;

import static gg.Util.toJsDate;

public class TemperatureRow {
    LocalDateTime date;
    Double chamberTemp;
    Double wortTemp;
    private Double settingTemperature;

    TemperatureRow(LocalDateTime date,
                   Double chamberTemp,
                   Double wortTemp,
                   Double settingTemperature) {
        this.date = date;
        this.chamberTemp = chamberTemp;
        this.wortTemp = wortTemp;
        this.settingTemperature = settingTemperature;
    }

    TemperatureRow(TemperatureRow t){
        this.date = t.date;
        this.chamberTemp = t.chamberTemp;
        this.wortTemp = t.wortTemp;
        this.settingTemperature = t.settingTemperature;
    }

    @Override
    public String toString() {
        return "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";
    }

    boolean isValid(){
        return date != null && !chamberTemp.isNaN() && !wortTemp.isNaN();
    }

    String toCsv(){
        return date.toString() + "|" + chamberTemp + "|" + wortTemp;
    }




}
