package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.EventDto;
import org.prolegacy.atom2027.entities.Edifice;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.readers.EventReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("events")
@CrossOrigin("*")
public class EventController {
    @Autowired
    private EventReader eventReader;

    @GetMapping
    public List<EventDto> getEvents(@RequestParam(required = false) EdificeId edificeId,
                                    @RequestParam(required = false, defaultValue = "false") boolean showArchived) {
        return eventReader.getEvents(edificeId, showArchived);
    }
}
