package org.prolegacy.atom2027.second_datasource.entities;

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Duration;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "time_resolve")
public class ForeignTimeResolve {
    @Id
    private Long idTime;
    private String resourceType;
    @Type(PostgreSQLIntervalType.class)
    private Duration avgResolutionTime;
    private Long idEdifice;
}
