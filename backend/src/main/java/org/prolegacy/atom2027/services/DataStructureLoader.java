package org.prolegacy.atom2027.services;

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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
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

        Map<Long, Long> cr = jdbcTemplate.queryForStream(
                """
                        with recursive deps as (
                            select
                                source_edifice_id as parent,
                                target_edifice_id as child
                            from edifice_dependency

                            union

                            select
                                d.parent as parent,
                                t.target_edifice_id as child
                            from deps d
                            join edifice_dependency t on t.source_edifice_id = d.child
                        )
                        select
                            parent,
                            count(distinct child) as count
                        from deps
                        group by parent
                        """,
                (rs, rowNum) -> {
                        Long sourceId =  rs.getLong("parent");
                        Long depCount = rs.getLong("count");
                        return Pair.of(sourceId, depCount);
                }
        ).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        Long maxIndex = cr.values().stream().max(Long::compareTo).orElse(0L);

        edifices.forEach(e -> {

            if (cr.containsKey(e.getId().longValue())) {
                Long ind = cr.get(e.getId().longValue());
                var index = Math.round((ind + 0.0) / maxIndex * 10);
                if (index == 10 && ind < maxIndex) {
                    index = 9;
                }
                if (index == 0 && ind > 0) {
                    index = 1;
                }
                e.setCriticalIndex(index);
                edificeRepository.save(e);
            }
        });
    }
}
