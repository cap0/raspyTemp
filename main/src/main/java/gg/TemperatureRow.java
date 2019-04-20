package gg;

import java.time.LocalDateTime;

import static gg.Util.toJsDate;

public class TemperatureRow {
    LocalDateTime date;
    Double chamberTemp;
    Double wortTemp;
    Double settingTemperature;
    int status;

    TemperatureRow(LocalDateTime date,
                   Double chamberTemp,
                   Double wortTemp,
                   Double settingTemperature, int status) {
        this.date = date;
        this.chamberTemp = chamberTemp;
        this.wortTemp = wortTemp;
        this.settingTemperature = settingTemperature;
        this.status = status;
    }

    TemperatureRow(TemperatureRow t){
        this.date = t.date;
        this.chamberTemp = t.chamberTemp;
        this.wortTemp = t.wortTemp;
        this.settingTemperature = t.settingTemperature;
        this.status = t.status;
    }

    @Override
    public String toString() { // TODO rimuovere?
        return "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";
    }

    boolean isValid(){
        return date != null && !chamberTemp.isNaN() && !wortTemp.isNaN();
    }

    String toCsv() {
        return date.toString() + "|" + chamberTemp + "|" + wortTemp + "|" + settingTemperature + "|" + status;
    }




}
