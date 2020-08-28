package uk.gov.justice.hmpps.casenotes.config

import lombok.Data
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.RegExUtils
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

@Slf4j
@Component
class SecurityUserContext {


  fun getAuthentication(): Authentication = SecurityContextHolder.getContext().authentication

  fun getCurrentUsername(): Optional<String> = getOptionalCurrentUser(this).map { it.username }

  fun getCurrentUser(): UserIdUser = getOptionalCurrentUser(this).orElseThrow { IllegalStateException("Current user not set but is required") }

  @Suppress("UNCHECKED_CAST")
  fun isOverrideRole(vararg overrideRoles: String?): Boolean {
    val roles: List<String> = if (overrideRoles.isNotEmpty()) overrideRoles.toList() as List<String> else listOf("SYSTEM_USER")
    return hasMatchingRole(roles, getAuthentication())
  }

  @Data
  class UserIdUser(val username: String, val userId: String)


  companion object {
    private fun hasMatchingRole(roles: List<String>, authentication: Authentication?): Boolean {
      return authentication != null &&
          authentication.authorities.stream()
              .anyMatch { a: GrantedAuthority? -> roles.contains(RegExUtils.replaceFirst(a!!.authority, "ROLE_", "")) }
    }

    fun getOptionalCurrentUser(securityUserContext: SecurityUserContext): Optional<UserIdUser> {
      val authentication = securityUserContext.getAuthentication()
      return if (authentication !is AuthAwareAuthenticationToken) Optional.empty() else Optional.of(authentication.userIdUser)
    }
  }
}
