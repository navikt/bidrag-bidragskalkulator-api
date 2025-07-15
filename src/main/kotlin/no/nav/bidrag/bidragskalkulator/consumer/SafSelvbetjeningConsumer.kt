package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.SafSelvbetjeningConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.SafSelvbetjeningResponsDto
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


class SafSelvbetjeningConsumer(
    private val properties: SafSelvbetjeningConfigurationProperties,
    private val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "saf.selvbetjening") {

    private val dokumentOversiktUrl = UriComponentsBuilder.fromUri(URI.create(properties.url))
        .path("/graphql")
        .build()
        .toUri()

    private fun genererHentDokumentUrl(journalpostId: String, dokumentInfoId: String, variantFormat: String) =
        UriComponentsBuilder.fromUri(URI.create(properties.url))
            .path("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}")
            .buildAndExpand(journalpostId, dokumentInfoId, variantFormat)
            .toUri()

    fun hentDokument(journalpostId: String, dokumentInfoId: String, variantFormat: String): HentDokumentRespons {
        val url = genererHentDokumentUrl(journalpostId, dokumentInfoId, variantFormat)

        try {
            val respons = restTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)
            val filnavn = respons.headers.contentDisposition.filename
            return HentDokumentRespons(
                dokument = respons.body ?: ByteArray(0),
                filnavn = filnavn
            )

        } catch (e: HttpClientErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    secureLogger.warn { "Tilgang feilet ved henting av dokument: ${e.message}" }
                    throw HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Tilgang feilet ved henting av dokument")
                }
                else -> {
                    secureLogger.warn { "Feil ved henting av dokument: ${e.message}" }
                    throw RuntimeException("Kunne ikke hente dokument", e)
                }
            }
        } catch (e: Exception) {
            secureLogger.error { "Uventet feil ved henting av dokument: ${e.message}" }
            throw RuntimeException("Kunne ikke hente dokument", e)
        }
    }

    fun hentDokumenterForIdent(
        ident: String, tema: List<String> = listOf("BID")
    ): SafSelvbetjeningResponsDto? {

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
        }

        val requestBody = GraphQLRequest(query,  mapOf())

        try {
            return postForNonNullEntity<SafSelvbetjeningResponsDto>(dokumentOversiktUrl, requestBody, headers)
        } catch (e: HttpClientErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    secureLogger.error { "Tilgang feilet ved henting av dokumenter: ${e.message}" }
                    throw HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Tilgang feilet ved henting av dokumenter")
                }
                else -> {
                    secureLogger.error {"Feil ved henting av dokumenter for bruker: ${e.message}" }
                    throw RuntimeException("Kunne ikke hente dokumenter", e)
                }
            }
        } catch (e: Exception) {
            secureLogger.error { "Uventet feil ved henting av dokumenter for bruker: ${e.message}" }
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
