package org.prolegacy.atom2027.services;

import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Log4j2
public class ReportService {

    public enum ExportFormat {
        PDF, DOCX, XLSX
    }

    @Value("classpath:test.jrxml")
    private Resource test;

    @Autowired
    private DataSource dataSource;


    public ResponseEntity<byte[]> createTest(ExportFormat exportFormat) {
        try {
            return jasperToResponseEntity(List.of(createTestInternal()), "test", exportFormat);
        } catch (Exception e) {
            log.error(e);
            throw new BusinessLogicException("Ошибка генерации печатной формы", e);
        }
    }

    private JasperPrint createTestInternal() throws IOException, JRException, SQLException {
        InputStream inputStream = test.getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("ru-RU"));
        try (var connection = dataSource.getConnection()) {
            return JasperFillManager.fillReport(jasperReport, parameters, connection);
        }
    }


//    private Resource jasperToPdf(List<JasperPrint> prints) throws IOException, JRException {
//        JRPdfExporter exporter = new JRPdfExporter();
//        SimplePdfExporterConfiguration exportConfig
//                = new SimplePdfExporterConfiguration();
//        exporter.setConfiguration(exportConfig);
//        exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
//        try (var output = new ByteArrayOutputStream()) {
//            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
//            exporter.exportReport();
//            return new ByteArrayResource(output.toByteArray());
//        }
//    }

    private ResponseEntity<byte[]> jasperToResponseEntity(List<JasperPrint> prints,
                                                          String filename,
                                                          ExportFormat format) throws IOException, JRException {
        var exporter = switch (format) {
            case PDF -> {
                JRPdfExporter pdfExporter = new JRPdfExporter();
                pdfExporter.setConfiguration(new SimplePdfExporterConfiguration());
                yield pdfExporter;
            }
            case DOCX -> {
                JRDocxExporter docxExporter = new JRDocxExporter();
                docxExporter.setConfiguration(new SimpleDocxExporterConfiguration());
                yield docxExporter;
            }
            case XLSX -> {
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setConfiguration(new SimpleXlsxExporterConfiguration());
                yield xlsxExporter;
            }
        };

        exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
        try (var output = new ByteArrayOutputStream()) {
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            exporter.exportReport();

            byte[] bytes = output.toByteArray();
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(
                            filename + switch (format) {
                                case PDF -> ".pdf";
                                case DOCX -> ".docx";
                                case XLSX -> ".xlsx";
                            },
                            StandardCharsets.UTF_8
                    ).build();
            return ResponseEntity.ok()
                    .contentType(
                            switch (format) {
                                case PDF -> MediaType.APPLICATION_PDF;
                                case DOCX ->
                                        new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document");
                                case XLSX ->
                                        new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                            }
                    )
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }
}
