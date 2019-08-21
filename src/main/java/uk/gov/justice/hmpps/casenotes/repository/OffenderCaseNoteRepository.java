package uk.gov.justice.hmpps.casenotes.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;

import java.util.List;
import java.util.UUID;

@Repository
public interface OffenderCaseNoteRepository extends PagingAndSortingRepository<OffenderCaseNote, UUID>, JpaSpecificationExecutor<OffenderCaseNote> {

    List<OffenderCaseNote> findAllByOffenderIdentifier(String offenderIdentifier);

}
