package com.adasoft.pharmasuite.apips.api.common.exception;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.adasoft.pharmasuite.apips.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> handleUnexpectedException(Exception ex,
                                                              HttpServletRequest request) {

        // 1) Log en vuestro formato
        LogManagement.error(
                String.format("Unexpected error %s %s: %s",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage()
                ),
                this
        );

        // 2) Traza completa al log
        ex.printStackTrace();

        // 3) Objeto de error estándar de la API
        ErrorApi error = ErrorApi.builder()
                .code("INTERNAL_ERROR")
                .description("Se ha producido un error inesperado procesando la petición.")
                .message(ex.getMessage())
                .build();

        // 4) Devolvemos 500 (error servidor)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
