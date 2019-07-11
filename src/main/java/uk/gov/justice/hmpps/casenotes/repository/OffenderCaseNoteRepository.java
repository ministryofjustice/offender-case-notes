package uk.gov.justice.hmpps.casenotes.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;

@Repository
public interface OffenderCaseNoteRepository extends PagingAndSortingRepository<OffenderCaseNote, Long>, JpaSpecificationExecutor<OffenderCaseNote> {

}
