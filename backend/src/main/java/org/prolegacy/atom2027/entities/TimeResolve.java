package org.prolegacy.atom2027.entities;

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Type;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.TimeResolveId;

import java.time.Duration;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class TimeResolve {
    @Id
    private TimeResolveId id;
    private String resourceType;
    @Type(PostgreSQLIntervalType.class)
    @Column(columnDefinition = "interval")
    private Duration averageResolutionTime;
    private EdificeId edificeId;
}
