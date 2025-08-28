package no.nav.bidrag.bidragskalkulator.utils

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import no.nav.bidrag.commons.security.service.OidcTokenManager
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class InnloggetBrukerUtils(private val oidcTokenManager: OidcTokenManager) {

    private companion object {
        private const val UNAUTHORIZED_TOKEN_MESSAGE = "Ugyldig token"
    }

    fun hentPåloggetPersonIdent(): String? {
        val token = oidcTokenManager.hentToken()
        return token?.let { hentPidFraToken(it) }
    }

    fun hentPåloggetPersonToken(): String? {
        return oidcTokenManager.hentToken()
    }

    /**
     * Ekstraherer 'pid' fra et JWT-token.
     * Kaster IllegalArgumentException hvis token er ugyldig eller mangler 'pid'-claim.
     */
    private fun hentPidFraToken(token: String): String {
        val jwt = JWTParser.parse(token) as? SignedJWT
            ?: throw IllegalArgumentException("Ugyldig JWT-format")
        return jwt.jwtClaimsSet.getStringClaim("pid")
            ?: throw IllegalStateException("Token mangler 'pid'-claim")
    }

     fun requirePåloggetPersonIdent(logger: Logger): String =
        hentPåloggetPersonIdent() ?: run {
            logger.warn("Avbrøt operasjon: ugyldig eller manglende token")
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig token")
        }
}
