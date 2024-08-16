package uk.gov.justice.hmpps.casenotes.types.internal

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType

@Service
class ReadCaseNoteType(
  private val securityUserContext: SecurityUserContext,
  private val parentTypeRepository: ParentTypeRepository,
) {

  fun getCaseNoteTypes(): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      activeOnly = false,
      includeSensitive = canViewSensitiveTypes(),
      includeRestricted = true,
      dpsUserSelectableOnly = false,
    ).map { it.toModel() }.sorted()

  fun getUserCaseNoteTypes(): List<CaseNoteType> =
    parentTypeRepository.findAllWithParams(
      activeOnly = true,
      includeSensitive = true,
      includeRestricted = canViewRestrictedTypes(),
      dpsUserSelectableOnly = true,
    ).map { it.toModel() }.sorted()

  private fun canViewSensitiveTypes() =
    securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")

  private fun canViewRestrictedTypes() =
    securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
}
