package uk.gov.justice.hmpps.casenotes.types.internal

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy
import uk.gov.justice.hmpps.casenotes.types.TypeInclude
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.INACTIVE
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.RESTRICTED
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.SENSITIVE
import uk.gov.justice.hmpps.casenotes.utils.ROLE_ADD_SENSITIVE_CASE_NOTES
import uk.gov.justice.hmpps.casenotes.utils.ROLE_POM
import uk.gov.justice.hmpps.casenotes.utils.ROLE_VIEW_SENSITIVE_CASE_NOTES

@Service
class ReadCaseNoteType(
  private val securityUserContext: SecurityUserContext,
  private val parentTypeRepository: ParentTypeRepository,
) {

  fun getCaseNoteTypes(selectableBy: SelectableBy, include: Set<TypeInclude>): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      activeOnly = INACTIVE !in include,
      includeSensitive = SENSITIVE in include || canViewSensitiveTypes(),
      includeRestricted = RESTRICTED in include || canViewRestrictedTypes(),
      dpsUserSelectableOnly = selectableBy == SelectableBy.DPS_USER,
    ).map { it.toModel() }.sorted()

  fun getUserCaseNoteTypes(): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      activeOnly = true,
      includeSensitive = true,
      includeRestricted = canViewRestrictedTypes(),
      dpsUserSelectableOnly = true,
    ).map { it.toModel() }.sorted()

  private fun canViewSensitiveTypes() =
    securityUserContext.hasAnyRole(ROLE_POM, ROLE_VIEW_SENSITIVE_CASE_NOTES, ROLE_ADD_SENSITIVE_CASE_NOTES)

  private fun canViewRestrictedTypes() =
    securityUserContext.hasAnyRole(ROLE_POM, ROLE_ADD_SENSITIVE_CASE_NOTES)
}
