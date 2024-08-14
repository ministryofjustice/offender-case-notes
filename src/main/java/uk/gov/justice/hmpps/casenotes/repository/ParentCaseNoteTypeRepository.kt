package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType

@Repository
interface ParentCaseNoteTypeRepository : JpaRepository<ParentNoteType, String>
