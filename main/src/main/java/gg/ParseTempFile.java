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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParseTempFile {

    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY);//.  "##.##");

    public static void main(String[] args) throws IOException {
        System.out.println("Start. Parameters: " + Arrays.asList(args));
        NumberFormat nf = DecimalFormat.getInstance(Locale.ITALY);
       // nf.setx//.  "##.##");
        DateRange dataRange = buildDataRange(args);
        String filePath = args[0];

        List<MyRow> convert = convert(dataRange, filePath);
        GenerateChart.generateChart(convert.toString());

        //  writeXlsxFile(fixedRows);
    }

    private static List<MyRow> convert(DateRange dataRange, String filePath) {
        System.out.println("startDate " + dataRange.sd + " endDate " + dataRange.ed );
        List<MyRow> rows = extractTemperatureInfoFromSourceFile(filePath);

        System.out.println("Processing " + rows.size() + " rows");

        CircularFifoQueue<Double> lastAvg1 = new CircularFifoQueue<>(5);
        CircularFifoQueue<Double> lastAvg2 = new CircularFifoQueue<>(5);

        List<MyRow> fixedRows = new ArrayList<>();
        int invalidValuesT1 =0;
        int invalidValuesT2 =0;
        int skippeRows =0;

        for (MyRow row : rows) {
            if (dataRange.sd != null && row.date.isBefore(dataRange.sd) || dataRange.ed != null && row.date.isAfter(dataRange.ed)) {
                skippeRows++;
                continue;
            }
            if (row.t1.doubleValue() < 0 || row.t1.doubleValue() > 50) {
                row.t1 = avg(lastAvg1);
                invalidValuesT1++;
            }else{
                lastAvg1.add(row.t1.doubleValue());
            }

            if (row.t2.doubleValue() < 0 || row.t2.doubleValue() > 50) {
                row.t2 = avg(lastAvg2);
                invalidValuesT2++;
            }else{
                lastAvg2.add(row.t2.doubleValue());
            }

            fixedRows.add(row);
            //     System.out.println(row);
        }

        System.out.println("Replaced invalid values. T1: " + invalidValuesT1 + " T2: " +invalidValuesT2 + " skipped rows: " +skippeRows);
        System.out.println("Chart: " + fixedRows);

        return fixedRows;
    }

    private static List<MyRow> extractTemperatureInfoFromSourceFile(String filePath) {
        List<MyRow> rows = null;
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            rows = stream
                    // .peek(e -> System.out.println(e))
                    .map(l -> l.split("\\|"))
                    .map(r -> new MyRow(r[0], r[1], r[2], r[3], r[4]))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot read file: " +filePath);
            System.exit(-1);
        }
        return rows;
    }

    private static void writeXlsxFile(List<MyRow> fixedRows) throws IOException {
        XSSFWorkbook myWorkBook = new XSSFWorkbook ();
        XSSFSheet dataSheet = myWorkBook.createSheet("data");

        int rowNumber = 0;
        XSSFRow header = dataSheet.createRow(rowNumber++);
        header.createCell(0, CellType.STRING).setCellValue("Date");
        header.createCell(1, CellType.STRING).setCellValue(fixedRows.get(0).s1);
        header.createCell(2, CellType.STRING).setCellValue(fixedRows.get(0).s2);

        CellStyle cellStyle = myWorkBook.createCellStyle();
        CreationHelper createHelper = myWorkBook.getCreationHelper();
        cellStyle.setDataFormat(
                createHelper.createDataFormat().getFormat("dd-MM-yyyy HH:mm:ss"));

        for (MyRow r : fixedRows) {
            XSSFRow xRow = dataSheet.createRow(rowNumber++);
            XSSFCell dateCell = xRow.createCell(0);
            Date out = Date.from(r.date.atZone(ZoneId.systemDefault()).toInstant());
            dateCell.setCellValue(out);
            dateCell.setCellStyle(cellStyle);
            xRow.createCell(1).setCellValue(r.t1.doubleValue());
            xRow.createCell(2).setCellValue(r.t2.doubleValue());
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

    private static Double avg(CircularFifoQueue<Double> lastAvg1) {
        double sum  = 0;
        int i=0;

        for (Double d : lastAvg1) {
            sum+=d;
            i++;
        }
        return sum/i;
    }

    private static class MyRow {
        @Override
        public String toString() {
            int year  = date.getYear();
            int month = date.getMonthValue();
            int day   = date.getDayOfMonth();
            int hour = date.getHour();
            int minute = date.getMinute();
            int second = date.getSecond();

            return
                    "[ new Date("+year+", "+(month -1) +", "+day+", "+hour+", "+minute+", "+second+", 0)," + t1 + "," +t2+ "]";
            //  ", t2=" + t2 +

        }

        private LocalDateTime date;
        private Number t1 = 0;
        private String s1;
        private Number t2 =0;
        private String s2;


        MyRow(String date, String t1, String s1, String t2, String s2) {
            try {
                Date parse = sdf.parse(date); //TODO remove this when writing file in the ISO DATETIME way
                this.date = parse.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                this.t1 = nf.parse(t1);
                this.s1 = s1;

                this.t2 = nf.parse(t2);
                this.s2 = s2;
            } catch (ParseException e) {
                e.printStackTrace();
            }
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
