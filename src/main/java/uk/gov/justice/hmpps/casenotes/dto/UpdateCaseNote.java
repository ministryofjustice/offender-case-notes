package uk.gov.justice.hmpps.casenotes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


@AllArgsConstructor
@EqualsAndHashCode
@Data
public class UpdateCaseNote {
    private final String text;
}
