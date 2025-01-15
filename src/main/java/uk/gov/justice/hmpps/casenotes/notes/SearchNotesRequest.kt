package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.Parameter
import uk.gov.justice.hmpps.casenotes.domain.Note
import java.time.LocalDateTime

data class SearchNotesRequest(
  val includeSensitive: Boolean,
  val typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
  val occurredFrom: LocalDateTime? = null,
  val occurredTo: LocalDateTime? = null,
  override val page: Int,
  override val size: Int,
  @Parameter(description = "The sort to apply to the results", example = "occurredAt,desc")
  override val sort: String = "${Note.OCCURRED_AT},desc",
) : PagedRequest

data class TypeSubTypeRequest(val type: String, val subTypes: Set<String> = setOf())

data class SearchNotesResponse(
  val content: List<CaseNote>,
  val metadata: PageMeta,
  val hasCaseNotes: Boolean,
)
