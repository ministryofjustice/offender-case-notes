package uk.gov.justice.hmpps.casenotes.services;

import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseNoteTypeDtoMergerTest {

    private final CaseNoteTypeMerger merger = new CaseNoteTypeMerger();

    @Test
    public void testSimpleMerge() {

        final var list1 = List.of(
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("GEN", "General"), create("SP", "Special"))
                ).build(),
                createBuilder("KA", "Key worker").subCodes(
                        List.of(create("KS", "Session"), create("KE", "Entry"))
                ).build()
        );

        final var list2 = List.of(
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("NEW", "New Stuff"), create("GEN", "Different Gen"))
                ).build(),
                createBuilder("POM", "POM Stuff").subCodes(
                        List.of(create("SPC", "Special"), create("GEN", "General"))
                ).build()
        );

        List<CaseNoteTypeDto> resultantList = merger.mergeAndSortList(list1, list2);

        assertThat(resultantList).containsExactly(
                createBuilder("KA", "Key worker").subCodes(
                        List.of(create("KE", "Entry"), create("KS", "Session"))
                ).build(),
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("GEN", "Different Gen"), create("NEW", "New Stuff"), create("SP", "Special"))
                ).build(),
                createBuilder("POM", "POM Stuff").subCodes(
                        List.of(create("GEN", "General"), create("SPC", "Special"))
                ).build()

        );
    }

    @Test
    public void testActiveFlagUpdatedWhenParentInactiveDuringMerge() {

        final var list1 = List.of(
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("GEN", "General"), createInactive("SP", "Special"))
                ).build(),
                createBuilder("DRR", "Drug Rehabilitation Requirement").activeFlag("N").subCodes(
                        List.of(create("DCOUN", "Drug Counselling Session"), createInactive("DTEST", "Drug Test"))
                ).build()
        );

        final var list2 = List.of(
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("NEW", "New Stuff"), create("GEN", "Different Gen"))
                ).build(),
                createBuilder("POM", "POM Stuff").subCodes(
                        List.of(create("SPC", "Special"), create("GEN", "General"))
                ).build(),
                createBuilder("DRR", "Drug Rehabilitation Requirement").subCodes(
                        List.of(create("DCOUN", "Drug Counselling Session"))
                ).build()
        );

        List<CaseNoteTypeDto> resultantList = merger.mergeAndSortList(list1, list2);

        assertThat(resultantList).containsExactly(

                createBuilder("DRR", "Drug Rehabilitation Requirement").subCodes(
                        List.of(create("DCOUN", "Drug Counselling Session"), createInactive("DTEST", "Drug Test"))
                ).build(),
                createBuilder("OBS", "Observation").subCodes(
                        List.of(create("GEN", "Different Gen"), create("NEW", "New Stuff"), createInactive("SP", "Special"))
                ).build(),
                createBuilder("POM", "POM Stuff").subCodes(
                        List.of(create("GEN", "General"), create("SPC", "Special"))
                ).build()
        );
    }

    private CaseNoteTypeDto.CaseNoteTypeDtoBuilder createBuilder(final String obs, final String observation) {
        return CaseNoteTypeDto.builder().code(obs).description(observation);
    }

    private CaseNoteTypeDto createInactive(final String sp, final String special) {
        return createBuilder(sp, special).activeFlag("N").build();
    }

    private CaseNoteTypeDto create(final String gen, final String general) {
        return createBuilder(gen, general).build();
    }
}
