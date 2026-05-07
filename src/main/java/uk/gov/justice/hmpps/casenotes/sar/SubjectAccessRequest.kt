package uk.gov.justice.hmpps.casenotes.sar

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequest(private val noteRepository: NoteRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val notes = noteRepository.findSarContent(
      prn,
      fromDate?.atStartOfDay(),
      toDate?.plusDays(1)?.atStartOfDay(),
    ).map { it.toSarNote() }.sortedByDescending { it.creationDateTime }
    return if (notes.isEmpty()) null else HmppsSubjectAccessRequestContent(content = notes)
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
