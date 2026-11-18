package org.prolegacy.atom2027.services;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.prolegacy.atom2027.entities.*;
import org.prolegacy.atom2027.entities.ids.*;
import org.prolegacy.atom2027.repositories.*;
import org.prolegacy.atom2027.second_datasource.entities.*;
import org.prolegacy.atom2027.second_datasource.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataStructureLoader {
    @Autowired
    private ForeignEdificeRepository foreignEdificeRepository;
    @Autowired
    private ForeignDependenciesRepository foreignDependenciesRepository;
    @Autowired
    private ForeignEventStatusRepository foreignEventStatusRepository;
    @Autowired
    private ForeignEventRepository foreignEventRepository;
    @Autowired
    private ForeignEmployeeRepository foreignEmployeeRepository;
    @Autowired
    private ForeignEmployeeQualificationRepository foreignEmployeeQualificationRepository;
    @Autowired
    private ForeignTimeResolveRepository foreignTimeResolveRepository;

    @Autowired
    private EdificeRepository edificeRepository;
    @Autowired
    private EdificeDependenciesRepository edificeDependenciesRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventStatusRepository eventStatusRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeQualificationRepository employeeQualificationRepository;
    @Autowired
    private TimeResolveRepository timeResolveRepository;
    @Autowired
    private BrigadeRepository brigadeRepository;
    @Autowired
    private BrigadeMemberRepository brigadeMemberRepository;
    @Autowired
    private BrigadeEventRepository brigadeEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void reloadData() {
        edificeRepository.deleteAllInBatch();
        edificeDependenciesRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        eventStatusRepository.deleteAllInBatch();
        employeeRepository.deleteAllInBatch();
        employeeQualificationRepository.deleteAllInBatch();
        timeResolveRepository.deleteAllInBatch();
        brigadeRepository.deleteAllInBatch();
        brigadeMemberRepository.deleteAllInBatch();
        brigadeEventRepository.deleteAllInBatch();

        final List<ForeignEdifice> foreignEdifices = foreignEdificeRepository.findAll();
        final List<ForeignDependencies> foreignDependencies = foreignDependenciesRepository.findAll();
        final List<ForeignEventStatus> foreignEventStatuses = foreignEventStatusRepository.findAll();
        final List<ForeignEvent> foreignEvents = foreignEventRepository.findAll();
        final List<ForeignEmployee> foreignEmployees = foreignEmployeeRepository.findAll();
        final List<ForeignEmployeeQualification> foreignEmployeeQualifications = foreignEmployeeQualificationRepository.findAll();
        final List<ForeignTimeResolve> foreignTimeResolves = foreignTimeResolveRepository.findAll();

        List<Edifice> edifices = edificeRepository.saveAll(foreignEdifices.stream().map(
                f -> new Edifice(
                        new EdificeId(f.getId()),
                        f.getName(),
                        f.getResourceType().contains("water"),
                        f.getResourceType().contains("energy"),
                        f.getDistrict(),
                        f.getStreet(),
                        f.getHouseNumber(),
                        0L,
                        0L,
                        0L
                )
        ).toList());
        edificeDependenciesRepository.saveAll(foreignDependencies.stream().map(
                f -> new EdificeDependency(
                        new EdificeDependencyId(f.getId()),
                        f.getResourceType().contains("water"),
                        f.getResourceType().contains("energy"),
                        new EdificeId(f.getIdEdificeDependent()),
                        new EdificeId(f.getIdEdificeSource())
                )
        ).toList());
        eventStatusRepository.saveAll(foreignEventStatuses.stream().map(
                f -> new EventStatus(
                        new EventStatusId(f.getId()),
                        f.getName()
                )
        ).toList());
        eventRepository.saveAll(
                foreignEvents.stream().map(e ->
                    new Event(
                            new EventId(e.getId()),
                            new EdificeId(e.getIdEdifice()),
                            e.getResourceType(),
                            e.getOccurredAt(),
                            new EventStatusId(e.getIdStatus())
                    )
                ).toList()
        );

        employeeRepository.saveAll(
                foreignEmployees.stream().map(e ->
                    new Employee(
                            new EmployeeId(e.getTabel()),
                            e.getFirstName(),
                            e.getLastName(),
                            e.getMiddleName(),
                            e.getStartDateExperience()
                    )
                ).toList()
        );

        employeeQualificationRepository.saveAll(
                foreignEmployeeQualifications.stream().map(e ->
                        new EmployeeQualification(
                                new EmployeeQualificationId(e.getIdQualification()),
                                new EmployeeId(e.getIdEmployee()),
                                e.getResourceType(),
                                e.getProff()
                        )
                ).toList()
        );

        timeResolveRepository.saveAll(
                foreignTimeResolves.stream().map(e ->
                        new TimeResolve(
                                new TimeResolveId(e.getIdTime()),
                                e.getResourceType(),
                                e.getAvgResolutionTime(),
                                new EdificeId(e.getIdEdifice())
                        )
                )      .toList()
        );

        edificeDependenciesRepository.flush();

        Map<Long, Long> waterCr = jdbcTemplate.queryForStream(
                """
                        with recursive deps as (
                            select
                                source_edifice_id as parent,
                                target_edifice_id as child
                            from edifice_dependency
                            where has_water = true
                        
                            union
                        
                            select
                                d.parent as parent,
                                t.target_edifice_id as child
                            from deps d
                            inner join edifice_dependency t on t.source_edifice_id = d.child and t.has_water = true
                        )
                        select
                            parent,
                            count(distinct child) as count
                        from deps
                        group by parent
                        """,
                (rs, rowNum) -> {
                    Long sourceId = rs.getLong("parent");
                    Long depCount = rs.getLong("count");
                    return Pair.of(sourceId, depCount);
                }
        ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        Map<Long, Long> energyCr = jdbcTemplate.queryForStream(
                """
                        with recursive deps as (
                            select
                                source_edifice_id as parent,
                                target_edifice_id as child
                            from edifice_dependency
                            where has_energy = true
                        
                            union
                        
                            select
                                d.parent as parent,
                                t.target_edifice_id as child
                            from deps d
                            inner join edifice_dependency t on t.source_edifice_id = d.child and t.has_energy = true
                        )
                        select
                            parent,
                            count(distinct child) as count
                        from deps
                        group by parent
                        """,
                (rs, rowNum) -> {
                    Long sourceId = rs.getLong("parent");
                    Long depCount = rs.getLong("count");
                    return Pair.of(sourceId, depCount);
                }
        ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        long maxIndex = SetUtils.union(
                        waterCr.keySet(),
                        energyCr.keySet()
                ).stream()
                .map(k -> waterCr.getOrDefault(k, 0L) + energyCr.getOrDefault(k, 0L))
                .max(Long::compareTo)
                .orElse(0L);

        edifices.forEach(e -> {


            Long water = waterCr.getOrDefault(e.getId().longValue(), 0L);
            Long energy = energyCr.getOrDefault(e.getId().longValue(), 0L);
            long total = water + energy;
            if (maxIndex > 0 && total > 0) {
                var index = Math.round((total + 0.0) / maxIndex * 10);
                if (index == 10 && total < maxIndex) {
                    index = 9;
                }
                if (index == 0) {
                    index = 1;
                }
                e.setCriticalIndex(index);
                e.setWaterDependantCount(water);
                e.setEnergyDependantCount(energy);
            }
        });
    }
}
