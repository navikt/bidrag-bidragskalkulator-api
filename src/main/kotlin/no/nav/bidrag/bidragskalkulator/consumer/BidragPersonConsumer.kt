package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.BidragPersonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class BidragPersonConsumer(
    val bidragPersonConfig: BidragPersonConfigurationProperties,
    restTemplate: RestTemplate
) : BaseConsumer(restTemplate, "bidrag.person") {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        check(bidragPersonConfig.url.isNotEmpty()) { "bidrag.person.url mangler i konfigurasjon" }
        check(bidragPersonConfig.hentMotpartbarnrelasjonPath.isNotEmpty()) { "bidrag.person.hentMotpartbarnrelasjonPath mangler i konfigurasjon" }
        check(bidragPersonConfig.hentPersoninformasjonPath.isNotEmpty()) { "bidrag.person.hentPersoninformasjonPath mangler i konfigurasjon" }
    }

    private val hentFamilierelasjonUri by lazy { UriComponentsBuilder
        .fromUri(URI.create(bidragPersonConfig.url))
        .pathSegment(bidragPersonConfig.hentMotpartbarnrelasjonPath)
        .build()
        .toUri()
    }

    private val hentPersonUri by lazy { UriComponentsBuilder
        .fromUri(URI.create(bidragPersonConfig.url))
        .pathSegment(bidragPersonConfig.hentPersoninformasjonPath)
        .build()
        .toUri()
    }

    fun hentFamilierelasjon(ident: String): MotpartBarnRelasjonDto = medApplikasjonsKontekst {
            postSafely(hentFamilierelasjonUri, PersonRequest(Personident(ident)), Personident(ident))
    }

    fun hentPerson(ident: Personident): PersonDto = medApplikasjonsKontekst {
        logger.info ( "Henter informasjon for person" )
        postSafely(hentPersonUri, PersonRequest(ident), ident)
    }

    private inline fun <reified T : Any> postSafely(uri: URI, request: Any, ident: Personident): T {
        return try {
            postForNonNullEntity<T>(uri, request)
        } catch (e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    logger.warn ("Fant ikke person i bidrag-person")
                    throw NoContentException("Fant ikke person i bidrag-person")
                }
                else -> {
                    logger.error("Serverfeil fra bidrag-person", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Uventet feil ved kall til bidrag-person", e)
            throw e
        }
    }
}
