package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageRequest.DateType
import java.time.LocalDateTime

interface NoteUsageRequest {
  @get:NotEmpty(message = "At least one type is required")
  val typeSubTypes: Set<TypeSubTypeRequest>

  @get:JsonAlias("occurredFrom")
  val from: LocalDateTime?

  @get:JsonAlias("occurredTo")
  val to: LocalDateTime?
  val prisonCode: String?
  val dateType: DateType

  enum class DateType {
    CREATED_AT,
    OCCURRED_AT,
  }
}

data class NoteUsageResponse<T : UsageResponse>(val content: Map<String, List<T>>)

interface UsageResponse {
  val type: String
  val subType: String
  val count: Int
  val latestNote: LatestNote?
}

data class LatestNote(val occurredAt: LocalDateTime)

data class UsageByPersonIdentifierRequest(
  override val typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
  override val from: LocalDateTime? = null,
  override val to: LocalDateTime? = null,
  @field:NotEmpty(message = "At least one person identifier is required")
  val personIdentifiers: Set<String> = setOf(),
  val authorIds: Set<String> = setOf(),
  override val prisonCode: String? = null,
  override val dateType: DateType = DateType.OCCURRED_AT,
) : NoteUsageRequest

data class UsageByPersonIdentifierResponse(
  val personIdentifier: String,
  override val type: String,
  override val subType: String,
  override val count: Int,
  override val latestNote: LatestNote? = null,
) : UsageResponse

data class UsageByAuthorIdRequest(
  override val typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
  override val from: LocalDateTime? = null,
  override val to: LocalDateTime? = null,
  @field:NotEmpty(message = "At least one author id is required")
  val authorIds: Set<String> = setOf(),
  override val prisonCode: String? = null,
  override val dateType: DateType = DateType.OCCURRED_AT,
) : NoteUsageRequest

data class UsageByAuthorIdResponse(
  val authorId: String,
  override val type: String,
  override val subType: String,
  override val count: Int,
  override val latestNote: LatestNote? = null,
) : UsageResponse
