package uk.gov.justice.hmpps.casenotes.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType

class CaseNoteTypeMergerTest {
  private val merger = CaseNoteTypeMerger()

  @Test
  fun testSimpleMerge() {
    val list1 = listOf(
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("GEN", "General"), create("SP", "Special"))
        ).build(),
        createBuilder("KA", "Key worker").subCodes(
            listOf(create("KS", "Session"), create("KE", "Entry"))
        ).build()
    )
    val list2 = listOf(
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("NEW", "New Stuff"), create("GEN", "Different Gen"))
        ).build(),
        createBuilder("POM", "POM Stuff").subCodes(
            listOf(create("SPC", "Special"), create("GEN", "General"))
        ).build()
    )
    val resultantList = merger.mergeAndSortList(list1, list2)
    Assertions.assertThat(resultantList).containsExactly(
        createBuilder("KA", "Key worker").subCodes(
            listOf(create("KE", "Entry"), create("KS", "Session"))
        ).build(),
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("GEN", "Different Gen"), create("NEW", "New Stuff"), create("SP", "Special"))
        ).build(),
        createBuilder("POM", "POM Stuff").subCodes(
            listOf(create("GEN", "General"), create("SPC", "Special"))
        ).build()
    )
  }

  @Test
  fun testActiveFlagUpdatedWhenParentInactiveDuringMerge() {
    val list1 = listOf(
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("GEN", "General"), createInactive("SP", "Special"))
        ).build(),
        createBuilder("DRR", "Drug Rehabilitation Requirement").activeFlag("N").subCodes(
            listOf(create("DCOUN", "Drug Counselling Session"), createInactive("DTEST", "Drug Test"))
        ).build()
    )
    val list2 = listOf(
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("NEW", "New Stuff"), create("GEN", "Different Gen"))
        ).build(),
        createBuilder("POM", "POM Stuff").subCodes(
            listOf(create("SPC", "Special"), create("GEN", "General"))
        ).build(),
        createBuilder("DRR", "Drug Rehabilitation Requirement").subCodes(
            listOf(create("DCOUN", "Drug Counselling Session"))
        ).build()
    )
    val resultantList = merger.mergeAndSortList(list1, list2)
    Assertions.assertThat(resultantList).containsExactly(
        createBuilder("DRR", "Drug Rehabilitation Requirement").subCodes(
            listOf(create("DCOUN", "Drug Counselling Session"), createInactive("DTEST", "Drug Test"))
        ).build(),
        createBuilder("OBS", "Observation").subCodes(
            listOf(create("GEN", "Different Gen"), create("NEW", "New Stuff"), createInactive("SP", "Special"))
        ).build(),
        createBuilder("POM", "POM Stuff").subCodes(
            listOf(create("GEN", "General"), create("SPC", "Special"))
        ).build()
    )
  }

  private fun createBuilder(obs: String, observation: String): CaseNoteType.CaseNoteTypeBuilder {
    return CaseNoteType.builder().code(obs).description(observation)
  }

  private fun createInactive(sp: String, special: String): CaseNoteType {
    return createBuilder(sp, special).activeFlag("N").build()
  }

  private fun create(gen: String, general: String): CaseNoteType {
    return createBuilder(gen, general).build()
  }
}
