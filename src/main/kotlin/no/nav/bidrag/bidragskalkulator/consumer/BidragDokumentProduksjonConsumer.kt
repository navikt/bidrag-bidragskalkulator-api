package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.DokumentproduksjonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException

class BidragDokumentProduksjonConsumer(
    val properties: DokumentproduksjonConfigurationProperties,
    val restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag.dokumentproduksjon") {

    val produserPdfuri by lazy {
        UriComponentsBuilder.fromUriString(properties.url)
            .path(properties.genererPdfPath)
            .build()
            .toUri()
    }

    fun genererPrivatAvtaleAPdf(privatAvtaleDto: PrivatAvtalePdfDto): ByteArrayOutputStream {

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_PDF)
            contentType = MediaType.APPLICATION_JSON
        }

        val outputStream = ByteArrayOutputStream()
        outputStream.use {
            try {
                val response = postForNonNullEntity<ByteArray>(produserPdfuri, privatAvtaleDto, headers)
                response.let { StreamUtils.copy(it, outputStream) }
            } catch (e: IOException) {
                secureLogger.error("Feil ved generering av privat avtale PDF: ${e.message}", e)
                throw RuntimeException("Kunne ikke generere privat avtale PDF", e)
            } catch (e: Exception) {
                secureLogger.error("Feil ved generering av privat avtale PDF: ${e.message}", e)
                throw RuntimeException("Kunne ikke generere privat avtale PDF", e)
            }
        }

        return outputStream
    }
}

