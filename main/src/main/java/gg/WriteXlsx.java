package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

class WriteXlsx {
    private static final Logger logger = LogManager.getLogger(WriteXlsx.class);

    static void writeXlsxFile(List<TemperatureRow> fixedRows, String xlsxPath) {
        XSSFWorkbook myWorkBook = new XSSFWorkbook ();
        XSSFSheet dataSheet = myWorkBook.createSheet("data");

        int rowNumber = 0;
        XSSFRow header = dataSheet.createRow(rowNumber++);
        header.createCell(0, CellType.STRING).setCellValue("Date");
        header.createCell(1, CellType.STRING).setCellValue("Room");
        header.createCell(2, CellType.STRING).setCellValue("Wort");

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
        logger.info("writing xlsx: " + xlsxPath);
        File myFile = new File(xlsxPath);
        try(FileOutputStream fos = new FileOutputStream (myFile)){
            myWorkBook.write(fos);
        } catch (IOException e) {
            logger.error("Unable to write xlsx file: " + xlsxPath, e);
        }
    }

    private static void writeXlsxChart(XSSFSheet dataSheet, int size) {
        logger.info("write xlsx chart");

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
}
