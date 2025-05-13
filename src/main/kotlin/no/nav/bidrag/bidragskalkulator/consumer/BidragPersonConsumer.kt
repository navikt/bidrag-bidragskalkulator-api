package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.apache.logging.log4j.LogManager.getLogger
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

    private val logger = getLogger(BidragPersonConsumer::class.java)

    private val hentFamilierelasjonUri = URI.create("$bidragPersonUrl/motpartbarnrelasjon")
    private val hentPersonUri = URI.create("$bidragPersonUrl/informasjon")

    fun <T : Any> medApplikasjonsKontekst(fn: () -> T): T {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            fn()
        }
    }

    fun hentFamilierelasjon(ident: String): MotpartBarnRelasjonDto = medApplikasjonsKontekst {
            logger.info("Henter familierelasjon for person")
            postSafely(hentFamilierelasjonUri, PersonRequest(Personident(ident)), Personident(ident))
        }


    fun hentPerson(ident: Personident): PersonDto = medApplikasjonsKontekst{
        logger.info("Henter informasjon for person")
        postSafely(hentPersonUri, PersonRequest(ident), ident)
    }

    private inline fun <reified T : Any> postSafely(uri: URI, request: Any, ident: Personident): T {
        return try {
            postForNonNullEntity<T>(uri, request)
        } catch (e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    logger.warn("Fant ikke person med ident")
                    throw NoContentException("Fant ikke person med ident")
                }
                else -> {
                    logger.error("Serverfeil fra bidrag-person for ident med uri $uri", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved kall til bidrag-person med uri $uri", e)
            throw e
        }
    }
}