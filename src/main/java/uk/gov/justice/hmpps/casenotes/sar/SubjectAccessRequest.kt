package uk.gov.justice.hmpps.casenotes.sar

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import java.time.LocalDate

@Service
class SubjectAccessRequest(private val noteRepository: NoteRepository) {
  fun getSarContent(personIdentifier: String, fromDate: LocalDate?, toDate: LocalDate?): SubjectAccessResponse? {
    val notes = noteRepository.findSarContent(
      personIdentifier,
      fromDate?.atStartOfDay(),
      toDate?.plusDays(1)?.atStartOfDay(),
    ).asSequence().map { it.toSarNote() }.sortedByDescending { it.creationDateTime }.toList()
    return if (notes.isEmpty()) null else SubjectAccessResponse(personIdentifier, notes)
  }
}

private fun Note.toSarNote() = SarNote(
  createdAt,
  subType.type.description,
  subType.description,
  text,
  authorUsername,
  amendments().map { it.toSarAmendment() },
)

private fun Amendment.toSarAmendment() = SarAmendment(createdAt, text, authorUsername)
