package uk.gov.justice.hmpps.casenotes.sar

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import kotlin.collections.sortedByDescending

// TODO - Uncomment out this line and replace the one after with it
// class SubjectAccessRequest(private val noteRepository: NoteRepository) : HmppsPrisonSubjectAccessRequestService {
@Service
class SubjectAccessRequest(private val noteRepository: NoteRepository) {
  // TODO - Re-override this
  fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val notes = noteRepository.findSarContentForAllNotes(
      prn,
      fromDate?.atStartOfDay(),
      toDate?.plusDays(1)?.atStartOfDay(),
    ).map { it.toSarNoteV2() }.sortedByDescending { it.creationDateTime }
    return if (notes.isEmpty()) null else HmppsSubjectAccessRequestContent(content = notes)
  }

  // TODO - Delete this function
  fun getDpsOnlyPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val notes = noteRepository.findDpsOnlySarContent(
      prn,
      fromDate?.atStartOfDay(),
      toDate?.plusDays(1)?.atStartOfDay(),
    ).map { it.toSarNoteV2() }.sortedByDescending { it.creationDateTime }
    return if (notes.isEmpty()) null else HmppsSubjectAccessRequestContent(content = notes)
  }
}

private fun Note.toSarNoteV2() = SarNoteV2(
  createdAt,
  subType.type.description,
  subType.description,
  text,
  authorUsername,
  amendments().map { it.toSarAmendment() },
  locationId,
  occurredAt,
)

private fun Amendment.toSarAmendment() = SarAmendment(createdAt, text, authorUsername)
