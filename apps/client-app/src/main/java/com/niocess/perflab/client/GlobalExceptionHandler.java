package com.niocess.perflab.client;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    Map<String, String> handleNotImplemented(UnsupportedOperationException ex) {
        return Map.of("error", ex== null? "" : ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String valid = String.join(", ",
                    java.util.Arrays.stream(ex.getRequiredType().getEnumConstants())
                            .map(Object::toString)
                            .map(String::toLowerCase)
                            .toArray(String[]::new));
            return Map.of("error", "invalid " + ex.getName() + ": '" + ex.getValue() + "'. Valid values: " + valid);
        }
        return Map.of("error", "invalid parameter: " + ex.getName());
    }
}
