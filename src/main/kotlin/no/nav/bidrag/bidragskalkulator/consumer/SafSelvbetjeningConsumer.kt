package no.nav.bidrag.bidragskalkulator.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.config.SafSelvbetjeningConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.SafSelvbetjeningResponsDto
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.apache.hc.core5.http.HttpException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


class SafSelvbetjeningConsumer(
    properties: SafSelvbetjeningConfigurationProperties,
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "saf.selvbetjening") {

    private val url = UriComponentsBuilder.fromUri(URI.create(properties.graphqlUrl))
        .path("/graphql")
        .build()
        .toUri()

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
            return postForNonNullEntity<SafSelvbetjeningResponsDto>(url, requestBody, headers)
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

data class GraphQLRequest(val query: String, val variables: Map<String, Any>? = null)
