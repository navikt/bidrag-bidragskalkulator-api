package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.SafSelvbetjeningConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.SafSelvbetjeningResponsDto
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.http.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class SafSelvbetjeningConsumer(
    private val properties: SafSelvbetjeningConfigurationProperties,
    private val restTemplate: RestTemplate,
) : BaseConsumer(restTemplate, "saf.selvbetjening") {

    init {
        check(properties.url.isNotEmpty()) { "saf.selvbetjening.url mangler i konfigurasjon" }
    }

    private val dokumentOversiktUrl by lazy {
        UriComponentsBuilder.fromUri(URI.create(properties.url))
            .path("/graphql")
            .build()
            .toUri()
    }

    private fun hentDokumentUrl(journalpostId: String, dokumentInfoId: String, variantFormat: String) =
        UriComponentsBuilder.fromUri(URI.create(properties.url))
            .path("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}")
            .buildAndExpand(journalpostId, dokumentInfoId, variantFormat)
            .toUri()

    fun hentDokument(journalpostId: String, dokumentInfoId: String, variantFormat: String): HentDokumentRespons {
        val url = hentDokumentUrl(journalpostId, dokumentInfoId, variantFormat)

        return try {
            val (respons, varighet) = measureTimedValue {
                // HEADERE: sett Accept eksplisitt (PDF/vedlegg kan komme i ulike formater)
                val headers = HttpHeaders().apply { accept = listOf(MediaType.ALL) }
                val entity = HttpEntity<Void>(headers)
                restTemplate.exchange(url, HttpMethod.GET, entity, ByteArray::class.java)
            }

            logger.info { "Kall til SAF selvbetjening for henting av dokument var vellykket (varighet_ms=${varighet.inWholeMilliseconds})." }

            val bytes = respons.body ?: ByteArray(0)
            val filnavn = respons.headers.contentDisposition?.filename

            HentDokumentRespons(dokument = bytes, filnavn = filnavn)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.NOT_FOUND -> {
                    logger.error{ "Dokument ble ikke funnet i SAF selvbetjening." }
                    secureLogger.error(e) { "Dokument ble ikke funnet i SAF selvbetjening: ${e.message}" }
                    throw NoContentException("Dokument ikke funnet i SAF selvbetjening")
                }
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
                    logger.error{ "Tilgang feilet ved henting av dokument fra SAF selvbetjening." }
                    secureLogger.error(e) { "Tilgang feilet ved henting av dokument fra SAF selvbetjening: ${e.message}" }
                    throw HttpClientErrorException(e.statusCode, "Tilgang feilet ved henting av dokument fra SAF selvbetjening")
                }
                else -> {
                    logger.error{ "Kall til SAF selvbetjening feilet ved henting av dokument." }
                    secureLogger.error(e) { "Kall til SAF selvbetjening feilet ved henting av dokument: ${e.message}" }
                    throw RuntimeException("Kunne ikke hente dokument", e)
                }
            }
        } catch (e: Exception) {
            logger.error{ "Uventet feil ved kall til SAF selvbetjening for henting av dokument." }
            secureLogger.error(e) { "Uventet feil ved kall til SAF selvbetjening for henting av dokument: ${e.message}" }
            throw RuntimeException("Kunne ikke hente dokument", e)
        }
    }

    fun hentDokumenterForIdent(
        ident: String,
        tema: List<String> = listOf("BID")
    ): SafSelvbetjeningResponsDto {
        val query = """
              {
                dokumentoversiktSelvbetjening(ident: "$ident", tema: [BID]) {
                  journalposter {
                    tema,
                    tittel,
                    datoSortering,
                    journalpostId,
                    mottaker {
                      navn,
                    }
                    avsender {
                      navn,
                    },
                    dokumenter {
                      dokumentInfoId,
                      tittel,
                      dokumentvarianter {
                        variantformat
                      }
                    }
                  },
                }
              }
              """.trimIndent()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val requestBody = GraphQLRequest(query,  mapOf())

        return try {
            val (resultat, varighet) = measureTimedValue {
                postForNonNullEntity<SafSelvbetjeningResponsDto>(dokumentOversiktUrl, requestBody, headers)
            }
            logger.info("Kall til SAF selvbetjening for dokumentoversikt var vellykket (varighet_ms=${varighet.inWholeMilliseconds}).")
            resultat
        } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
                    logger.error{ "Tilgang feilet ved henting av dokumentoversikt fra SAF selvbetjening." }
                    // Her logger vi MED exception for å bevare stacktrace i secure
                    secureLogger.error(e) { "Tilgang feilet ved henting av dokumentoversikt: ${e.message}" }
                    throw HttpClientErrorException(e.statusCode, "Tilgang feilet ved henting av dokumentoversikt")
                }
                else -> {
                    logger.error{ "Kall til SAF selvbetjening feilet ved henting av dokumentoversikt." }
                    secureLogger.error(e) { "Feil ved henting av dokumentoversikt: ${e.message}" }
                    throw RuntimeException("Kunne ikke hente dokumenter", e)
                }
            }
        } catch (e: Exception) {
            logger.error{ "Uventet feil ved henting av dokumentoversikt fra SAF selvbetjening." }
            secureLogger.error(e) { "Uventet feil ved henting av dokumentoversikt: ${e.message}" }
            throw RuntimeException("Kunne ikke hente dokumenter", e)
        }
    }
}

data class HentDokumentRespons(
    val dokument: ByteArray,
    val filnavn: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HentDokumentRespons
        if (!dokument.contentEquals(other.dokument)) return false
        if (filnavn != other.filnavn) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dokument.contentHashCode()
        result = 31 * result + (filnavn?.hashCode() ?: 0)
        return result
    }
}

data class GraphQLRequest(val query: String, val variables: Map<String, Any>? = null)
