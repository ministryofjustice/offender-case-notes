package uk.gov.justice.hmpps.casenotes.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime


@Schema(description = "Case Note Filter")
@JsonInclude(JsonInclude.Include.NON_NULL)
class CaseNoteFilter(
  @Schema(description = "Filter by Case Note Type", example = "KA")
  val type: String? = null,

  @Schema(description = "Filter by Case Note Sub Type", example = "KS")
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

  @Schema(description = "Filter by a list of case note types and optional case not sub types separated by plus", example = "KA+KE,OBS,POM+GEN")
  val caseNoteTypeSubTypes:String? = null,
)
