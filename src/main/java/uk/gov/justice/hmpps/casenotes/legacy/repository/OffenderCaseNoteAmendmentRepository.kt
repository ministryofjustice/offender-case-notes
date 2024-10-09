package uk.gov.justice.hmpps.casenotes.legacy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNoteAmendment
import java.util.UUID

@Repository
interface OffenderCaseNoteAmendmentRepository : JpaSpecificationExecutor<OffenderCaseNoteAmendment>, JpaRepository<OffenderCaseNoteAmendment, UUID>
