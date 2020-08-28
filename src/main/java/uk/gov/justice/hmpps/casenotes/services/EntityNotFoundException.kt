package uk.gov.justice.hmpps.casenotes.services

import java.util.function.Supplier

class EntityNotFoundException(message: String?) : RuntimeException(message), Supplier<EntityNotFoundException> {
  override fun get(): EntityNotFoundException = EntityNotFoundException(message)

  @Synchronized
  override fun fillInStackTrace(): Throwable = this

  companion object {
    private const val DEFAULT_MESSAGE_FOR_ID_FORMAT = "Resource with id [%s] not found."
    fun withId(id: Long): EntityNotFoundException = withId(id.toString())

    @JvmStatic
    fun withId(id: String?): EntityNotFoundException = EntityNotFoundException(String.format(DEFAULT_MESSAGE_FOR_ID_FORMAT, id))

    fun withMessage(message: String?): EntityNotFoundException = EntityNotFoundException(message)

    fun withMessage(message: String?, vararg args: Any?): EntityNotFoundException = EntityNotFoundException(String.format(message!!, *args))

  }
}
