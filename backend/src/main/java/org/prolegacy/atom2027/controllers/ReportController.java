package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping("reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("test")
    public ResponseEntity<byte[]> test() {
        return reportService.createTest();
    }
}
