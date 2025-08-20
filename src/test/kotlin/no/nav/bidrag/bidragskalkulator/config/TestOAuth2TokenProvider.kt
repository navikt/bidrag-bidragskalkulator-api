package no.nav.bidrag.bidragskalkulator.config

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("test")
class TestOAuth2TokenProvider(private val mockOAuth2Server: MockOAuth2Server) {

    private val clientId = "aud-localhost"

    @Bean
    fun gyldigOAuth2Token(): String {
        val claims = mapOf(
            "acr" to "Level4",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to påloggetPerson.ident.toString(),
        )

        return mockOAuth2Server.issueToken(
            SecurityConstants.TOKENX,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = SecurityConstants.TOKENX,
                subject = påloggetPerson.ident.toString(),
                typeHeader = JOSEObjectType.JWT.type,
                audience = listOf(clientId),
                claims = claims,
                expiry = 3600
            )
        ).serialize()
    }

    @Bean
    fun ugyldigOAuth2Token(): String =  gyldigOAuth2Token() + "x"

    companion object TestData {
        val påloggetPerson = PersonDto(
            ident = Personident("03848797048"),
            navn = "LIGGESTOL, EKSAKT",
            fornavn = "Eksakt",
            mellomnavn = "",
            etternavn = "LIGGESTOL",
            kjønn = Kjønn.KVINNE,
            fødselsdato = LocalDate.parse("1987-04-03"),
            visningsnavn = "Eksakt Liggestol")
    }
}
