package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.BidragPersonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class BidragPersonConsumer(
    val bidragPersonConfig: BidragPersonConfigurationProperties,
    restTemplate: RestTemplate
) : BaseConsumer(restTemplate, "bidrag.person") {

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
        logger.info { "Henter familie relasjon for person" }
        postSafely(hentFamilierelasjonUri, PersonRequest(Personident(ident)), Personident(ident))
    }

    fun hentPerson(ident: Personident): PersonDto = medApplikasjonsKontekst {
        logger.info { "Henter informasjon for person" }
        postSafely(hentPersonUri, PersonRequest(ident), ident)
    }

    private inline fun <reified T : Any> postSafely(uri: URI, request: Any, ident: Personident): T {
        return try {
            val (output, varighet) = measureTimedValue {
                postForNonNullEntity<T>(uri, request)
            }

            logger.info { "Kall til bidrag-person OK (varighet_ms=${varighet.inWholeMilliseconds})" }
            output
        } catch (e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    logger.warn { "Fant ikke person i bidrag-person" }
                    throw NoContentException("Fant ikke person i bidrag-person")
                }
                else -> {
                    logger.error{ "Kall til bidrag-person feilet" }
                    secureLogger.error(e) { "Kall til bidrag-person feilet: ${e.message}" }
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error{ "Uventet feil ved kall til bidrag-person" }
            secureLogger.error(e) { "Uventet feil ved kall til bidrag-person: ${e.message}" }
            throw e
        }
    }
}
