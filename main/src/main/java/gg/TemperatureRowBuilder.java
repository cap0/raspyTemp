package gg;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

class TemperatureRowBuilder {

    private LocalDateTime date;
    private double chamberTemp;
    private String chamberSensorName;
    private double wortTemp;
    private String wortSensorName;
    private Double settingTemperature;


    TemperatureRowBuilder date(String date, String datePattern){
        DateTimeFormatter formatter = StringUtils.isBlank(datePattern) ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : DateTimeFormatter.ofPattern(datePattern);
        this.date = LocalDateTime.parse(date, formatter);
        return this;
    }

    TemperatureRowBuilder chamber(String chamberSensorName, String chamberTemp){
        this.chamberTemp = parseDouble(chamberTemp);
        this.chamberSensorName = chamberSensorName;
        return this;
    }

    TemperatureRowBuilder wort(String wortSensorName, String wortTemp){
        this.wortTemp = parseDouble(wortTemp);
        this.wortSensorName = wortSensorName;
        return this;
    }

    TemperatureRowBuilder settings(LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        this.settingTemperature = getSettingTemperature(this.date, temperatureSettings);
        return this;
    }

    TemperatureRow build(){
        return new TemperatureRow(date, chamberTemp, chamberSensorName, wortTemp, wortSensorName, settingTemperature);
    }

    private Double getSettingTemperature(LocalDateTime date, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        for (Map.Entry<LocalDateTime, Double> set : temperatureSettings.entrySet()) {
            if (date.isBefore(set.getKey())){
                return set.getValue();
            }
        }

        return -1D;
    }

    private static NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY); // TODO handle
    private double parseDouble(String chamberTemp) {
        try {
            return nf.parse(chamberTemp).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0; // TODO handle
    }
}
