package uk.gov.justice.hmpps.casenotes.utils

import kotlin.reflect.KProperty0

// Only to be used in tests to help instantiate entities
fun <T : Any, V : Any?> T.set(field: KProperty0<V>, value: V): T {
  return setByName(field.name, value)
}

fun <T : Any, V : Any?> T.setByName(field: String, value: V): T {
  val f = this::class.java.getDeclaredField(field)
  f.isAccessible = true
  f[this] = value
  f.isAccessible = false
  return this
}
