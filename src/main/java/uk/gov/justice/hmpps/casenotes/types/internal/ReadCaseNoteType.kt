package uk.gov.justice.hmpps.casenotes.types.internal

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.services.ExternalApiService
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType

@Service
class ReadCaseNoteType(
  private val securityUserContext: SecurityUserContext,
  private val externalApiService: ExternalApiService,
  private val parentTypeRepository: ParentTypeRepository,
) {

  fun getCaseNoteTypes(): List<CaseNoteType> {
    val nomisTypes = externalApiService.getCaseNoteTypes().map { it.copy(subCodes = it.subCodes.sorted()) }
    val dpsTypes = parentTypeRepository.findAllWithParams(
      activeOnly = false,
      includeSensitive = canViewSensitiveTypes(),
      includeRestricted = true,
    ).map { it.toModel() }
    return (nomisTypes + dpsTypes).sorted()
  }

  fun getUserCaseNoteTypes(): List<CaseNoteType> {
    val nomisTypes = externalApiService.getUserCaseNoteTypes().map { it.copy(subCodes = it.subCodes.sorted()) }
    val dpsTypes = parentTypeRepository.findAllWithParams(
      activeOnly = true,
      includeSensitive = true,
      includeRestricted = canViewRestrictedTypes(),
    ).map { it.toModel() }
    return (nomisTypes + dpsTypes).sorted()
  }

  private fun canViewSensitiveTypes() =
    securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")

  private fun canViewRestrictedTypes() =
    securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
}
