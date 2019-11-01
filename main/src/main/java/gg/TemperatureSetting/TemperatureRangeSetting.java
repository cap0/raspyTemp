package gg.TemperatureSetting;

import org.apache.commons.lang3.Range;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public class TemperatureRangeSetting implements Comparable {
    private Range<ChronoLocalDateTime<?>> range;
    private Double tempValue;

    public TemperatureRangeSetting(Range<ChronoLocalDateTime<?>> range, Double tempValue) {
        this.range = range;
        this.tempValue = tempValue;
    }

    public Double getValue(){
        return tempValue;
    }

    public boolean contains(LocalDateTime d){
        return range.contains(d);
    }

    public boolean isBefore(LocalDateTime d){
        return range.isBefore(d);
    }

    public ChronoLocalDateTime<?> getMinimum(){
        return range.getMinimum();
    }

    public ChronoLocalDateTime<?> getMaximum(){
        return range.getMaximum();
    }

    public String formatForFile(){
        return range.getMinimum().format(ISO_LOCAL_DATE_TIME)+";"+range.getMaximum().format(ISO_LOCAL_DATE_TIME)+";"+tempValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemperatureRangeSetting that = (TemperatureRangeSetting) o;
        return range.equals(that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(range);
    }

    @Override
    public int compareTo(Object o) {
       return this.range.getMinimum().compareTo(((TemperatureRangeSetting)o).range.getMinimum());
    }

    @Override
    public String toString() {
        return formatForFile();
    }
}
