package org.prolegacy.atom2027.services.user;

import org.apache.commons.lang3.StringUtils;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.repositories.UserRepository;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
class UserUtilsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String encodeAndValidatePassword(String password) {
        return encodeAndValidatePassword(
                password,
                "Пароль не может быть пустым",
                "Отсутствует пароль"
        );
    }

    public String encodeAndValidatePassword(String password,
                                            String passwordIsBlankExceptionMessage,
                                            String passwordIsNullExceptionMessage) {
        return Optional.ofNullable(password)
                .map(s -> {
                    if (StringUtils.isBlank(s)) {
                        throw new BusinessLogicException(passwordIsBlankExceptionMessage);
                    }
                    return s;
                })
                .map(pass -> passwordEncoder.encode(pass))
                .orElseThrow(() -> new BusinessLogicException(passwordIsNullExceptionMessage));
    }

    public void validateUserDoesntExist(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail().toLowerCase(Locale.US))) {
            throw new BusinessLogicException("Пользователь с адресом электронной почты %s уже существует".formatted(userDto.getEmail()));
        }
    }
}
