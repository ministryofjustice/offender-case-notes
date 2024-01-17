package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType

@Repository
interface CaseNoteTypeRepository : JpaRepository<CaseNoteType?, Long?> {
  @Suppress("FunctionName")
  fun findCaseNoteTypeByParentType_TypeAndType(parentType: String?, type: String?): CaseNoteType?
}
