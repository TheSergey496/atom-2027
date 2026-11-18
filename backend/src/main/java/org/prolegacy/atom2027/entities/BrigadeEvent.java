package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.entities.ids.BrigadeEventId;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.prolegacy.atom2027.entities.ids.EventId;

import java.time.LocalDateTime;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class BrigadeEvent {
    @Id
    @LongIdGenerator
    private BrigadeEventId id;

    private BrigadeId brigadeId;
    private EventId eventId;
    private Long orderNumber;
    private LocalDateTime startDate;
    private LocalDateTime expectedEndTime;
}
