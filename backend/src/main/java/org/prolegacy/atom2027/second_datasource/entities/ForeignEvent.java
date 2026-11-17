package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@Table(name = "events")
public class ForeignEvent {
    @Id
    @Column(name = "id_event")
    private Integer id;
    private Long idEdifice;
    private String resourceType;
    private Instant occurredAt;
    private Long idStatus;
}