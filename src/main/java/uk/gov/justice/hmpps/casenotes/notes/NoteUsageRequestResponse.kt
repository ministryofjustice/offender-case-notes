package uk.gov.justice.hmpps.casenotes.notes

import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime
import java.util.UUID

interface NoteUsageRequest {
  @get:NotEmpty(message = "At least one type is required")
  val typeSubTypes: Set<TypeSubTypeRequest>
  val occurredFrom: LocalDateTime?
  val occurredTo: LocalDateTime?
}

data class NoteUsageResponse<T : UsageResponse>(val content: Map<String, List<T>>)

interface UsageResponse {
  val type: String
  val subType: String
  val count: Int
  val latestNote: LatestNote?
}

data class LatestNote(val id: UUID, val occurredAt: LocalDateTime)

data class UsageByPersonIdentifierRequest(
  override val typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
  override val occurredFrom: LocalDateTime? = null,
  override val occurredTo: LocalDateTime? = null,
  @field:NotEmpty(message = "At least one person identifier is required")
  val personIdentifiers: Set<String> = setOf(),
  val authorIds: Set<String> = setOf(),
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
  override val occurredFrom: LocalDateTime? = null,
  override val occurredTo: LocalDateTime? = null,
  @field:NotEmpty(message = "At least one author id is required")
  val authorIds: Set<String> = setOf(),
) : NoteUsageRequest

data class UsageByAuthorIdResponse(
  val authorId: String,
  override val type: String,
  override val subType: String,
  override val count: Int,
  override val latestNote: LatestNote? = null,
) : UsageResponse