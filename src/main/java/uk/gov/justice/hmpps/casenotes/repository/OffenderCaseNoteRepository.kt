package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OffenderCaseNoteRepository :
  JpaSpecificationExecutor<OffenderCaseNote>,
  JpaRepository<OffenderCaseNote, UUID> {

  @Modifying
  @Query(
    "update case_note set person_identifier = :new where person_identifier = :old",
    nativeQuery = true,
  )
  fun updateOffenderIdentifier(old: String, new: String): Int

  @Modifying
  @Query("delete from case_note ocn WHERE ocn.person_identifier = :personIdentifier", nativeQuery = true)
  fun deleteCaseNoteByPersonIdentifier(personIdentifier: String): Int

  @Modifying
  @Query(
    """
        delete from case_note_amendment ocna where case_note_id in 
            (select case_note_id from case_note where person_identifier = :personIdentifier)
      """,
    nativeQuery = true,
  )
  fun deleteCaseNoteAmendmentsByPersonIdentifier(personIdentifier: String): Int
}
