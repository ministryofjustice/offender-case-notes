package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime
import java.util.*


@Repository
interface OffenderCaseNoteRepository : PagingAndSortingRepository<OffenderCaseNote, UUID>, JpaSpecificationExecutor<OffenderCaseNote> {
  fun findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(types: Set<String>?, createdDate: LocalDateTime?, page: Pageable?): List<OffenderCaseNote>
  fun findByModifyDateTimeBetweenOrderByModifyDateTime(fromDateTime: LocalDateTime, toDateTime: LocalDateTime): List<OffenderCaseNote>

  @Modifying
  @Query("UPDATE OffenderCaseNote ocn SET ocn.offenderIdentifier = :newOffenderIdentifier WHERE ocn.offenderIdentifier = :oldOffenderIdentifier")
  fun updateOffenderIdentifier(@Param("oldOffenderIdentifier") oldOffenderIdentifier: String?, @Param("newOffenderIdentifier") newOffenderIdentifier: String?): Int
}
