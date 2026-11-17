package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.readers.EdificeReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("edifices")
@CrossOrigin("*")
public class EdificeController {
    @Autowired
    private EdificeReader edificeReader;

    @GetMapping
    public List<EdificeDto> getEdifices(@RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                        Instant instantAt) {
        return edificeReader.getEdifices(instantAt);
    }
}
