package gg;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.charts.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public class ParseTempFile {

    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String SOURCE_FILE = "sourceFile";
    private static final String OUTPUT_FILE = "outputFile";
    private static final String TEMPERATURE_SETTINGS = "temperatureSettings";
    private static final String GENERATE_XLSX_FILE = "generateXlsxFile";
    private static final String MIN_ALLOWED_TEMP = "minAllowedTemp";
    private static final String MAX_ALLOWED_TEMP = "maxAllowedTemp";
    private static final String OUTPUT_XLSX_FILE = "outputXlsxFile";
    private static final String SERIES_AGGREGATION_FACTOR = "seriesAggregationFactor";

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); //TODO use localdatetime
    private static NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY);

    public static void main(String[] args) throws Exception {
        if(args.length!=1){
            System.err.println("Property file is missing");
            System.exit(-1);
        }

        System.out.println("Start. Parameters: " + Arrays.asList(args));
        Properties p = Main.getProperties(args[0]);

        LinkedHashMap<LocalDateTime, Double> settingsTemperature = getTemperatureSettings(p);

        DateRange dataRange = buildDataRange(getProperty(p, START_DATE), getProperty(p, END_DATE));
        Double minAllowedTemp = Double.valueOf(getPropertyOrDefault(p, MIN_ALLOWED_TEMP, "0"));
        Double maxAllowedTemp = Double.valueOf(getPropertyOrDefault(p, MAX_ALLOWED_TEMP, "50"));
        int aggregationFactor = Integer.valueOf(getPropertyOrDefault(p, SERIES_AGGREGATION_FACTOR, "1"));

        StatisticalInfo statisticalInfo = processSourceFile(dataRange, getProperty(p, SOURCE_FILE), settingsTemperature, minAllowedTemp, maxAllowedTemp, aggregationFactor);
        new GenerateChart().generateChart(statisticalInfo, getProperty(p, OUTPUT_FILE), p);
        if(Boolean.parseBoolean(getPropertyOrDefault(p, GENERATE_XLSX_FILE, "false"))) {
            writeXlsxFile(statisticalInfo.temperatures, getProperty(p, OUTPUT_XLSX_FILE));
        }
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

    private static StatisticalInfo processSourceFile(DateRange dataRange, String filePath, LinkedHashMap<LocalDateTime, Double> settingsTemperature,
                                                     Double minAllowedTemp, Double maxAllowedTemp, int aggregationFactor) {
        System.out.println("startDate " + dataRange.sd + " endDate " + dataRange.ed);
        List<TemperatureRow> rows = extractTemperatureInfoFromSourceFile(filePath, settingsTemperature);

        System.out.println("Processing " + rows.size() + " rows");

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

            if (aggregationFactor >1) {
                if( i % aggregationFactor == 0) {
                    TemperatureRow t = new TemperatureRow(row);
                    t.chamberTemp = avg(lastAvgChamber);
                    t.wortTemp = avg(lastAvgWort);
                    stats.temperatures.add(t);
                }
            } else{
                stats.temperatures.add(row);
            }

            i++;
        }

        stats.chamberStats = stats.temperatures.stream()
                .collect(Collectors.summarizingDouble(r-> r.chamberTemp));

        stats.wortStats = stats.temperatures.stream()
                .collect(Collectors.summarizingDouble(r-> r.wortTemp));

        System.out.println(stats);

        return stats;
    }

    private static boolean isDateOutsideRange(LocalDateTime date, DateRange dataRange) {
        return dataRange.sd != null && date.isBefore(dataRange.sd) || dataRange.ed != null && date.isAfter(dataRange.ed);
    }

    private static boolean isTemperatureNotValid(Double minAllowedTemp, Double maxAllowedTemp, Double chamberTemp) {
        return chamberTemp < minAllowedTemp || chamberTemp > maxAllowedTemp;
    }


    private static List<TemperatureRow> extractTemperatureInfoFromSourceFile(String filePath, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        List<TemperatureRow> rows = null;
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            rows = stream
                    .map(l -> l.split("\\|"))
                    .map(r -> new TemperatureRow(r[0], r[1], r[2], r[3], r[4], temperatureSettings)) //TODO improve this
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot read file: " +filePath);
            System.exit(-1);
        }
        return rows;
    }

    private static void writeXlsxFile(List<TemperatureRow> fixedRows, String xlsxPath) {
        XSSFWorkbook myWorkBook = new XSSFWorkbook ();
        XSSFSheet dataSheet = myWorkBook.createSheet("data");

        int rowNumber = 0;
        XSSFRow header = dataSheet.createRow(rowNumber++);
        header.createCell(0, CellType.STRING).setCellValue("Date");
        header.createCell(1, CellType.STRING).setCellValue(fixedRows.get(0).chamberSensorName);
        header.createCell(2, CellType.STRING).setCellValue(fixedRows.get(0).wortSensorName);

        CellStyle cellStyle = myWorkBook.createCellStyle();
        CreationHelper createHelper = myWorkBook.getCreationHelper();
        cellStyle.setDataFormat(
                createHelper.createDataFormat().getFormat("dd-MM-yyyy HH:mm:ss"));

        for (TemperatureRow r : fixedRows) {
            XSSFRow xRow = dataSheet.createRow(rowNumber++);
            XSSFCell dateCell = xRow.createCell(0);
            Date out = Date.from(r.date.atZone(ZoneId.systemDefault()).toInstant());
            dateCell.setCellValue(out);
            dateCell.setCellStyle(cellStyle);
            xRow.createCell(1).setCellValue(r.chamberTemp);
            xRow.createCell(2).setCellValue(r.wortTemp);
        }

        writeXlsxChart(dataSheet, fixedRows.size());
        System.out.println("writing xlsx: " + xlsxPath);
        File myFile = new File(xlsxPath);
        try(FileOutputStream fos = new FileOutputStream (myFile)){
            myWorkBook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to write xlsx file: " + xlsxPath);
        }
    }

    private static void writeXlsxChart(XSSFSheet dataSheet, int size) {
        System.out.println("write xlsx chart");

        Drawing drawing = dataSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 1, 1, 10, 30);
        Chart chart = drawing.createChart(anchor);

        ChartAxis bottomAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.BOTTOM);
        ChartAxis leftAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);
        leftAxis.setMajorTickMark(AxisTickMark.CROSS);
        leftAxis.setMinorTickMark(AxisTickMark.CROSS);

        ScatterChartData scatterChartData = chart.getChartDataFactory().createScatterChartData();

        ChartDataSource<String> xs = DataSources.fromStringCellRange(dataSheet, CellRangeAddress.valueOf("A2:A" +size));
        ChartDataSource<Number> sensor1 = DataSources.fromNumericCellRange(dataSheet, CellRangeAddress.valueOf("B2:B" +size));
        ScatterChartSeries sensor1Series = scatterChartData.addSerie(xs, sensor1);
        sensor1Series.setTitle(dataSheet.getRow(0).getCell(1).getStringCellValue());

        ChartDataSource<Number> sensor2 = DataSources.fromNumericCellRange(dataSheet, CellRangeAddress.valueOf("C2:C" +size));
        ScatterChartSeries sensor2Series = scatterChartData.addSerie(xs, sensor2);
        sensor2Series.setTitle(dataSheet.getRow(0).getCell(2).getStringCellValue());

        chart.getOrCreateLegend();
        chart.plot(scatterChartData, bottomAxis, leftAxis);
    }

    private static Double avg(CircularFifoQueue<Double> lastValues) {
        double sum  = 0;
        int i=0;

        for (Double d : lastValues) {
            sum+=d;
            i++;
        }
        return sum/i;
    }

    public static class TemperatureRow {
        private LocalDateTime date;
        private Double chamberTemp = 0D;
        private String chamberSensorName;
        private Double wortTemp =0D;
        private String wortSensorName;
        private Double settingTemperature =0D;


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


            return
                    "[ " + toJsDate(date) + "," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";

        }
    }

    private static class DateRange {
        final LocalDateTime sd;
        final LocalDateTime ed;

        DateRange(LocalDateTime sd, LocalDateTime ed) {
            this.sd = sd;
            this.ed = ed;
        }
    }

    private static DateRange buildDataRange(String startDate, String endDate) {
        LocalDateTime sd = null;
        LocalDateTime ed = null;
        if (StringUtils.isNotBlank(startDate)) {
            sd = LocalDateTime.parse(startDate, ISO_LOCAL_DATE_TIME);
        }
        if (StringUtils.isNotBlank(endDate)) {
            ed = LocalDateTime.parse(endDate, ISO_LOCAL_DATE_TIME);
        }
        return new DateRange(sd, ed);
    }

    private static String getProperty(Properties p, String key) {
        String value = p.getProperty(key);
        return value!=null?value.trim():null;
    }

    private static String getPropertyOrDefault(Properties p, String key, String def) {
        String value = p.getProperty(key, def);
        return value.trim();
    }

    static String toJsDate(LocalDateTime date) {
        int year  = date.getYear();
        int month = date.getMonthValue();
        int day   = date.getDayOfMonth();
        int hour = date.getHour();
        int minute = date.getMinute();
        int second = date.getSecond();

        return "new Date(" + year + ", " + (month - 1) + ", " + day + ", " + hour + ", " + minute + ", " + second + ", 0)";
    }
}
