package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.hmpps.casenotes.domain.Note

interface PagedRequest {
  @get:Parameter(description = "The page to request, starting at 1", example = "1")
  @get:Min(value = 1, message = "Page number must be at least 1")
  val page: Int

  @get:Parameter(description = "The page size to request", example = "10")
  @get:Min(value = 1, message = "Page size must be at least 1")
  val size: Int
  val sort: String

  fun validSortFields(): Set<String> = setOf(Note.OCCURRED_AT, Note.CREATED_AT)

  fun sort(): Sort {
    val validate: (String) -> String = {
      if (it in validSortFields()) {
        it
      } else {
        throw ValidationException("400 BAD_REQUEST Validation failure: Sort field invalid, please provide one of ${validSortFields()}")
      }
    }
    val split = sort.split(",")
    val (field, direction) = when (split.size) {
      0 -> Note.OCCURRED_AT to Sort.Direction.DESC
      1 -> validate(split[0]) to Sort.Direction.DESC
      else -> validate(split[0]) to if (split[1].lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
    }
    return buildSort(field, direction)
  }

  fun buildSort(field: String, direction: Sort.Direction): Sort = Sort.by(direction, field)

  fun pageable(): Pageable = PageRequest.of(page - 1, size, sort())
}

data class PageMeta(
  @Schema(description = "The total number of results across all pages", example = "1")
  val totalElements: Int,
  @Schema(description = "The current page number", example = "1")
  val page: Int,
  @Schema(description = "The maximum number of results per page", example = "10")
  val size: Int
)
