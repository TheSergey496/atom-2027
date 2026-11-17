package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EventStatusId;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class EventStatus {
    @Id
    private EventStatusId id;
    private String name;
}