package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.dto.EmployeeDto;
import org.prolegacy.atom2027.dto.ProtocolDto;
import org.prolegacy.atom2027.entities.Check;
import org.prolegacy.atom2027.entities.Edifice;
import org.prolegacy.atom2027.entities.Protocol;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.readers.EmployeeReader;
import org.prolegacy.atom2027.readers.ProtocolReader;
import org.prolegacy.atom2027.repositories.CheckRepository;
import org.prolegacy.atom2027.repositories.EdificeRepository;
import org.prolegacy.atom2027.repositories.ProtocolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProtocolService {
    @Autowired
    private ProtocolRepository protocolRepository;
    @Autowired
    private CheckRepository checkRepository;
    @Autowired
    private EmployeeReader employeeReader;
    @Autowired
    private EdificeRepository edificeRepository;

    @Autowired
    private ProtocolReader protocolReader;

    @Transactional
    public ProtocolDto createProtocol(ProtocolDto protocolDto) {
        final Check check;
        if (protocolDto.getCheck() != null && protocolDto.getCheck().getCheckId() != null) {
            final CheckId checkId = protocolDto.getCheck().getCheckId();
            check = checkRepository.findById(checkId)
                    .orElseThrow(() -> new BusinessLogicException("Не найдена проверка с id=%s".formatted(checkId)));
        } else {
            var edificeId = Optional.ofNullable(protocolDto.getEdifice())
                    .map(EdificeDto::getId)
                    .orElseThrow(() -> new BusinessLogicException("Не задан объект для проверки"));
            Edifice edifice = edificeRepository.findById(edificeId)
                    .orElseThrow(() -> new BusinessLogicException("Не найден объект с id = %s".formatted(edificeId)));
            check = checkRepository.save(new Check(protocolDto));
        }
        if (protocolDto.getEmployee() == null || protocolDto.getEmployee().getId() == null) {
            throw new BusinessLogicException("Не указан проверяющий рабочий");
        }
        final EmployeeDto employee = Optional.ofNullable(employeeReader.getEmployee(protocolDto.getEmployee().getId()))
                .orElseThrow(() -> new BusinessLogicException("Не найден рабочий с id = %s"));
        final Edifice edifice = edificeRepository.findById(check.getEdificeId())
                .orElseThrow(() -> new BusinessLogicException("Не найден объект с id=%s".formatted(protocolDto.getEdifice().getId())));

        String qualificationError = null;
        final boolean badForWater = edifice.isHasWater() && !employee.getQualifications().contains("Вода");
        final boolean badForEnergy = edifice.isHasEnergy() && !employee.getQualifications().contains("Электроэнергия");

        if (badForEnergy && badForWater) {
            qualificationError = "Выбранный проверяющий должен обладать квалификациями по воде и электроэнергии";
        } else if (badForWater) {
            qualificationError = "Выбранный проверяющий должен обладать квалификацией по воде";
        } else if (badForEnergy) {
            qualificationError = "Выбранный проверяющий должен обладать квалификацией по электроэнергии";
        }

        if (qualificationError != null) {
            throw new BusinessLogicException(qualificationError);
        }

        final Protocol protocol = protocolRepository.save(new Protocol(protocolDto, check.getCheckId(), employee.getId()));
        check.finish();

        return protocolReader.getProtocol(protocol.getId());
    }
}
