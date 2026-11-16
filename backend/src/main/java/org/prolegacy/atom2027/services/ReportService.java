package org.prolegacy.atom2027.services;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportService {

    @Value("classpath:test.jrxml")
    private Resource test;

    @Autowired
    private DataSource dataSource;


    public ResponseEntity<byte[]> createTest() {
        try {
            return jasperToPdf(List.of(createTestInternal()));
        } catch (Exception e) {
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

    private ResponseEntity<byte[]> jasperToPdf(List<JasperPrint> prints) throws IOException, JRException {
        JRPdfExporter exporter = new JRPdfExporter();
        SimplePdfExporterConfiguration exportConfig
                = new SimplePdfExporterConfiguration();
        exporter.setConfiguration(exportConfig);
        exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
        try (var output = new ByteArrayOutputStream()) {
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            exporter.exportReport();

            byte[] bytes = output.toByteArray();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\"")
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }
}
