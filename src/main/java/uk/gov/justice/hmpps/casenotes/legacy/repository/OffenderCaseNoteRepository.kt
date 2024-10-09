package uk.gov.justice.hmpps.casenotes.legacy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.domain.RefreshRepository
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNote
import java.util.UUID

@Repository
interface OffenderCaseNoteRepository :
  JpaSpecificationExecutor<OffenderCaseNote>,
  JpaRepository<OffenderCaseNote, UUID>,
  RefreshRepository<OffenderCaseNote, UUID>
