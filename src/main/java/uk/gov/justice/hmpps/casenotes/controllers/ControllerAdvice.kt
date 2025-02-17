package uk.gov.justice.hmpps.casenotes.controllers

import jakarta.persistence.EntityExistsException
import jakarta.validation.ValidationException
import lombok.extern.slf4j.Slf4j
import org.springframework.context.MessageSourceResolvable
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
import java.sql.BatchUpdateException

@RestControllerAdvice
@Slf4j
class ControllerAdvice {

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value()), developerMessage = e.message))

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> = ResponseEntity
    .status(e.statusCode)
    .body(e.responseBodyAsByteArray)

  @ExceptionHandler(EntityExistsException::class)
  fun handleEntityExistsException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.CONFLICT)
    .body(ErrorResponse(status = (HttpStatus.CONFLICT.value()), developerMessage = e.message))

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.NOT_FOUND)
    .body(ErrorResponse(status = (HttpStatus.NOT_FOUND.value()), developerMessage = e.message))

  @ExceptionHandler(
    MissingServletRequestParameterException::class,
    ValidationException::class,
    IllegalArgumentException::class,
    IllegalStateException::class,
  )
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(ErrorResponse(status = (BAD_REQUEST.value()), developerMessage = e.message))

  @ExceptionHandler(WebClientException::class, Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    val exceptionToReport = e.getNestedBatchException() ?: e
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = (HttpStatus.INTERNAL_SERVER_ERROR.value()),
          developerMessage = exceptionToReport.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  private fun List<MessageSourceResolvable>.mapErrors() = map { it.defaultMessage }.distinct().sorted().let {
    val validationFailure = "Validation failure"
    val message = if (it.size > 1) {
      """
              |${validationFailure}s: 
              |${it.joinToString(System.lineSeparator())}
              |
      """.trimMargin()
    } else {
      "$validationFailure: ${it.joinToString(System.lineSeparator())}"
    }
    ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = message,
          developerMessage = "400 BAD_REQUEST $message",
        ),
      )
  }

  fun Throwable.getNestedBatchException(): Throwable? = if (this is BatchUpdateException) cause else cause?.getNestedBatchException()
}
