package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.BidragPersonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service("bidragPersonConsumer")
class BidragPersonConsumer(
    val bidragPersonConfig: BidragPersonConfigurationProperties,
    @Qualifier("azure") restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag.person") {

    init {
        check(bidragPersonConfig.url.isNotEmpty()) { "bidrag-person url mangler i konfigurasjon" }
        check(bidragPersonConfig.hentMotpartbarnrelasjonPath.isNotEmpty()) { "bidrag.person.hentMotpartbarnrelasjonPath mangler i konfigurasjon" }
        check(bidragPersonConfig.hentPersoninformasjonPath.isNotEmpty()) { "hentPersoninformasjonPath mangler i konfigurasjon" }
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


    fun <T : Any> medApplikasjonsKontekst(fn: () -> T): T {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            fn()
        }
    }

    fun hentFamilierelasjon(ident: String): MotpartBarnRelasjonDto = medApplikasjonsKontekst {
            secureLogger.info("Henter familierelasjon for person $ident")
            postSafely(hentFamilierelasjonUri, PersonRequest(Personident(ident)), Personident(ident))
        }


    fun hentPerson(ident: Personident): PersonDto = medApplikasjonsKontekst{
        secureLogger.info("Henter informasjon for person $ident")
        postSafely(hentPersonUri, PersonRequest(ident), ident)
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