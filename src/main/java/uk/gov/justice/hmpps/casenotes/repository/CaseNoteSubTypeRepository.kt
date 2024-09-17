package uk.gov.justice.hmpps.casenotes.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.hmpps.casenotes.model.CaseNoteSubType
import java.util.Optional

@Repository
interface CaseNoteSubTypeRepository : JpaRepository<CaseNoteSubType, Long> {
  @Query(
    """
        select st from CaseNoteSubType st
        join fetch st.type t
        where t.code = :parentTypeCode and st.code = :typeCode
    """,
  )
  fun findByParentTypeAndType(parentTypeCode: String, typeCode: String): Optional<CaseNoteSubType>
}
