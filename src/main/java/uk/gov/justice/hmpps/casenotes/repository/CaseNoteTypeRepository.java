package uk.gov.justice.hmpps.casenotes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;

@Repository
public interface CaseNoteTypeRepository extends JpaRepository<SensitiveCaseNoteType, Long> {

    SensitiveCaseNoteType findSensitiveCaseNoteTypeByParentType_TypeAndType(String parentType, String type);
}
