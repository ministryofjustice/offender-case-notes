package uk.gov.justice.hmpps.casenotes.types

import io.swagger.v3.oas.annotations.media.Schema

data class CaseNoteType(
  @Schema(required = true, description = "Case Note Code", example = "OBSERVE")
  val code: String,

  @Schema(required = true, description = "Case Note description.", example = "Observations")
  val description: String,

  @Schema(required = true, description = "Active indicator flag.", example = "Y", allowableValues = ["Y,N"])
  val activeFlag: ActiveYn,

  @Schema(description = "Indicates the type of note is sensitive", example = "true")
  val sensitive: Boolean,

  @Schema(
    description = "Indicates the type of note can only be created by a sub-set of users (e.g. POMs)",
    example = "true",
  )
  val restrictedUse: Boolean,

  @Schema(description = "Shows the actors that can select this case note type")
  val selectableBy: List<SelectableBy>,

  @Schema(description = "List of case note sub types")
  val subCodes: List<CaseNoteType> = listOf(),
) : Comparable<CaseNoteType> {
  override fun compareTo(other: CaseNoteType): Int {
    val dif = description.compareTo(other.description, ignoreCase = true)
    return if (dif == 0) code.compareTo(other.code) else dif
  }
}

enum class ActiveYn {
  Y,
  N,
}

fun Boolean.asActiveYn() = if (this) ActiveYn.Y else ActiveYn.N
