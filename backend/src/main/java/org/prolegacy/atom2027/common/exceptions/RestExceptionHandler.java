package org.prolegacy.atom2027.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(new ErrorResponse(InsufficientPrivilegesException.class.getSimpleName(), new InsufficientPrivilegesException().getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        ex.printStackTrace();
        if (BusinessLogicException.class.isAssignableFrom(ex.getClass())) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(new ErrorResponse(BusinessLogicException.class.getSimpleName(), ex.getMessage()));
        } else if (InsufficientPrivilegesException.class.isAssignableFrom(ex.getClass())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN.value())
                    .body(new ErrorResponse(InsufficientPrivilegesException.class.getSimpleName(), ex.getMessage()));
        } else if (MaxUploadSizeExceededException.class.isAssignableFrom(ex.getClass())) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(new ErrorResponse(BusinessLogicException.class.getSimpleName(), "Слишком большой файл для загрузки на сервер"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body("Произошла ошибка на сервере");
    }

    public record ErrorResponse(String className, String message) {
    }
}
