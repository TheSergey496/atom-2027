package org.prolegacy.atom2027.common.exceptions;

public class InsufficientPrivilegesException extends RuntimeException {
    public InsufficientPrivilegesException() {
        super("Недостаточно прав для выполнения запроса");
    }
}
