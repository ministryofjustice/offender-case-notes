package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.EntityExistsException
import jakarta.validation.Valid
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.CreateParentType
import uk.gov.justice.hmpps.casenotes.types.CreateSubType
import uk.gov.justice.hmpps.casenotes.types.UpdateParentType
import uk.gov.justice.hmpps.casenotes.types.UpdateSubType

@Validated
@Transactional
@Service
@PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
class WriteCaseNoteType(private val parentTypeRepository: ParentTypeRepository) {
  fun createParentType(@Valid createParentType: CreateParentType): CaseNoteType {
    if (parentTypeRepository.findByIdOrNull(createParentType.type) != null) {
      throw EntityExistsException(createParentType.type)
    }

    val pt = ParentType(createParentType.type, createParentType.description).apply { new = true }
    return parentTypeRepository.save(pt).toModel()
  }

  fun createSubType(parentTypeCode: String, @Valid createSubType: CreateSubType): CaseNoteType =
    parentTypeRepository.findByIdOrNull(parentTypeCode)?.apply {
      addSubType(createSubType)
    }?.toModel() ?: throw EntityNotFoundException(parentTypeCode)

  fun updateParentType(parentTypeCode: String, @Valid updateParentType: UpdateParentType): CaseNoteType =
    parentTypeRepository.findByIdOrNull(parentTypeCode)?.apply {
      description = updateParentType.description
    }?.toModel() ?: throw EntityNotFoundException(parentTypeCode)

  fun updateSubType(parentTypeCode: String, subTypeCode: String, @Valid updateSubType: UpdateSubType): CaseNoteType =
    parentTypeRepository.findByIdOrNull(parentTypeCode)?.apply {
      findSubType(subTypeCode)?.also {
        it.description = updateSubType.description
        it.active = updateSubType.active
        it.sensitive = updateSubType.sensitive
        it.restrictedUse = updateSubType.restrictedUse
      } ?: throw EntityNotFoundException("$parentTypeCode $subTypeCode")
    }?.toModel() ?: throw EntityNotFoundException(parentTypeCode)
}
