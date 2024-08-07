package uk.gov.justice.hmpps.casenotes.config

import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.RegExUtils
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Slf4j
@Component
class SecurityUserContext {

  fun getAuthentication(): Authentication = SecurityContextHolder.getContext().authentication

  fun getCurrentUsername(): String? = getAuthorisedUser()?.username

  fun getCurrentUser(): UserIdUser = getAuthorisedUser()
    ?: throw IllegalStateException("Current user not set but is required")

  fun isOverrideRole(vararg overrideRoles: String): Boolean =
    hasMatchingRole(if (overrideRoles.isEmpty()) listOf("SYSTEM_USER") else overrideRoles.toList())

  private fun getAuthorisedUser(): UserIdUser? =
    when (val auth = getAuthentication()) {
      is AuthAwareAuthenticationToken -> auth.userIdUser
      else -> null
    }

  private fun hasMatchingRole(roles: List<String>): Boolean =
    getAuthentication().authorities?.any { a: GrantedAuthority? ->
      roles.contains(RegExUtils.replaceFirst(a!!.authority, "ROLE_", ""))
    } == true

  data class UserIdUser(val username: String, val userId: String)
}
