package uk.gov.justice.hmpps.casenotes.types

import jakarta.persistence.EntityExistsException
import jakarta.validation.Valid
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException

const val SOURCE = "OCNS"

@Validated
@Service
class CaseNoteTypeService(private val parentTypeRepository: ParentTypeRepository) {
  @Transactional
  @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
  fun createParentType(@Valid createParentType: CreateParentType): CaseNoteType {
    if (parentTypeRepository.findByIdOrNull(createParentType.type) != null) {
      throw EntityExistsException(createParentType.type)
    }

    val pt = ParentType(createParentType.type, createParentType.description).apply { new = true }
    return parentTypeRepository.save(pt).toModel()
  }

  @Transactional
  @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
  fun createSubType(parentTypeCode: String, @Valid createSubType: CreateSubType): CaseNoteType =
    parentTypeRepository.findByIdOrNull(parentTypeCode)?.apply {
      addSubType(createSubType)
    }?.toModel() ?: throw EntityNotFoundException(parentTypeCode)
}

fun ParentType.toModel(): CaseNoteType =
  CaseNoteType(type, description, isActive().asActiveYn(), SOURCE, subCodes = getSubtypes().map(SubType::toModel))

fun SubType.toModel(): CaseNoteType = CaseNoteType(type, description, active.asActiveYn(), SOURCE, sensitive, restrictedUse)
