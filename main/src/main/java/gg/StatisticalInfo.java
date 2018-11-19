package gg;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public class StatisticalInfo {

    LocalDateTime startDate = null;
    LocalDateTime endDate = null;

    int invalidValuesChamber = 0;
    int invalidValuesWort = 0;
    int skippedDates = 0;

    DoubleSummaryStatistics chamberStats;
    DoubleSummaryStatistics wortStats;

    List<TemperatureRow> temperatures = new ArrayList<>();

    @Override
    public String toString(){
       return
                "\n Replaced invalid values Chamber: " + invalidValuesChamber +
                "\n Replaced invalid values Wort: " + invalidValuesWort +
                "\n Skipped rows: " + skippedDates +
                "\n Statistics chamber: " + chamberStats +
                "\n Statistics wort: " + wortStats;
    }

    void storeDates(LocalDateTime date) {
        if (startDate==null) {
            startDate= date;
        }else{
            endDate=date;
        }
    }
}
