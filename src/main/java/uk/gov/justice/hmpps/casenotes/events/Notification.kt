package uk.gov.justice.hmpps.casenotes.events

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.hmpps.casenotes.events.MessageAttributes.Companion.EVENT_TYPE

data class Notification(
  @JsonProperty("Message") val message: String,
  @JsonProperty("MessageAttributes") val attributes: MessageAttributes = MessageAttributes(),
) {
  val eventType: String? @JsonIgnore get() = attributes[EVENT_TYPE]?.value
}

data class MessageAttributes(
  @JsonAnyGetter @JsonAnySetter
  private val attributes: MutableMap<String, MessageAttribute> = mutableMapOf(),
) : MutableMap<String, MessageAttribute> by attributes {
  constructor(eventType: String) : this(mutableMapOf(EVENT_TYPE to MessageAttribute("String", eventType)))

  override operator fun get(key: String): MessageAttribute? = attributes[key]
  operator fun set(key: String, value: MessageAttribute) {
    attributes[key] = value
  }

  companion object {
    const val EVENT_TYPE = "eventType"
  }
}

data class MessageAttribute(@JsonProperty("Type") val type: String, @JsonProperty("Value") val value: String)
