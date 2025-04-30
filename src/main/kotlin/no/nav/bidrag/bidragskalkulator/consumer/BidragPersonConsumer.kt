package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
class BidragPersonConsumer(
    @Value("\${BIDRAG_PERSON_URL}") bidragPersonUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag-person") {

    private val hentFamilierelasjonUri = URI.create("$bidragPersonUrl/motpartbarnrelasjon")
    private val hentNavnFoedselDoedUri = URI.create("$bidragPersonUrl/navnfoedseldoed")


    fun hentFamilierelasjon(ident: String): MotpartBarnRelasjonDto {
        secureLogger.info("Henter familierelasjon for person $ident")
        return postSafely(hentFamilierelasjonUri, PersonRequest(Personident(ident)), Personident(ident))
    }

    fun hentNavnFoedselDoed(ident: Personident): NavnFødselDødDto {
        secureLogger.info("Henter navn, fødselsdata og eventuell død for person $ident")
        return postSafely(hentNavnFoedselDoedUri, PersonRequest(ident), ident)
    }

    private inline fun <reified T : Any> postSafely(uri: URI, request: Any, ident: Personident): T {
        return try {
            postForNonNullEntity<T>(uri, request)
        } catch (e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    secureLogger.warn("Fant ikke person med ident $ident")
                    throw NoContentException("Fant ikke person med ident $ident")
                }
                else -> {
                    secureLogger.error("Serverfeil fra bidrag-person for ident $ident", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            secureLogger.error("Uventet feil ved kall til bidrag-person for ident $ident", e)
            throw e
        }
    }
}