package org.prolegacy.atom2027.common.auth.config;


import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.SetUtils;
import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.entities.Role;
import org.prolegacy.atom2027.common.auth.interfaces.RoleInfo;
import org.prolegacy.atom2027.common.auth.readers.RoleReader;
import org.prolegacy.atom2027.common.auth.repositories.RoleRepository;
import org.prolegacy.atom2027.common.util.InitializationOrder;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Order(InitializationOrder.ROLE_GENERATOR)
@Log4j2
public class RoleGenerator implements ApplicationRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleReader roleReader;

    @Transactional
    @Override
    public void run(@NonNull ApplicationArguments args) {
        final Reflections reflections = new Reflections("org.prolegacy.atom2027");
        Map<String, RoleDto> hardcodedRoles = reflections.getSubTypesOf(RoleInfo.class).stream()
                .filter(Class::isEnum)
                .flatMap(aClass -> Arrays.stream(aClass.getEnumConstants()))
                .map(role -> new RoleDto(null, role.getRoleName(), role.getLocalization()))
                .collect(Collectors.toMap(RoleDto::getName, Function.identity()));
        Map<String, RoleDto> existingRoles = this.roleReader.getRoles()
                .stream()
                .collect(Collectors.toMap(RoleDto::getName, Function.identity()));
        Set<String> existingRoleNames = existingRoles.keySet();
        Set<String> hardcodedRoleNames = hardcodedRoles.keySet();
        Set<String> rolesToDelete = SetUtils.difference(existingRoleNames, hardcodedRoleNames);
        if (!rolesToDelete.isEmpty()) {
            roleRepository.deleteByNameIn(rolesToDelete);
        }
        roleRepository.saveAll(SetUtils.difference(hardcodedRoleNames, existingRoleNames)
                .stream()
                .map(hardcodedRoles::get)
                .map(roleDto -> new Role(roleDto.getName(), roleDto.getLocaleName()))
                .collect(Collectors.toList()));
    }
}
