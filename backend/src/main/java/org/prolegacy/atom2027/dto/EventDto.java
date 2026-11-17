package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EventId;
import org.prolegacy.atom2027.entities.ids.EventStatusId;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private EventId id;
    private EdificeId edificeId;
    private EdificeDto edifice;
    private String resourceType;
    private Instant occurredAt;
    private EventStatusId eventStatusId;
    private EventStatusDto eventStatus;
}
