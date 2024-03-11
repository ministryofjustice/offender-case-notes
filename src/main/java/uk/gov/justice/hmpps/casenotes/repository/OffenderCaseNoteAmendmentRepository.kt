package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment

@Repository
interface OffenderCaseNoteAmendmentRepository : JpaSpecificationExecutor<OffenderCaseNoteAmendment>, JpaRepository<OffenderCaseNoteAmendment, Long>
