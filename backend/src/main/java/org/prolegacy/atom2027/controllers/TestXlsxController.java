package org.prolegacy.atom2027.controllers;

import lombok.Data;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.instancio.Instancio;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("xlsx")
public class TestXlsxController {
    @PostMapping("test")
    public ResponseEntity<byte[]> getXlsxWithCharts() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 1. Create a sheet and fill it with random data from Instancio
            XSSFSheet dataSheet = workbook.createSheet("Sales Data");

            // Generate 10 random SalesRecord objects
            List<SalesRecord> records = Instancio.ofList(SalesRecord.class)
                    .size(10)
                    .create();

            // Header row
            XSSFRow headerRow = dataSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Product");
            headerRow.createCell(1).setCellValue("Category");
            headerRow.createCell(2).setCellValue("Units Sold");
            headerRow.createCell(3).setCellValue("Revenue");

            // Data rows
            int rowIndex = 1;
            for (SalesRecord record : records) {
                XSSFRow row = dataSheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getProduct());
                row.createCell(1).setCellValue(record.getCategory());
                row.createCell(2, CellType.NUMERIC).setCellValue(record.getUnitsSold());
                row.createCell(3, CellType.NUMERIC).setCellValue(record.getRevenue());
            }

            // 2. Create a bar chart (Units Sold per product)
            XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
            XSSFClientAnchor barAnchor = drawing.createAnchor(100, 50, -30, 55, 5, 0, 15, 15);
            XSSFChart barChart = drawing.createChart(barAnchor);

            // Define data sources for the bar chart
            XDDFDataSource<String> products = XDDFDataSourcesFactory.fromStringCellRange(dataSheet,
                    new CellRangeAddress(1, records.size(), 0, 0));
            XDDFNumericalDataSource<Double> unitsSold = XDDFDataSourcesFactory.fromNumericCellRange(dataSheet,
                    new CellRangeAddress(1, records.size(), 2, 2));

            // Create axes and chart data
            XDDFCategoryAxis bottomAxis = barChart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = barChart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFChartData barData = barChart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
            XDDFChartData.Series series = barData.addSeries(products, unitsSold);
            series.setTitle("Units Sold", null);
            XDDFShapeProperties shapeProperties = series.getShapeProperties();
            if (shapeProperties == null) {
                shapeProperties = new XDDFShapeProperties();
            }
            shapeProperties.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(PresetColor.ORANGE)));
            series.setShapeProperties(shapeProperties);

            barChart.plot(barData);
            barChart.setTitleText("Units Sold by Product");
            barChart.setTitleOverlay(false);

            // 3. Create a pie chart (Revenue share by category)
            // For simplicity, we aggregate revenue by category into a helper area
            // (In a real app you might compute this separately)
            XSSFSheet helperSheet = workbook.createSheet("Category Revenue");
            XSSFRow helperHeader = helperSheet.createRow(0);
            helperHeader.createCell(0).setCellValue("Category");
            helperHeader.createCell(1).setCellValue("Total Revenue");

            // Group revenue by category (simple example, assumes categories are distinct)
            // For a robust solution, use a Map<String, Double> to aggregate.
            // Here we just write each record’s category and revenue for demonstration.
            int helperRow = 1;
            for (SalesRecord record : records) {
                XSSFRow r = helperSheet.createRow(helperRow++);
                r.createCell(0).setCellValue(record.getCategory());
                r.createCell(1, CellType.NUMERIC).setCellValue(record.getRevenue());
            }

            XSSFClientAnchor pieAnchor = drawing.createAnchor(0, 0, 0, 0, 5, 17, 15, 32);
            XSSFChart pieChart = drawing.createChart(pieAnchor);

            XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(helperSheet,
                    new CellRangeAddress(1, helperRow - 1, 0, 0));
            XDDFNumericalDataSource<Double> revenues = XDDFDataSourcesFactory.fromNumericCellRange(helperSheet,
                    new CellRangeAddress(1, helperRow - 1, 1, 1));

            XDDFChartData pieData = pieChart.createData(ChartTypes.PIE, null, null);
            XDDFChartData.Series pieSeries = pieData.addSeries(categories, revenues);
            pieSeries.setTitle("Revenue by Category", null);
            pieChart.plot(pieData);
            pieChart.setTitleText("Revenue Share by Category");
            pieChart.setTitleOverlay(false);
            pieChart.getOrAddLegend().setPosition(LegendPosition.RIGHT);

            // 4. Write the workbook to the output stream and return as ResponseEntity
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "random_charts.xlsx");
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    @Data
    public static class SalesRecord {
        private String product;
        private String category;
        private int unitsSold;
        private double revenue;
    }
}
