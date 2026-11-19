package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.ProtocolDto;
import org.prolegacy.atom2027.entities.ids.ProtocolId;
import org.prolegacy.atom2027.readers.ProtocolReader;
import org.prolegacy.atom2027.services.ProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@TypescriptEndpoint
@RequestMapping("protocols")
@CrossOrigin("*")
public class ProtocolController {
    @Autowired
    private ProtocolReader protocolReader;
    @Autowired
    private ProtocolService protocolService;

    @GetMapping("{id}")
    public ProtocolDto getProtocol(@PathVariable ProtocolId id) {
        return protocolReader.getProtocol(id);
    }

    @GetMapping
    public List<ProtocolDto> getProtocols() {
        return protocolReader.getProtocols();
    }

    @PostMapping
    public ProtocolDto createProtocol(@RequestBody ProtocolDto protocolDto) {
        return protocolService.createProtocol(protocolDto);
    }
}
