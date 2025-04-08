package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.web.client.HttpServerErrorException

@Service
class BidragPersonConsumer(
    @Value("\${BIDRAG_PERSON_URL}") bidragPersonUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-person") {
    private val hentFamilierelasjonUri =
        UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("motpartbarnrelasjon")
            .build()
            .toUri()


    fun hentFamilierelasjon(ident: String): MotpartBarnRelasjonDto? {
        return try {
            postForEntity<MotpartBarnRelasjonDto>(hentFamilierelasjonUri, PersonRequest(Personident(ident)))
        } catch (e: HttpServerErrorException) {
            if (e.statusCode.value() == 404) {
                secureLogger.info("Fant ikke person med ident $ident")
                null
            } else {
                secureLogger.error("Feil ved serverkall til bidrag-person for ident $ident", e)
                throw e
            }
        } catch (e: Exception) {
            secureLogger.error("Uventet feil ved kall til bidrag-person for ident $ident", e)
            throw e
        }
    }
}
