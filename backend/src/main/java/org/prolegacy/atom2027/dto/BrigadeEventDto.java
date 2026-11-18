package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.BrigadeEventId;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrigadeEventDto {
    private BrigadeEventId id;
    private Long orderNumber;
    private BrigadeDto brigade;
    private EventDto event;
    private EdificeDto edifice;
    private String averageResolutionTimeStr;
    private String actualResolutionTimeStr;
    private LocalDateTime startDate;
    private LocalDateTime expectedEndTime;
}
