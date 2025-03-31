package no.nav.bidrag.bidragskalkulator.config

import com.nimbusds.jose.JOSEObjectType
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestOAuth2TokenProvider(private val mockOAuth2Server: MockOAuth2Server) {

    private val clientId = "aud-localhost"
    private val personident = "12345678910"

    @Bean
    fun validOAuth2Token(): String {
        val claims = mapOf(
            "acr" to "Level4",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to personident,
        )

        return mockOAuth2Server.issueToken(
            SecurityConstants.TOKENX,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = SecurityConstants.TOKENX,
                subject = personident,
                typeHeader = JOSEObjectType.JWT.type,
                audience = listOf(clientId),
                claims = claims,
                expiry = 3600
            )
        ).serialize()
    }
}