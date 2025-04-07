package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.commons.security.utils.TokenUtils
import org.springframework.stereotype.Service

@Service
class TokenInfoService {
    fun getLoggedInUserInfo(): String? {
        return try {
            // Extract the logged-in user's identity from the token
            val user = TokenUtils.hentBruker()
            user
        } catch (e: Exception) {
            null // Return null or handle the exception as needed
        }
    }
}