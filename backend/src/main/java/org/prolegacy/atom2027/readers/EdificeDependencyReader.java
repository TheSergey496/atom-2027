package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EdificeDependencyDto;
import org.prolegacy.atom2027.entities.QEdificeDependency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EdificeDependencyReader {
    private static final QEdificeDependency edificeDependency = QEdificeDependency.edificeDependency;

    @Autowired
    private JPAQueryFactory queryFactory;


    public List<EdificeDependencyDto> getEdificeDependencies() {
        return queryFactory.from(edificeDependency)
                .orderBy(edificeDependency.id.asc())
                .selectDto(EdificeDependencyDto.class)
                .fetch();
    }
}
