package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType
import java.util.Optional

@Repository
interface CaseNoteTypeRepository : JpaRepository<CaseNoteType, Long> {
  @Query(
    """
        select t from CaseNoteType t
        join fetch t.parentType pt
        where pt.type = :parentType and t.type = :type
    """,
  )
  fun findByParentTypeAndType(parentType: String, type: String): Optional<CaseNoteType>
}
