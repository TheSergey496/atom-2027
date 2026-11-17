package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.EventStatusId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusDto {
    private EventStatusId id;
    private String name;
}
