package org.prolegacy.atom2027.common.exceptions;

public class BusinessLogicException extends RuntimeException {
    public BusinessLogicException() {
        super("Произошла ошибка на сервере");
    }

    public BusinessLogicException(String message) {
        super(message);
    }

    public BusinessLogicException(String message, Exception e) {
        super(message, e);
    }
}
