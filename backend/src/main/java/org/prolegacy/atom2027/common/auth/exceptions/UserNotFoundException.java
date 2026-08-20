package org.prolegacy.atom2027.common.auth.exceptions;

import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;

public class UserNotFoundException extends BusinessLogicException {
    public UserNotFoundException() {
        super("Пользователь не найден");
    }

    public UserNotFoundException(String email) {
        super(email == null ? "Пользователь не найден" : "Пользователь с email-адресом %s не найден".formatted(email));
    }
}
