package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.EdificeDto;
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
@RequestMapping("edifices")
@CrossOrigin("*")
public class EdificeController {
    @Autowired
    private EdificeReader edificeReader;
    @Autowired
    private DataStructureLoader dataStructureLoader;
    @Autowired
    private EventsLoaderScheduler eventsLoaderScheduler;

    @GetMapping
    public List<EdificeDto> getEdifices(@RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                        Instant instantAt) {
        return edificeReader.getEdifices(instantAt);
    }

    @DeleteMapping("reset")
    public void resetData() {
        this.eventsLoaderScheduler.setDisabled(true);
        dataStructureLoader.reloadData();
        eventsLoaderScheduler.resetLastTime();
        this.eventsLoaderScheduler.setDisabled(false);
    }
}
