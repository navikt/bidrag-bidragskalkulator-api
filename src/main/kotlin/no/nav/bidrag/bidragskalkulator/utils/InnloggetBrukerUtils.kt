package no.nav.bidrag.bidragskalkulator.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import no.nav.bidrag.commons.security.service.OidcTokenManager
import org.springframework.stereotype.Component

@Component
class InnloggetBrukerUtils(private val oidcTokenManager: OidcTokenManager) {

    fun hentPåloggetPersonIdent(): String {
        val token = hentToken() ?: error("Fant ikke token")

        return hentPidFraToken(token)
    }

    private fun hentToken(): String? =
        try {
            oidcTokenManager.hentToken()
        } catch (e: Exception) {
            null
        }

    private fun hentPidFraToken(token: String): String {
        val jwt = JWTParser.parse(token) as? SignedJWT
            ?: throw IllegalArgumentException("Ugyldig JWT-format")
        return jwt.jwtClaimsSet.getStringClaim("pid")
            ?: throw IllegalStateException("Token mangler 'pid'-claim")
    }
}