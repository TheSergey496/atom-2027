package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.ProtocolId;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolDto {
    private ProtocolId id;
    private CheckDto check;
    private Instant protocolTime;
    private EmployeeDto employee;
    private EdificeDto edifice;
    private String result;
    private String defects;
    private String conclusion;
    private String recommendations;
    private Boolean isNotPlanned;
}
