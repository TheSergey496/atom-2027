package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.EmployeeDto;
import org.prolegacy.atom2027.dto.EmployeeQualificationDto;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.readers.EmployeeQualificationReader;
import org.prolegacy.atom2027.readers.EmployeeReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("employees")
@CrossOrigin("*")
public class EmployeeController {
    @Autowired
    private EmployeeReader employeeReader;
    @Autowired
    private EmployeeQualificationReader employeeQualificationReader;

    @GetMapping
    public List<EmployeeDto> getEmployees() {
        return employeeReader.getEmployees();
    }

    @GetMapping("{id}/qualifications")
    public List<EmployeeQualificationDto> getQualifications(@PathVariable EmployeeId id) {
        return employeeQualificationReader.getEmployeeQualifications(id);
    }
}
