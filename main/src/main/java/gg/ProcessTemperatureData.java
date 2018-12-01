package gg;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static gg.Constants.*;
import static gg.Util.getProperty;
import static gg.Util.getPropertyOrDefault;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.singletonList;

public class ProcessTemperatureData implements Runnable{

    private static final Logger logger = LogManager.getLogger(ProcessTemperatureData.class);

    private LinkedHashMap<LocalDateTime, Double> settingsTemperature;
    private DateRange dataRange;
    private Double minAllowedTemp;
    private Double maxAllowedTemp;
    private int aggregationFactor;
    private String sourceFilePath;
    private String temperatureProcessedOutputFile;
    private boolean generateXlsxFile;
    private String xlsxFilePath;
    private String datePattern;

    private ReentrantLock lock;

    public ProcessTemperatureData(Properties p, ReentrantLock lock){
        getProperties(p);
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            lock.lock();
            processRawData();
        } catch (Exception t) {
            logger.fatal("unexpected error", t);
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws IOException {
        new ProcessTemperatureData(Util.getProperties(args[0]), new ReentrantLock()).processRawData();
    }

    private void getProperties(Properties p){
        settingsTemperature = getTemperatureSettings(p);
        dataRange = buildDataRange(getProperty(p, START_DATE), getProperty(p, END_DATE));
        minAllowedTemp = Double.valueOf(getPropertyOrDefault(p, MIN_ALLOWED_TEMP, "0"));
        maxAllowedTemp = Double.valueOf(getPropertyOrDefault(p, MAX_ALLOWED_TEMP, "50"));
        aggregationFactor = Integer.valueOf(getPropertyOrDefault(p, SERIES_AGGREGATION_FACTOR, "1"));
        sourceFilePath = getProperty(p, TEMPERATURE_OUTPUT_FILE);
        temperatureProcessedOutputFile = getProperty(p, TEMPERATURE_PROCESSED_OUTPUT_FILE);
        datePattern = getProperty(p, DATE_PATTERN);

        generateXlsxFile = Boolean.parseBoolean(getPropertyOrDefault(p, GENERATE_XLSX_FILE, "false"));
        xlsxFilePath = getProperty(p, XLSX_OUTPUT_FILE);
    }

    private void processRawData() throws IOException {
        logger.info("Start processing data");
        StatisticalInfo statisticalInfo = processSourceFile(dataRange, sourceFilePath, settingsTemperature, minAllowedTemp, maxAllowedTemp, aggregationFactor);
        writeProcessedData(temperatureProcessedOutputFile, statisticalInfo.temperatures);

        if(generateXlsxFile) {
            WriteXlsx.writeXlsxFile(statisticalInfo.temperatures, xlsxFilePath);
        }
    }

    private void writeProcessedData(String temperatureProcessedOutputFile, List<TemperatureRow> temperatures) throws IOException {
        writeNowDateInFile(temperatureProcessedOutputFile);

        List<String> lines = temperatures.stream()
                .map(TemperatureRow::toCsv)
                .collect(Collectors.toList());

        Files.write(Paths.get(temperatureProcessedOutputFile), lines, APPEND);
    }

    private void writeNowDateInFile(String temperatureProcessedOutputFile) throws IOException {
        Files.write(Paths.get(temperatureProcessedOutputFile), singletonList(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME)));
    }

    private static String[] getTemperatureSettingFromProperty(Properties p) {
        String temperatureSettings = getProperty(p, TEMPERATURE_SETTINGS);
        if(temperatureSettings!= null && !temperatureSettings.isEmpty()){
            return temperatureSettings.trim().split(";");
        }
        return new String[0];
    }

    private static LinkedHashMap<LocalDateTime, Double> getTemperatureSettings(Properties p) {
        String[] temperatureSettings = getTemperatureSettingFromProperty(p);

        LinkedHashMap<LocalDateTime, Double> settingsTemperature = new LinkedHashMap<>();
        for (int i = 0; i < temperatureSettings.length; i=i+2) {
            LocalDateTime dateTime = LocalDateTime.parse(temperatureSettings[i], ISO_LOCAL_DATE_TIME);
            double temperature = Double.parseDouble(temperatureSettings[i + 1]);
            settingsTemperature.put(dateTime, temperature);
        }
        return settingsTemperature;
    }

    private StatisticalInfo processSourceFile(DateRange dataRange, String filePath, LinkedHashMap<LocalDateTime, Double> settingsTemperature,
                                                     Double minAllowedTemp, Double maxAllowedTemp, int aggregationFactor) {
        List<TemperatureRow> rows = extractTemperatureInfoFromSourceFile(filePath, settingsTemperature);

        logger.info("Processing " + rows.size() + " rows");

        CircularFifoQueue<Double> lastAvgChamber = new CircularFifoQueue<>(aggregationFactor);
        CircularFifoQueue<Double> lastAvgWort = new CircularFifoQueue<>(aggregationFactor);

        StatisticalInfo stats = new StatisticalInfo();
        int i =0 ;
        for (TemperatureRow row : rows) {
            if (isDateOutsideRange(row.date, dataRange)) {
                stats.skippedDates++;
                continue;
            }
            stats.storeDates(row.date);

            if (isTemperatureNotValid(minAllowedTemp, maxAllowedTemp, row.chamberTemp)) {
                row.chamberTemp = avg(lastAvgChamber);
                stats.invalidValuesChamber++;
            } else {
                lastAvgChamber.add(row.chamberTemp);
            }

            if (isTemperatureNotValid(minAllowedTemp, maxAllowedTemp, row.wortTemp)) {
                row.wortTemp = avg(lastAvgWort);
                stats.invalidValuesWort++;
            } else {
                lastAvgWort.add(row.wortTemp);
            }

            if (aggregationFactor > 1) {
                if (i % aggregationFactor == 0) {
                    TemperatureRow t = new TemperatureRow(row);
                    t.chamberTemp = avg(lastAvgChamber);
                    t.wortTemp = avg(lastAvgWort);
                    stats.temperatures.add(t);
                }
            } else {
                stats.temperatures.add(row);
            }

            i++;
        }

        stats.chamberStats = stats.temperatures.stream()
                .collect(Collectors.summarizingDouble(r-> r.chamberTemp));

        stats.wortStats = stats.temperatures.stream()
                .collect(Collectors.summarizingDouble(r-> r.wortTemp));

        return stats;
    }

    private static boolean isDateOutsideRange(LocalDateTime date, DateRange dataRange) {
        return dataRange.sd != null && date.isBefore(dataRange.sd) || dataRange.ed != null && date.isAfter(dataRange.ed);
    }

    private static boolean isTemperatureNotValid(Double minAllowedTemp, Double maxAllowedTemp, Double chamberTemp) {
        return chamberTemp < minAllowedTemp || chamberTemp > maxAllowedTemp;
    }

    private List<TemperatureRow> extractTemperatureInfoFromSourceFile(String filePath, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        List<TemperatureRow> rows = null;
        try {
            List<String> temperatureLines = Files.readAllLines(Paths.get(filePath));
            temperatureLines.remove(0); //Ignoring header

            rows = temperatureLines.stream()
                    .map(l -> l.split("\\|"))
                    .map(r -> new TemperatureRowBuilder()
                            .date(r[0], datePattern)
                            .chamber(r.length >1 ?r[1] : "0")
                            .wort(r.length >2 ?r[2] : "0")
                            .settings(temperatureSettings)
                            .build())
                    .filter(TemperatureRow::isValid)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Cannot read file: " + filePath, e);
            System.exit(-1);
        }
        return rows;
    }

    private static Double avg(CircularFifoQueue<Double> lastValues) {
        if(lastValues.isEmpty()){
            return 0D;
        }

        double sum  = 0;
        int i=0;

        for (Double d : lastValues) {
            sum+=d;
            i++;
        }
        return sum/i;
    }

    private static class DateRange {
        final LocalDateTime sd;
        final LocalDateTime ed;

        DateRange(LocalDateTime sd, LocalDateTime ed) {
            this.sd = sd;
            this.ed = ed;
        }
    }

    private static ProcessTemperatureData.DateRange buildDataRange(String startDate, String endDate) {
        LocalDateTime sd = null;
        LocalDateTime ed = null;
        if (StringUtils.isNotBlank(startDate)) {
            sd = LocalDateTime.parse(startDate, ISO_LOCAL_DATE_TIME);
        }
        if (StringUtils.isNotBlank(endDate)) {
            ed = LocalDateTime.parse(endDate, ISO_LOCAL_DATE_TIME);
        }
        return new ProcessTemperatureData.DateRange(sd, ed);
    }
}
