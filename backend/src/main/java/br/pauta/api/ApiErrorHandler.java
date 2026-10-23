package br.pauta.api;

import br.pauta.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/** Contrato unico de erro da API: { code, message, timestamp }. */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    public record ApiError(String code, String message, Instant timestamp) {
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.code(), e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_PARAMETER", e.getMessage(), Instant.now()));
    }

    /** Corpo ausente, JSON malformado ou sem Content-Type JSON: erro de quem chamou, não do servidor. */
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiError> unreadableBody(Exception e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_BODY", "Corpo da requisição inválido: envie JSON com o campo 'question'.",
                        Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("INTERNAL_ERROR", "Erro inesperado - veja os logs do servidor", Instant.now()));
    }
}
