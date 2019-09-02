package uk.gov.justice.hmpps.casenotes.services;

import org.junit.Test;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseNoteTypeMergerTest {

    private final CaseNoteTypeMerger merger = new CaseNoteTypeMerger();

    @Test
    public void testSimpleMerge() {

        final var list1 = List.of(
                CaseNoteType.builder().code("OBS").description("Observation").subCodes(
                        List.of(CaseNoteType.builder().code("GEN").description("General").build(), CaseNoteType.builder().code("SP").description("Special").build())
                ).build(),
                CaseNoteType.builder().code("KA").description("Key worker").subCodes(
                        List.of(CaseNoteType.builder().code("KS").description("Session").build(), CaseNoteType.builder().code("KE").description("Entry").build())
                ).build()
        );

        final var list2 = List.of(
                CaseNoteType.builder().code("OBS").description("Observation").subCodes(
                        List.of(CaseNoteType.builder().code("NEW").description("New Stuff").build(), CaseNoteType.builder().code("GEN").description("Different Gen").build())
                ).build(),
                CaseNoteType.builder().code("POM").description("POM Stuff").subCodes(
                        List.of(CaseNoteType.builder().code("SPC").description("Special").build(), CaseNoteType.builder().code("GEN").description("General").build())
                ).build()
        );

        List<CaseNoteType> resultantList = merger.mergeAndSortList(list1, list2);

        assertThat(resultantList).hasSize(3);

        assertThat(resultantList).containsExactly(
                List.of(
                        CaseNoteType.builder().code("KA").description("Key worker").subCodes(
                                List.of(CaseNoteType.builder().code("KE").description("Entry").build(), CaseNoteType.builder().code("KS").description("Session").build())
                        ).build(),
                        CaseNoteType.builder().code("OBS").description("Observation").subCodes(
                                List.of(CaseNoteType.builder().code("GEN").description("Different Gen").build(), CaseNoteType.builder().code("NEW").description("New Stuff").build(), CaseNoteType.builder().code("SP").description("Special").build())
                        ).build(),
                        CaseNoteType.builder().code("POM").description("POM Stuff").subCodes(
                                List.of(CaseNoteType.builder().code("GEN").description("General").build(), CaseNoteType.builder().code("SPC").description("Special").build())
                        ).build()
                ).toArray(new CaseNoteType[0])
        );


    }
}
