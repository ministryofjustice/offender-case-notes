package uk.gov.justice.hmpps.casenotes.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.UserIdUser

class AuthAwareTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {
  override fun convert(jwt: Jwt): AbstractAuthenticationToken =
      AuthAwareAuthenticationToken(jwt, jwt.claims["user_id"] as String, extractAuthorities(jwt))

  @Suppress("UNCHECKED_CAST")
  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> =
      (jwt.claims.getOrDefault("authorities", listOf<String>()) as Collection<String>)
          .map(::SimpleGrantedAuthority).toSet()
}

class AuthAwareAuthenticationToken(jwt: Jwt, userId: String, authorities: Collection<GrantedAuthority>)
  : JwtAuthenticationToken(jwt, authorities) {

  val userIdUser: UserIdUser = UserIdUser(jwt.subject, userId)
}
