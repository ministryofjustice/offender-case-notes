package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType

@Repository
interface CaseNoteTypeRepository : JpaRepository<SensitiveCaseNoteType?, Long?> {
  fun findSensitiveCaseNoteTypeByParentType_TypeAndType(parentType: String?, type: String?): SensitiveCaseNoteType?
}
