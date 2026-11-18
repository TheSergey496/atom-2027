package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.BrigadeId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrigadeDto {
    private BrigadeId id;
    private String name;
    private List<EventDto> events;
}
