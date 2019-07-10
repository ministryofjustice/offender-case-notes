package uk.gov.justice.hmpps.casenotes.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;

import java.util.List;

@Repository
public interface OffenderCaseNoteRepository extends CrudRepository<OffenderCaseNote, Long> {

    List<OffenderCaseNote> findOffenderCaseNotesByOffenderIdentifier(String offenderIdentifier);
}
