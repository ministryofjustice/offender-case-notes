package uk.gov.justice.hmpps.casenotes.types.internal

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy
import uk.gov.justice.hmpps.casenotes.utils.ROLE_ADD_SENSITIVE_CASE_NOTES
import uk.gov.justice.hmpps.casenotes.utils.ROLE_POM

@Service
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
