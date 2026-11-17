package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.EdificeDependencyDto;
import org.prolegacy.atom2027.readers.EdificeDependencyReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("edifices")
@CrossOrigin("*")
public class EdificeDependencyController {
    @Autowired
    private EdificeDependencyReader edificeDependencyReader;

    @GetMapping
    public List<EdificeDependencyDto> getEdificeDependencies() {
        return edificeDependencyReader.getEdificeDependencies();
    }
}
