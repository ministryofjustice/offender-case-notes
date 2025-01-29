package uk.gov.justice.hmpps.casenotes.legacy.service

import java.util.function.Supplier

class EntityNotFoundException(message: String?) :
  RuntimeException(message),
  Supplier<EntityNotFoundException> {
  override fun get(): EntityNotFoundException = EntityNotFoundException(message)

  @Synchronized
  override fun fillInStackTrace(): Throwable = this

  companion object {
    private const val DEFAULT_MESSAGE_FOR_ID_FORMAT = "Resource with id [%s] not found."

    @JvmStatic
    fun withId(id: String?): EntityNotFoundException = EntityNotFoundException(
      String.format(
        DEFAULT_MESSAGE_FOR_ID_FORMAT,
        id,
      ),
    )
  }
}
