package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Schema(description = "Case Note Filter")
@JsonInclude(JsonInclude.Include.NON_NULL)
class CaseNoteFilter(
  @Schema(description = "Filter by Case Note Type. Cannot be used in conjunction with typeSubTypes.", example = "KA")
  val type: String? = null,

  @Schema(
    description = "Filter by Case Note Sub Type. Must be used in conjunction with type, and cannot be used in conjunction with typeSubTypes.",
    example = "KS",
  )
  val subType: String? = null,

  @Schema(description = "Filter case notes from this date", example = "2017-10-31T01:30:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val startDate: LocalDateTime? = null,

  @Schema(description = "Filter case notes up to this date", example = "2019-05-31T01:30:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val endDate: LocalDateTime? = null,

  @Schema(description = "Filter by the location", example = "MDI")
  val locationId: String? = null,

  @Schema(description = "Filter by username", example = "USER1")
  val authorUsername: String? = null,

  @Schema(
    description = "Allow client credentials clients to filter out sensitive case notes. Defaults to true (sensitive case notes are included by default).",
    example = "false",
  )
  val includeSensitive: Boolean = true,
) {

  fun getTypesAndSubTypes(): Map<String, Set<String>> {
    if (!type.isNullOrEmpty()) {
      return if (subType.isNullOrEmpty()) mapOf(type to setOf()) else mapOf(type to setOf(subType))
    } else if (!subType.isNullOrEmpty()) {
      throw ValidationException("SubType must be used in conjunction with type.")
    }

    return emptyMap()
  }
}
