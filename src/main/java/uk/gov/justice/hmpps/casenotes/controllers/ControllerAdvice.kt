package uk.gov.justice.hmpps.casenotes.controllers

import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import javax.persistence.EntityExistsException
import javax.validation.ValidationException

@RestControllerAdvice(basePackages = ["uk.gov.justice.hmpps.casenotes.controllers"])
@Slf4j
class ControllerAdvice {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    if (e.statusCode.is4xxClientError) {
      log.info("Unexpected client exception with message {}", e.message)
    } else {
      log.error("Unexpected server exception", e)
    }
    return ResponseEntity
      .status(e.rawStatusCode)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(WebClientException::class)
  fun handleWebClientException(e: WebClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = (HttpStatus.INTERNAL_SERVER_ERROR.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value())))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned with message {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = (HttpStatus.BAD_REQUEST.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = (HttpStatus.BAD_REQUEST.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = (HttpStatus.INTERNAL_SERVER_ERROR.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = (HttpStatus.NOT_FOUND.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(EntityExistsException::class)
  fun handleEntityExistsException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorResponse(status = (HttpStatus.CONFLICT.value()), developerMessage = (e.message)))
  }
}
