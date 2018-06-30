package gg;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static gg.Util.toJsDate;

public class TemperatureRow {
    LocalDateTime date;
    Double chamberTemp = 0D;
    String chamberSensorName;
    Double wortTemp =0D;
    String wortSensorName;
    private Double settingTemperature =0D;

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); //TODO use localdatetime
    private static NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY);

    //TODO avoid all this logic in the constructor
    TemperatureRow(String date, String chamberTemp, String chamberSensorName, String wortTemp, String wortSensorName, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        try {
            Date parse = sdf.parse(date); //TODO remove this when writing file in the ISO DATETIME way
            this.date = parse.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            this.chamberTemp = nf.parse(chamberTemp).doubleValue();
            this.chamberSensorName = chamberSensorName;

            this.wortTemp = nf.parse(wortTemp).doubleValue();
            this.wortSensorName = wortSensorName;

            this.settingTemperature = getSettingTemperature(this.date, temperatureSettings);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    TemperatureRow(TemperatureRow t){
        this.date = t.date;
        this.chamberTemp = t.chamberTemp;
        this.chamberSensorName = t.chamberSensorName;
        this.wortTemp = t.wortTemp;
        this.wortSensorName = t.wortSensorName;
        this.settingTemperature = t.settingTemperature;
    }

    private Double getSettingTemperature(LocalDateTime date, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        for (Map.Entry<LocalDateTime, Double> set : temperatureSettings.entrySet()) {
            if (date.isBefore(set.getKey())){
                return set.getValue();
            }
        }

        return -1D;
    }

    @Override
    public String toString() { //TODO make another method
        return "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";
    }
}
