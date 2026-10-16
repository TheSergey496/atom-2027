package org.prolegacy.atom2027.services.user;

import org.apache.commons.codec.digest.DigestUtils;
import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.entities.Role;
import org.prolegacy.atom2027.common.auth.entities.User;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.prolegacy.atom2027.common.auth.exceptions.UserNotFoundException;
import org.prolegacy.atom2027.common.auth.providers.UserProvider;
import org.prolegacy.atom2027.common.auth.readers.UserReader;
import org.prolegacy.atom2027.common.auth.repositories.RoleRepository;
import org.prolegacy.atom2027.common.auth.repositories.UserRepository;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserAdminService {

    @Value("${auth.admin-role:#{null}}")
    private String adminRoleName;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserProvider userProvider;
    @Autowired
    private UserReader userReader;
    @Autowired
    private UserUtilsService userUtilsService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto) {
        userUtilsService.validateUserDoesntExist(userDto);
        User user = new User(
                userDto,
                userUtilsService.encodeAndValidatePassword(userDto.getPassword()),
                getAndValidateRoles(userDto)
        );
        userRepository.save(user);

        return userReader.getUser(user.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto updateUser(UserId userId, UserDto userDto) {
        User user = Optional.ofNullable(userId)
                .flatMap(uId -> userRepository.findById(uId))
                .orElseThrow(() -> new UserNotFoundException(userDto.getEmail()));

        String adminRoleWithPrefix = Optional.ofNullable(adminRoleName).map(r -> "ROLE_" + r)
                .orElseThrow(() -> new BusinessLogicException("Не найдена роль администратора"));

        if (user.getRoles().stream().map(Role::getName).anyMatch(roleName -> Objects.equals(roleName, adminRoleWithPrefix))
                && userDto.getRoles().stream().map(RoleDto::getName).noneMatch(roleName -> Objects.equals(roleName, adminRoleWithPrefix))) {
            this.validateIsntLastAdmin(userId, "Невозможно забрать роль администратора у последнего пользователя с ролью администратора");
            this.validateIsNotSelfChange(userId, "Невозможно забрать роль администратора у текущего пользователя");
        }

        user.adminUpdate(userDto, getAndValidateRoles(userDto));

        return userReader.getUser(user.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto archiveUser(UserId userId) {
        validateIsNotSelfChange(userId, "Невозможно заархивировать текущего пользователя");
        validateIsntLastAdmin(userId, "Невозможно заархивировать последнего пользователя с ролью администратора");
        User user = Optional.ofNullable(userId)
                .flatMap(uId -> userRepository.findById(uId))
                .orElseThrow(UserNotFoundException::new);
        user.archive();

        return userReader.getUser(user.getId());
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto unarchiveUser(UserId userId) {
        User user = Optional.ofNullable(userId)
                .flatMap(uId -> userRepository.findById(uId))
                .orElseThrow(UserNotFoundException::new);
        user.unarchive();

        return userReader.getUser(user.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto resetPassword(UserId userId) {
        User user = Optional.ofNullable(userId)
                .flatMap(uId -> userRepository.findById(uId))
                .orElseThrow(UserNotFoundException::new);
        validateIsNotSelfChange(userId, "Невозможно сбросить пароль текущего пользователя");
        user.setPassword(userUtilsService.encodeAndValidatePassword(Base64.getEncoder().encodeToString(DigestUtils.sha256(user.getEmail()))));

        return userReader.getUser(user.getId());
    }

    private List<Role> getAndValidateRoles(UserDto userDto) {
        List<Role> roles = Optional.ofNullable(userDto.getRoles())
                .map(roleDtoList -> {
                    if (roleDtoList.isEmpty()) {
                        return new ArrayList<Role>();
                    }
                    return roleRepository.findAllById(
                            roleDtoList.stream().map(RoleDto::getId).peek(roleId -> {
                                if (Objects.isNull(roleId)) {
                                    throw new BusinessLogicException("Некорректная роль");
                                }
                            }).toList());
                }).orElseGet(ArrayList::new);
        if (roles.isEmpty()) {
            throw new BusinessLogicException("Пользователь должен иметь хотя бы одну роль");
        }
        return roles;
    }

    private void validateIsntLastAdmin(UserId userId, String errorMessage) {
        List<User> usersWithAdminRole = Optional.ofNullable(adminRoleName)
                .map(roleWithoutPrefix -> userRepository.findActualUsersWithRoleName("ROLE_" + roleWithoutPrefix))
                .orElseThrow(() -> new BusinessLogicException("Не найдена роль администратора"));
        if (usersWithAdminRole.size() == 1 && usersWithAdminRole.get(0).getId().equals(userId)) {
            throw new BusinessLogicException(errorMessage);
        }
    }

    private void validateIsNotSelfChange(UserId userId, String errorMessage) {
        if (Optional.ofNullable(userProvider.get()).map(User::getId).map(userId1 -> Objects.equals(userId, userId1)).orElse(false)) {
            throw new BusinessLogicException(errorMessage);
        }
    }
}
