package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OffenderCaseNoteRepository : PagingAndSortingRepository<OffenderCaseNote, UUID>, JpaSpecificationExecutor<OffenderCaseNote> {

  fun findByCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(types: Set<String>?, createdDate: LocalDateTime?, page: Pageable?): List<OffenderCaseNote>

  fun findByModifyDateTimeBetweenOrderByModifyDateTime(fromDateTime: LocalDateTime, toDateTime: LocalDateTime): List<OffenderCaseNote>

  @Modifying
  @Query("UPDATE OFFENDER_CASE_NOTE ocn SET offender_identifier = ?2 WHERE ocn.offender_identifier = ?1", nativeQuery = true)
  fun updateOffenderIdentifier(oldOffenderIdentifier: String, newOffenderIdentifier: String): Int

  @Modifying
  @Query(value = "DELETE FROM OFFENDER_CASE_NOTE ocn WHERE ocn.offender_identifier = ?1", nativeQuery = true)
  fun deleteOffenderCaseNoteByOffenderIdentifier(offenderIdentifier: String): Int

  @Modifying
  @Query(value = "DELETE FROM offender_case_note_amendment ocna where offender_case_note_id in (select offender_case_note_id from offender_case_note where offender_identifier = ?1)", nativeQuery = true)
  fun deleteOffenderCaseNoteAmendmentsByOffenderIdentifier(offenderIdentifier: String): Int
}
