package uk.gov.justice.hmpps.casenotes.sync

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AuthorTest {

  @ParameterizedTest
  @MethodSource("authors")
  fun `author full name is correctly formatted`(author: Author, fullName: String) {
    assertThat(author.fullName()).isEqualTo(fullName)
  }

  companion object {
    private val DEFAULT = Author("username", "1234567", "JOHN", "SMITH")

    @JvmStatic
    fun authors() = listOf(
      Arguments.of(DEFAULT, "John Smith"),
      Arguments.of(
        DEFAULT.copy(firstName = "DOUBLE-BARRELLED", lastName = "NAME-WITH-HYPHEN"),
        "Double-Barrelled Name-With-Hyphen",
      ),
      Arguments.of(
        DEFAULT.copy(firstName = "o'leary", lastName = "O'NEIL"),
        "O'Leary O'Neil",
      ),
      Arguments.of(
        DEFAULT.copy(firstName = "Andrés", lastName = "CEPEDA"),
        "Andrés Cepeda",
      ),
      Arguments.of(
        DEFAULT.copy(firstName = "zoë", lastName = "FRANÇOIS"),
        "Zoë François",
      ),
    )
  }
}
