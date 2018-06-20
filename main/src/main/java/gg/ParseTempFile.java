package gg;

import org.apache.commons.collections4.queue.CircularFifoQueue;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParseTempFile {

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY);

    public static void main(String[] args) throws IOException {
        System.out.println("Start. Parameters: " + Arrays.asList(args));
        DateRange dataRange = buildDataRange(args);
        String filePath = args[0];

        LinkedHashMap<LocalDateTime, Double> settingsTemperature = new LinkedHashMap<>();
        settingsTemperature.put(LocalDateTime.of(LocalDate.of(2018, Month.JUNE, 11), LocalTime.of(20, 0, 0)), 17D);
        settingsTemperature.put(LocalDateTime.of(LocalDate.of(2018, Month.JUNE, 13), LocalTime.of(13, 45, 0)), 18D);
        settingsTemperature.put(LocalDateTime.of(LocalDate.of(2018, Month.JUNE, 16), LocalTime.of(10, 0, 0)), 19D);
        settingsTemperature.put(LocalDateTime.of(LocalDate.of(2018, Month.JUNE, 18), LocalTime.of(22, 0, 0)), 20D);
        settingsTemperature.put(LocalDateTime.of(LocalDate.of(2019, Month.JUNE, 18), LocalTime.of(22, 0, 0)), 2D);
        StatisticalInfo statisticalInfo = convert(dataRange, filePath, settingsTemperature);
        GenerateChart.generateChart(statisticalInfo);
        //writeXlsxFile(fixedRows);
    }

    private static StatisticalInfo convert(DateRange dataRange, String filePath, LinkedHashMap<LocalDateTime, Double> settingsTemperature) {
        System.out.println("startDate " + dataRange.sd + " endDate " + dataRange.ed);
        List<TemperatureRow> rows = extractTemperatureInfoFromSourceFile(filePath, settingsTemperature);

        System.out.println("Processing " + rows.size() + " rows");

        CircularFifoQueue<Double> lastAvgChamber = new CircularFifoQueue<>(5);
        CircularFifoQueue<Double> lastAvgWort = new CircularFifoQueue<>(5);

        StatisticalInfo stats = new StatisticalInfo();
        for (TemperatureRow row : rows) {
            if (dataRange.sd != null && row.date.isBefore(dataRange.sd) || dataRange.ed != null && row.date.isAfter(dataRange.ed)) {
                stats.skippedDates++;
                continue;
            }
            if (row.chamberTemp < 0 || row.chamberTemp > 50) {
                row.chamberTemp = avg(lastAvgChamber);
                stats.invalidValuesChamber++;
            } else {
                lastAvgChamber.add(row.chamberTemp);
            }

            if (row.wortTemp < 0 || row.wortTemp > 50) {
                row.wortTemp = avg(lastAvgWort);
                stats.invalidValuesWort++;
            } else {
                lastAvgWort.add(row.wortTemp);
            }

            stats.fixedRows.add(row);
        }

        stats.chamberStats = stats.fixedRows.stream()
                .collect(Collectors.summarizingDouble(r-> r.chamberTemp));

        stats.wortStats = stats.fixedRows.stream()
                .collect(Collectors.summarizingDouble(r-> r.wortTemp));

        System.out.println(stats);

        return stats;
    }


    private static List<TemperatureRow> extractTemperatureInfoFromSourceFile(String filePath, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
        List<TemperatureRow> rows = null;
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            rows = stream
                    .map(l -> l.split("\\|"))
                    .map(r -> new TemperatureRow(r[0], r[1], r[2], r[3], r[4], temperatureSettings))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot read file: " +filePath);
            System.exit(-1);
        }
        return rows;
    }

    private static void writeXlsxFile(List<TemperatureRow> fixedRows) throws IOException {
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

        writeChart(dataSheet, fixedRows.size());
        String pathname = "C:\\Users\\gab\\temperature.xlsx";
        System.out.println("writing xlsx: " + pathname);
        File myFile = new File(pathname);
        FileOutputStream fos = new FileOutputStream (myFile);
        myWorkBook.write(fos);

        // Read more: http://www.java67.com/2014/09/how-to-read-write-xlsx-file-in-java-apache-poi-example.html#ixzz5HwIOZwNL
    }

    private static void writeChart(XSSFSheet dataSheet, int size) {
        System.out.println("write chart");

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

        private Double getSettingTemperature(LocalDateTime date, LinkedHashMap<LocalDateTime, Double> temperatureSettings) {
            for (Map.Entry<LocalDateTime, Double> set : temperatureSettings.entrySet()) {
                if (date.isBefore(set.getKey())){
                    return set.getValue();
                }
            }

            return 20D;
        }

        @Override
        public String toString() {
            int year  = date.getYear();
            int month = date.getMonthValue();
            int day   = date.getDayOfMonth();
            int hour = date.getHour();
            int minute = date.getMinute();
            int second = date.getSecond();

            return
                    "[ new Date("+year+", "+(month -1) +", "+day+", "+hour+", "+minute+", "+second+", 0)," + chamberTemp + "," + wortTemp + "," + settingTemperature + "]";

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

    private static DateRange buildDataRange(String[] args) {
        LocalDateTime sd = null;
        LocalDateTime ed = null;
        if (args.length > 0) {
            sd = LocalDateTime.parse(args[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (args.length == 3) {
                ed = LocalDateTime.parse(args[2], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
        return new DateRange(sd, ed);
    }
}
