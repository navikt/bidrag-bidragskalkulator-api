package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.DokumentproduksjonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.GenererPrivatAvtalePdfRequest
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class BidragDokumentProduksjonConsumer(
    val properties: DokumentproduksjonConfigurationProperties,
    restTemplate: RestTemplate,
    private val headers: HttpHeaders
) : BaseConsumer(restTemplate, "bidrag.dokumentproduksjon") {

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

    @Throws(IOException::class)
    fun genererPrivatAvtaleAPdf(request: GenererPrivatAvtalePdfRequest): ByteArrayOutputStream =
        medApplikasjonsKontekst {
            try {
                val (output, varighet) = measureTimedValue {
                    val bytes: ByteArray = postForNonNullEntity(produserPdfuri, request, headers)
                    ByteArrayOutputStream(bytes.size).apply { write(bytes) }
                }

                logger.info { "Kall til bidrag-dokument-produksjon OK (varighet_ms=${varighet.inWholeMilliseconds})" }
                output
            } catch (e: IOException) {
                logger.error{ "Kall til bidrag-dokument-produksjon feilet" }
                secureLogger.error(e) { "Kall til bidrag-dokument-produksjon feilet: ${e.message}" }

                throw RuntimeException("Kunne ikke generere privat avtale PDF", e)
            }
        }

}
