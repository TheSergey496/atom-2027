package org.prolegacy.atom2027.services;

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
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void reloadData() {
        edificeRepository.deleteAll();
        edificeDependenciesRepository.deleteAll();
        eventRepository.deleteAll();
        eventStatusRepository.deleteAll();

        final List<ForeignEdifice> foreignEdifices = foreignEdificeRepository.findAll();
        final List<ForeignDependencies> foreignDependencies = foreignDependenciesRepository.findAll();
        final List<ForeignEventStatus> foreignEventStatuses = foreignEventStatusRepository.findAll();

        edificeRepository.saveAll(foreignEdifices.stream().map(
                f -> new Edifice(
                        new EdificeId(f.getId()),
                        f.getName(),
                        f.getResourceType().contains("water"),
                        f.getResourceType().contains("energy"),
                        f.getDistrict(),
                        f.getStreet(),
                        f.getHouseNumber()
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
    }
}
