package org.dynamcorp.handsaiv2.exception;

import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.utils.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Manejador global de excepciones para todos los controladores de la aplicación.
 * Centraliza la lógica de manejo de errores y proporciona respuestas JSON estructuradas.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorMessage> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                "Recurso no encontrado",
                ex.getClass().getSimpleName(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
    }

    @ExceptionHandler(ToolExecutionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorMessage> handleToolExecutionException(ToolExecutionException ex) {
        log.error("Tool execution error: {}", ex.getMessage());

        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                "Error en la ejecución de herramienta",
                ex.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException ex) {
        log.error("Error interno del servidor: {}", ex.getMessage(), ex);

        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                "Error interno del servidor",
                ex.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Parámetros inválidos: {}", ex.getMessage());

        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                "Parámetros inválidos",
                ex.getClass().getSimpleName(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorMessage> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Estado inválido: {}", ex.getMessage());

        ErrorMessage errorMessage = new ErrorMessage(
                ex.getMessage(),
                "Estado de la operación inválido",
                ex.getClass().getSimpleName(),
                HttpStatus.CONFLICT.value()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorMessage> handleNullPointerException(NullPointerException ex) {
        log.error("Error de referencia nula: {}", ex.getMessage(), ex);

        ErrorMessage errorMessage = new ErrorMessage(
                "Se ha encontrado una referencia nula inesperada",
                "Error interno del servidor",
                ex.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);

        ErrorMessage errorMessage = new ErrorMessage(
                "Ha ocurrido un error inesperado: " + ex.getMessage(),
                "Error interno del servidor",
                ex.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
    }
}
