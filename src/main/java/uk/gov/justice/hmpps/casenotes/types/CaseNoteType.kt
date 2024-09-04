package uk.gov.justice.hmpps.casenotes.types

import io.swagger.v3.oas.annotations.media.Schema

interface CodedValue : Comparable<CodedValue> {
  val code: String
  val description: String
  override fun compareTo(other: CodedValue): Int {
    val dif = description.compareTo(other.description, ignoreCase = true)
    return if (dif == 0) code.compareTo(other.code) else dif
  }
}

data class CaseNoteType(
  override val code: String,
  override val description: String,
  @Schema(description = "List of case note sub types")
  val subCodes: List<CaseNoteSubType> = listOf(),
) : CodedValue

data class CaseNoteSubType(
  @Schema(required = true, description = "Case Note Code", example = "OBSERVE")
  override val code: String,

  @Schema(required = true, description = "Case Note description.", example = "Observations")
  override val description: String,

  @Schema(required = true, description = "Indicates if the type is active or not")
  val active: Boolean,

  @Schema(description = "Indicates the type of note is sensitive", example = "true")
  val sensitive: Boolean,

  @Schema(
    description = "Indicates the type of note can only be created by a sub-set of users (e.g. POMs)",
    example = "true",
  )
  val restrictedUse: Boolean,

  @Schema(description = "Shows the actors that can select this case note type")
  val selectableBy: List<SelectableBy>,
) : CodedValue {

  @Deprecated("to be replaced with 'active' boolean")
  val activeFlag: ActiveYn = if (active) ActiveYn.Y else ActiveYn.N
}

enum class ActiveYn {
  Y,
  N,
}
