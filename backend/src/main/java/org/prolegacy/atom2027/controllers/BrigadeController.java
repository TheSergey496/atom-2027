package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.BrigadeDto;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.readers.BrigadeReader;
import org.prolegacy.atom2027.readers.EdificeReader;
import org.prolegacy.atom2027.services.DataStructureLoader;
import org.prolegacy.atom2027.services.EventsLoaderScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("brigades")
@CrossOrigin("*")
public class BrigadeController {
    @Autowired
    private BrigadeReader brigadeReader;

    @GetMapping
    public List<BrigadeDto> getBrigades() {
        return brigadeReader.getBrigades();
    }
}
