package gg;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public class StatisticalInfo {

    int invalidValuesChamber = 0;
    int invalidValuesWort = 0;
    int skippedDates = 0;

    DoubleSummaryStatistics chamberStats;
    DoubleSummaryStatistics wortStats;

    List<ParseTempFile.TemperatureRow> temperatures = new ArrayList<>();

    @Override
    public String toString(){
       return
                "\n Replaced invalid values Chamber: " + invalidValuesChamber +
                "\n Replaced invalid values Wort: " + invalidValuesWort +
                "\n Skipped rows: " + skippedDates +
                "\n Statistics chamber: " + chamberStats +
                "\n Statistics wort: " + wortStats;
    }
}
