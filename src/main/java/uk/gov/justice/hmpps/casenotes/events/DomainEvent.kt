package uk.gov.justice.hmpps.casenotes.events

import uk.gov.justice.hmpps.casenotes.config.Source
import java.time.ZonedDateTime
import java.util.UUID

data class DomainEvent(
  val occurredAt: ZonedDateTime,
  val eventType: String,
  val detailUrl: String,
  val description: String,
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference,
  val version: Int = 1,
)

data class AdditionalInformation(
  val id: UUID,
  val legacyId: Long,
  val type: String,
  val subType: String,
  val source: Source,
  val syncToNomis: Boolean,
  val systemGenerated: Boolean,
)

data class PersonReference(val identifiers: Set<Identifier> = setOf()) {
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
  fun findNomsNumber() = get(NOMS_NUMBER_TYPE)

  companion object {
    private const val NOMS_NUMBER_TYPE = "NOMS"
    fun withIdentifier(prisonNumber: String) = PersonReference(setOf(Identifier(NOMS_NUMBER_TYPE, prisonNumber)))
  }

  data class Identifier(val type: String, val value: String)
}
