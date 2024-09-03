package uk.gov.justice.hmpps.casenotes.types

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.domain.ParentTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.Type
import uk.gov.justice.hmpps.casenotes.utils.ROLE_ADD_SENSITIVE_CASE_NOTES
import uk.gov.justice.hmpps.casenotes.utils.ROLE_POM

@Service
@Transactional(readOnly = true)
class ReadCaseNoteType(
  private val securityUserContext: SecurityUserContext,
  private val parentTypeRepository: ParentTypeRepository,
) {

  fun getCaseNoteTypes(
    selectableBy: SelectableBy,
    includeInactive: Boolean,
    includeRestricted: Boolean,
  ): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      includeInactive = includeInactive,
      includeRestricted = includeRestricted,
      dpsUserSelectableOnly = selectableBy == SelectableBy.DPS_USER,
    ).map { it.toModel() }.sorted()

  fun getUserCaseNoteTypes(): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      includeInactive = false,
      includeRestricted = canViewRestrictedTypes(),
      dpsUserSelectableOnly = true,
    ).map { it.toModel() }.sorted()

  private fun canViewRestrictedTypes() =
    securityUserContext.hasAnyRole(ROLE_POM, ROLE_ADD_SENSITIVE_CASE_NOTES)
}

private fun Type.toModel(): CaseNoteType =
  CaseNoteType(
    code,
    description,
    subCodes = getSubtypes().map { it.toModel() }.sorted(),
  )

private fun SubType.toModel(): CaseNoteSubType =
  CaseNoteSubType(
    code,
    description,
    active,
    sensitive,
    restrictedUse,
    if (dpsUserSelectable) listOf(SelectableBy.DPS_USER) else listOf(),
  )
