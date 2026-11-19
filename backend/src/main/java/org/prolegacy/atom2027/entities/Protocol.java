package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.dto.ProtocolDto;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.ProtocolId;

import java.time.Instant;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Protocol {
    @Id
    @LongIdGenerator
    private ProtocolId id;
    private CheckId checkId;
    private Instant protocolTime;
    private EmployeeId employeeId;
    private String result;
    private String defects;
    private String conclusion;
    private String recommendations;

    public Protocol(ProtocolDto protocol, CheckId checkId, EmployeeId employeeId) {
        this.checkId = checkId;
        this.protocolTime = Instant.now();
        this.employeeId = employeeId;
        validate(protocol); // TODO спросить и возможно выключить
        this.result = protocol.getResult();
        this.defects = protocol.getDefects();
        this.conclusion = protocol.getConclusion();
        this.recommendations = protocol.getRecommendations();
    }

    private void validate(ProtocolDto dto) {
        if (dto.getResult() == null ) {
            throw new BusinessLogicException("Не указаны Результаты проверки");
        }
        if (dto.getDefects() == null ) {
            throw new BusinessLogicException("Не указаны Обнаруженные замечания и дефекты");
        }
        if (dto.getConclusion() == null ) {
            throw new BusinessLogicException("Не указано Итоговое заключение");
        }
        if (dto.getRecommendations() == null ) {
            throw new BusinessLogicException("Не указаны Рекомендации/комментарии");
        }
    }
}
