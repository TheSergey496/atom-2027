package org.prolegacy.atom2027.init;

import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.repositories.RoleRepository;
import org.prolegacy.atom2027.common.auth.repositories.UserRepository;
import org.prolegacy.atom2027.services.user.UserAdminService;
import org.prolegacy.atom2027.services.user.UserService;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.common.util.InitializationOrder;
import org.prolegacy.atom2027.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Base64;
import java.util.Set;

@Configuration
@Order(InitializationOrder.DEFAULT_USER_INIT)
public class InitialUserGenerator implements ApplicationRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        var user = userRepository.findByEmail("admin@admin.com");
        if (user.isPresent()) {
            return;
        }

        var role = roleRepository.findByName(Role.ADMIN.getRoleName())
                .orElseThrow(() -> new BusinessLogicException("Отсутствует роль администратора"));

        userAdminService.createUser(UserDto.builder()
                .email("admin@admin.com")
                .password(Base64.getEncoder().encodeToString(DigestUtils.sha256("password")))
                .firstname("Админ")
                .lastname("Админов")
                .surname("Админович")
                .roles(Set.of(new RoleDto(role.getId(), null, null)))
                .build()
        );
    }
}
