package uk.gov.justice.hmpps.casenotes.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import java.time.LocalDate

class SubjectAccessRequestServiceTest : IntegrationTest() {

  @Autowired
  lateinit var subjectAccessRequestService: SubjectAccessRequestService

  @Test
  fun `should fetch no records for invalid offender id`() {
    val offenderIdentifier = "invalid"
    val fromDate: LocalDate? = null
    val toDate: LocalDate? = null

    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)
    assertThat(caseNotes.size).isEqualTo(0)
  }

  @Test
  fun `should fetch no records for invalid offender id with to and form date`() {
    val offenderIdentifier = "invalid"
    val fromDate: LocalDate? = LocalDate.of(2019, 2, 27)
    val toDate: LocalDate? = LocalDate.of(2019, 1, 27)

    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)
    assertThat(caseNotes.size).isEqualTo(0)
  }

  @Test
  fun `should fetch all records for valid offender id, when no dates are provided`() {
    val offenderIdentifier = "L6962XX"
    val toDate: LocalDate? = null
    val fromDate: LocalDate? = null

    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)

    assertThat(caseNotes.size).isEqualTo(5)
    assertThat(caseNotes.map { it.creationDateTime.toString() }).containsExactlyInAnyOrder(
      "2019-08-29T17:08:46.655233",
      "2022-07-29T11:08:46.655233",
      "2023-07-29T17:08:46.655233",
      "2023-11-29T17:08:46.655233",
      "2023-12-30T17:08:46.655233",
    )
  }

  @Test
  fun `should fetch records for valid offender id with only start date`() {
    val offenderIdentifier = "L6962XX"

    val fromDate: LocalDate = LocalDate.of(2023, 1, 17)
    val toDate: LocalDate? = null
    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)

    assertThat(caseNotes.size).isEqualTo(3)
    assertThat(caseNotes.map { it.creationDateTime.toString() }).containsExactlyInAnyOrder(
      "2023-07-29T17:08:46.655233",
      "2023-11-29T17:08:46.655233",
      "2023-12-30T17:08:46.655233",
    )
  }

  @Test
  fun `should fetch records for valid offender id with only end date`() {
    val offenderIdentifier = "L6962XX"

    val fromDate: LocalDate? = null
    val toDate: LocalDate = LocalDate.of(2022, 7, 29)
    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)

    assertThat(caseNotes.size).isEqualTo(2)
    assertThat(caseNotes[0].creationDateTime).isEqualTo("2022-07-29T11:08:46.655233")
    assertThat(caseNotes[1].creationDateTime).isEqualTo("2019-08-29T17:08:46.655233")
  }

  @Test
  fun `should fetch all records for valid offender id, when with dates are provided`() {
    val offenderIdentifier = "L6962XX"

    val fromDate: LocalDate = LocalDate.of(2019, 1, 27)
    val toDate: LocalDate = LocalDate.of(2023, 1, 27)

    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)

    assertThat(caseNotes.size).isEqualTo(2)
    assertThat(caseNotes[0].creationDateTime).isEqualTo("2022-07-29T11:08:46.655233")
    assertThat(caseNotes[1].creationDateTime).isEqualTo("2019-08-29T17:08:46.655233")
  }

  @Test
  fun `should fetch amendments with in date range and it's parent case note, when no case notes were in requested range`() {
    val offenderIdentifier = "L6962XX"

    // There are no case notes in below date range but one amendment
    val fromDate: LocalDate = LocalDate.of(2024, 1, 22)
    val toDate: LocalDate = LocalDate.of(2024, 1, 29)

    val caseNotes = subjectAccessRequestService.getCaseNotes(offenderIdentifier, fromDate, toDate)

    assertThat(caseNotes.size).isEqualTo(1)
    assertThat(caseNotes[0].creationDateTime).isEqualTo("2023-07-29T17:08:46.655233")
    assertThat(caseNotes[0].text).isEqualTo("text33")
    assertThat(caseNotes[0].type).isEqualTo("POM")
    assertThat(caseNotes[0].subType).isEqualTo("GEN")
    assertThat(caseNotes[0].authorName).isEqualTo("John Smith")

    assertThat(caseNotes[0].amendments.size).isEqualTo(2)

    assertThat(caseNotes[0].amendments[0].creationDateTime).isEqualTo("2024-01-29T17:08:46.655233")
    assertThat(caseNotes[0].amendments[0].authorName).isEqualTo("John Smith")
    assertThat(caseNotes[0].amendments[0].additionalNoteText).isEqualTo("Note amendment2")

    assertThat(caseNotes[0].amendments[1].creationDateTime).isEqualTo("2023-12-29T17:08:46.655233")
    assertThat(caseNotes[0].amendments[1].authorName).isEqualTo("John Smith")
    assertThat(caseNotes[0].amendments[1].additionalNoteText).isEqualTo("Note amendment1")
  }
}
