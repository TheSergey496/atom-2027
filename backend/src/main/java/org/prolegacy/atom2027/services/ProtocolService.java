package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
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
            check = checkRepository.save(new Check());
        }
        if (protocolDto.getEmployee() == null || protocolDto.getEmployee().getId() == null) {
            throw new BusinessLogicException("Не указан проверяющий работник");
        }
        final EmployeeDto employee = employeeReader.getEmployee(protocolDto.getEmployee().getId());
        final Edifice edifice = edificeRepository.findById(check.getEdificeId())
                .orElseThrow(() -> new BusinessLogicException("Не найден объект с id=%s".formatted(protocolDto.getEdifice().getId())));

        String qualificationError = null;
        final boolean badForWater = edifice.isHasWater() && !employee.getQualifications().contains("water");
        final boolean badForEnergy = edifice.isHasEnergy() && !employee.getQualifications().contains("energy");

        if (badForEnergy && badForWater) {
            qualificationError = "Выбранный проверяющий не обладает кфалификациями по Воде и Электроэнергии";
        } else if (badForWater) {
            qualificationError = "Выбранный проверяющий не обладает кфалификацией по Воде";
        } else if (badForEnergy) {
            qualificationError = "Выбранный проверяющий не обладает кфалификацией по Электроэнергии";
        }

        if (qualificationError != null) {
            throw new BusinessLogicException(qualificationError);
        }

        final Protocol protocol = protocolRepository.save(new Protocol(protocolDto, check.getCheckId(), employee.getId()));

        return protocolReader.getProtocol(protocol.getId());
    }
}
