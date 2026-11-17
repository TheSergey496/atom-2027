package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EventId;
import org.prolegacy.atom2027.entities.ids.EventStatusId;

import java.time.Instant;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @Id
    private EventId id;
    private EdificeId edificeId;
    private String resourceType;
    private Instant occurredAt;
    private EventStatusId eventStatusId;
}