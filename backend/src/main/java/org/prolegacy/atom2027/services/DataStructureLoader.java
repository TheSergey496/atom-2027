package org.prolegacy.atom2027.services;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.prolegacy.atom2027.entities.Edifice;
import org.prolegacy.atom2027.entities.EdificeDependency;
import org.prolegacy.atom2027.entities.EventStatus;
import org.prolegacy.atom2027.entities.ids.EdificeDependencyId;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EventStatusId;
import org.prolegacy.atom2027.repositories.EdificeDependenciesRepository;
import org.prolegacy.atom2027.repositories.EdificeRepository;
import org.prolegacy.atom2027.repositories.EventRepository;
import org.prolegacy.atom2027.repositories.EventStatusRepository;
import org.prolegacy.atom2027.second_datasource.entities.ForeignDependencies;
import org.prolegacy.atom2027.second_datasource.entities.ForeignEdifice;
import org.prolegacy.atom2027.second_datasource.entities.ForeignEventStatus;
import org.prolegacy.atom2027.second_datasource.repositories.ForeignDependenciesRepository;
import org.prolegacy.atom2027.second_datasource.repositories.ForeignEdificeRepository;
import org.prolegacy.atom2027.second_datasource.repositories.ForeignEventStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
    private EdificeRepository edificeRepository;
    @Autowired
    private EdificeDependenciesRepository edificeDependenciesRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventStatusRepository eventStatusRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void reloadData() {
        edificeRepository.deleteAll();
        edificeDependenciesRepository.deleteAll();
        eventRepository.deleteAll();
        eventStatusRepository.deleteAll();

        final List<ForeignEdifice> foreignEdifices = foreignEdificeRepository.findAll();
        final List<ForeignDependencies> foreignDependencies = foreignDependenciesRepository.findAll();
        final List<ForeignEventStatus> foreignEventStatuses = foreignEventStatusRepository.findAll();

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
                edificeRepository.save(e);
            }
        });
    }
}
