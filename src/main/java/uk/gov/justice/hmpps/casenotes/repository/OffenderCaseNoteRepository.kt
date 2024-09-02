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
  fun findByCaseNoteTypeParentTypeTypeInAndModifyDateTimeAfterOrderByModifyDateTime(
    types: Set<String>?,
    createdDate: LocalDateTime?,
    page: Pageable?,
  ): List<OffenderCaseNote>

  @Modifying
  @Query(
    "update offender_case_note set offender_identifier = :new where offender_identifier = :old",
    nativeQuery = true,
  )
  fun updateOffenderIdentifier(old: String, new: String): Int

  @Modifying
  @Query("delete from offender_case_note ocn WHERE ocn.offender_identifier = :offenderIdentifier", nativeQuery = true)
  fun deleteOffenderCaseNoteByOffenderIdentifier(offenderIdentifier: String): Int

  @Modifying
  @Query(
    """
        delete from offender_case_note_amendment ocna where offender_case_note_id in 
            (select offender_case_note_id from offender_case_note where offender_identifier = :offenderIdentifier)
      """,
    nativeQuery = true,
  )
  fun deleteOffenderCaseNoteAmendmentsByOffenderIdentifier(offenderIdentifier: String): Int

  fun findByLegacyId(legacyId: Long): OffenderCaseNote?
}
