package uk.gov.justice.hmpps.casenotes.types

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.ParentTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.Type

@Service
@Transactional(readOnly = true)
class ReadCaseNoteType(
  private val parentTypeRepository: ParentTypeRepository,
) {

  fun getCaseNoteTypes(
    selectableBy: SelectableBy,
    includeInactive: Boolean,
    includeRestricted: Boolean,
  ): List<CaseNoteType> = parentTypeRepository.findAllWithParams(
    includeInactive = includeInactive,
    includeRestricted = includeRestricted,
    dpsUserSelectableOnly = selectableBy == SelectableBy.DPS_USER,
  ).map { it.toModel() }.sorted()
}

private fun Type.toModel(): CaseNoteType = CaseNoteType(
  code,
  description,
  subCodes = getSubtypes().map { it.toModel() }.sorted(),
)

private fun SubType.toModel(): CaseNoteSubType = CaseNoteSubType(
  code,
  description,
  active,
  sensitive,
  restrictedUse,
  if (dpsUserSelectable) listOf(SelectableBy.DPS_USER) else listOf(),
)
