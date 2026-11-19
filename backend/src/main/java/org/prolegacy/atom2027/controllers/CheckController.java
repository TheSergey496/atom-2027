package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.CheckDto;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.readers.CheckReader;
import org.prolegacy.atom2027.services.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("checks")
@CrossOrigin("*")
public class CheckController {
    @Autowired
    private CheckReader checkReader;
    @Autowired
    private CheckService checkService;

    @GetMapping
    public List<CheckDto> getChecks(
            @RequestParam(required = false, defaultValue = "false") boolean onlyCancelled,
            @RequestParam(required = false, defaultValue = "false") boolean onlyUnfinished
    ) {
        return checkReader.getChecks(onlyCancelled, onlyUnfinished);
    }

    @GetMapping("{checkId}")
    public CheckDto getCheck(@PathVariable CheckId checkId) {
        return checkReader.getCheck(checkId);
    }

    @PostMapping
    public CheckDto createCheck(@RequestBody CheckDto dto) {
        return checkService.createCheck(dto);
    }

    @PutMapping
    public CheckDto updateCheck(@RequestBody CheckDto dto) {
        return checkService.updateCheck(dto);
    }

    @DeleteMapping("{checkId}")
    public CheckDto cancelCheck(@PathVariable CheckId checkId) {
        return checkService.cancelCheck(checkId);
    }
}
