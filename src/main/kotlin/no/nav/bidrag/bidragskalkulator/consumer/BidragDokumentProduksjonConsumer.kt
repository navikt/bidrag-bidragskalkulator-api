package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.DokumentproduksjonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.GenererPrivatAvtalePdfRequest
import no.nav.bidrag.commons.util.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.time.measureTimedValue

class BidragDokumentProduksjonConsumer(
    val properties: DokumentproduksjonConfigurationProperties,
    restTemplate: RestTemplate,
    private val headers: HttpHeaders
) : BaseConsumer(restTemplate, "bidrag.dokumentproduksjon") {

    val logger = LoggerFactory.getLogger(BidragDokumentProduksjonConsumer::class.java)

    init {
        check(properties.url.isNotEmpty()) { "bidrag.dokumentproduksjon.url mangler i konfigurasjon" }
        check(properties.genererPdfPath.isNotEmpty()) { "bidrag.dokumentproduksjon.genererPdfPath mangler i konfigurasjon" }
    }

    val produserPdfuri by lazy {
        UriComponentsBuilder.fromUriString(properties.url)
            .path(properties.genererPdfPath)
            .build()
            .toUri()
    }

    fun genererPrivatAvtaleAPdf(genererPrivatAvtalePdfRequest: GenererPrivatAvtalePdfRequest): ByteArrayOutputStream =
        medApplikasjonsKontekst {
            val outputStream = ByteArrayOutputStream()
            ByteArrayOutputStream().use {
                try {
                    val response = postForNonNullEntity<ByteArray>(produserPdfuri,
                        genererPrivatAvtalePdfRequest,
                        headers)
                    response.let { StreamUtils.copy(it, outputStream) }

                    val timed = measureTimedValue {
                        val response = postForNonNullEntity<ByteArray>(produserPdfuri, genererPrivatAvtalePdfRequest, headers)
                        StreamUtils.copy(response, outputStream)
                        outputStream
                    }

                    logger.info("Fullført kall til bidrag-dokument-produksjon på ${timed.duration.inWholeMilliseconds} ms")
                    timed.value
                } catch (e: IOException) {
                    secureLogger.error(e) { "Feil ved generering av privat avtale PDF: ${e.message}" }

                    throw RuntimeException("Kunne ikke generere privat avtale PDF", e)
                } catch (e: Exception) {
                    secureLogger.error(e) { "Feil ved generering av privat avtale PDF: ${e.message}" }

                    throw RuntimeException("Kunne ikke generere privat avtale PDF", e)
                }
            }

            outputStream
        }

}
