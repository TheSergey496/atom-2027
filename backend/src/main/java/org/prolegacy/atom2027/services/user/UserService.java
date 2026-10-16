package org.prolegacy.atom2027.services.user;

import com.google.common.collect.Lists;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.entities.User;
import org.prolegacy.atom2027.common.auth.providers.UserProvider;
import org.prolegacy.atom2027.common.auth.readers.UserReader;
import org.prolegacy.atom2027.common.auth.repositories.UserRepository;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProvider userProvider;
    @Autowired
    private UserUtilsService userUtilsService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserReader userReader;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto registerUser(UserDto userDto) {
        userUtilsService.validateUserDoesntExist(userDto);

        User user = new User(userDto, userUtilsService.encodeAndValidatePassword(userDto.getPassword()), Lists.newArrayList());
        userRepository.save(user);

        return userReader.getUser(user.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto updateUser(String password, UserDto userDto) {
        User user = userProvider.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessLogicException("Неверный пароль");
        }

        String newPassword = null;
        if (userDto.getPassword() != null && !passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            newPassword = userUtilsService.encodeAndValidatePassword(
                    userDto.getPassword(),
                    "Новый пароль не может быть пустым",
                    "Отсутствует новый пароль"
            );
        }

        user.update(userDto, newPassword);

        return userReader.getUser(user.getId(), true);
    }
}
