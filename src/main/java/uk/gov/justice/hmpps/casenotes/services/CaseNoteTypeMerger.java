package uk.gov.justice.hmpps.casenotes.services;

import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CaseNoteTypeMerger {

    public List<CaseNoteTypeDto> mergeAndSortList(final List<CaseNoteTypeDto> list1, final List<CaseNoteTypeDto> list2) {
        return merge(list1, list2)
                .stream()
                .map(t -> CaseNoteTypeDto.builder()
                            .code(t.getCode())
                            .description(t.getDescription())
                            .activeFlag(t.getActiveFlag())
                            .source(t.getSource())
                            .sensitive(t.isSensitive())
                            .subCodes(t.getSubCodes().stream().sorted().collect(Collectors.toList()))
                            .build())
                .sorted()
                .collect(Collectors.toList());
    }

    private List<CaseNoteTypeDto> merge(final List<CaseNoteTypeDto> list1, final List<CaseNoteTypeDto> list2) {

        final var map1 = list1 != null ? list1.stream().peek(cn -> {
            if (cn.getActiveFlag().equals("N")) {
                cn.getSubCodes().forEach(cns -> cns.setActiveFlag("N"));
            }
        }).collect(Collectors.toMap(CaseNoteTypeDto::getCode, cn -> cn)) : new HashMap<String, CaseNoteTypeDto>();

        final var map2 = list2 != null ? list2.stream().collect(Collectors.toMap(CaseNoteTypeDto::getCode, cn -> cn)) : new HashMap<String, CaseNoteTypeDto>();

        final var mergedMap = Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> CaseNoteTypeDto.builder()
                                .code(v1.getCode())
                                .description(v2.getDescription())
                                .activeFlag(v2.getActiveFlag())
                                .sensitive(v2.isSensitive())
                                .subCodes(merge(v1.getSubCodes(), v2.getSubCodes()))
                                .build())
                );

        return new ArrayList<>(mergedMap.values());
    }
}
