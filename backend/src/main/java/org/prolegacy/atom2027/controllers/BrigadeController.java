package org.prolegacy.atom2027.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.dto.BrigadeDto;
import org.prolegacy.atom2027.entities.Brigade;
import org.prolegacy.atom2027.entities.BrigadeEvent;
import org.prolegacy.atom2027.entities.BrigadeMember;
import org.prolegacy.atom2027.readers.BrigadeReader;
import org.prolegacy.atom2027.repositories.BrigadeEventRepository;
import org.prolegacy.atom2027.repositories.BrigadeMemberRepository;
import org.prolegacy.atom2027.repositories.BrigadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@TypescriptEndpoint
@RequestMapping("brigades")
@CrossOrigin("*")
public class BrigadeController {
    @Autowired
    private BrigadeReader brigadeReader;
    @Autowired
    private BrigadeRepository brigadeRepository;
    @Autowired
    private BrigadeMemberRepository brigadeMemberRepository;
    @Autowired
    private BrigadeEventRepository brigadeEventRepository;

    @GetMapping
    public List<BrigadeDto> getBrigades() {
        return brigadeReader.getBrigades();
    }

    public record ExportBrigadeDto(
            @JsonProperty("brigade_id") Long brigadeId,
            List<Long> employees,
            @JsonProperty("event_order") List<Long> eventsOrder
    ) {
    }

    public record BrigadesResponse(List<ExportBrigadeDto> brigades) {
    }

    @GetMapping("json")
    public BrigadesResponse exportBrigadesJson() {
        List<Brigade> brigades = brigadeRepository.findAll();
        List<BrigadeMember> members = brigadeMemberRepository.findAll();
        List<BrigadeEvent> events = brigadeEventRepository.findAll();

        // Group employees by brigade
        Map<Long, List<Long>> membersByBrigade = members.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getBrigadeId().longValue(),
                        Collectors.mapping(m -> m.getEmployeeId().longValue(), Collectors.toList())
                ));

        // Group events by brigade and sort by orderNumber
        Map<Long, List<Long>> eventsByBrigade = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getBrigadeId().longValue(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(BrigadeEvent::getOrderNumber))
                                        .map(e -> e.getEventId().longValue())
                                        .collect(Collectors.toList())
                        )
                ));

        // Map to DTOs
        List<ExportBrigadeDto> dtos = brigades.stream()
                .map(b -> {
                    Long bId = b.getId().longValue();
                    return new ExportBrigadeDto(
                            bId,
                            membersByBrigade.getOrDefault(bId, Collections.emptyList()),
                            eventsByBrigade.getOrDefault(bId, Collections.emptyList())
                    );
                })
                .toList();

        return new BrigadesResponse(dtos);
    }
}
