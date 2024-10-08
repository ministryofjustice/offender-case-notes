package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.domain.RefreshRepository
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.util.UUID

@Repository
interface OffenderCaseNoteRepository :
  JpaSpecificationExecutor<OffenderCaseNote>,
  JpaRepository<OffenderCaseNote, UUID>,
  RefreshRepository<OffenderCaseNote, UUID> {
  @Modifying
  @Query("update OffenderCaseNote set personIdentifier = :new where personIdentifier = :old")
  fun updateOffenderIdentifier(old: String, new: String): Int
}
