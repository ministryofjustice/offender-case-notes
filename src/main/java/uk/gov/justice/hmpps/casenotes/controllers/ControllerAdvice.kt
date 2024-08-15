package uk.gov.justice.hmpps.casenotes.controllers

import jakarta.persistence.EntityExistsException
import jakarta.validation.ValidationException
import lombok.extern.slf4j.Slf4j
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

@RestControllerAdvice(basePackages = ["uk.gov.justice.hmpps.casenotes.controllers", "uk.gov.justice.hmpps.casenotes.types"])
@Slf4j
class ControllerAdvice {

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value())))
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    return ResponseEntity
      .status(e.statusCode)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(EntityExistsException::class)
  fun handleEntityExistsException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorResponse(status = (HttpStatus.CONFLICT.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = (HttpStatus.NOT_FOUND.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(MissingServletRequestParameterException::class, ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = (HttpStatus.BAD_REQUEST.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(WebClientException::class, Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = (HttpStatus.INTERNAL_SERVER_ERROR.value()), developerMessage = (e.message)))
  }
}
