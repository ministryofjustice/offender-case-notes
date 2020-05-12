package uk.gov.justice.hmpps.casenotes.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse;
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException;

import javax.persistence.EntityExistsException;
import javax.validation.ValidationException;


@RestControllerAdvice(
        basePackages = {"uk.gov.justice.hmpps.casenotes.controllers"}
)
@Slf4j
public class ControllerAdvice {

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<byte[]> handleWebClientResponseException(final WebClientResponseException e) {
        if (e.getStatusCode().is4xxClientError()) {
            log.info("Unexpected client exception", e);
        } else {
            log.error("Unexpected server exception", e);
        }
        return ResponseEntity
                .status(e.getRawStatusCode())
                .body(e.getResponseBodyAsByteArray());
    }

    @ExceptionHandler(WebClientException.class)
    public ResponseEntity<ErrorResponse> handleWebClientException(final WebClientException e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(final AccessDeniedException e) {
        log.debug("Forbidden (403) returned", e);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final ValidationException e) {
        log.debug("Bad Request (400) returned", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final MissingServletRequestParameterException e) {
        log.debug("Bad Request (400) returned", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(final Exception e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityExistsException(final Exception e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.CONFLICT.value())
                        .developerMessage(e.getMessage())
                        .build());
    }

}
