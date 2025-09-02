package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.FørstesidegeneratorConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.*
import no.nav.bidrag.bidragskalkulator.exception.MetaforceException
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class FørstesidegeneratorConsumer(
    private val config: FørstesidegeneratorConfigurationProperties,
    restTemplate: RestTemplate,
    private val headers: HttpHeaders
) : BaseConsumer(restTemplate, "foerstesidegenerator") {

    init {
        check(config.url.isNotEmpty()) { "foerstesidegenerator.url mangler i konfigurasjon" }
        check(config.genererFoerstesidePath.isNotEmpty()) { "foerstesidegenerator.genererFoerstesidePath mangler i konfigurasjon" }
    }

    val genererFørstesideUrl: URI by lazy {
        UriComponentsBuilder
            .fromUriString(config.url)
            .path(config.genererFoerstesidePath)
            .build()
            .toUri()
    }

    /**
     * Genererer førsteside for en privat avtale ved å kalle ekstern tjeneste.
     *
     * Denne funksjonen bygger opp en FoerstesideDto og sender den til foerstesidegenerator-tjenesten,
     * som videre kaller Metaforce for å generere en PDF-basert førsteside.
     *
     * Hva er Metaforce?
     * ------------------
     * Metaforce er en ekstern dokumenttjeneste som brukes for å generere dokumenter (typisk PDF).
     * I Nav brukes Metaforce ofte som underliggende system i tjenester som produserer skjema,
     * brev eller forsider til innsendinger.
     *
     * Når denne funksjonen kalles:
     * - Den bygger en forespørsel (`FoerstesideDto`) med nødvendige metadata.
     * - Sender kallet til `foerstesidegenerator`, som internt kaller Metaforce.
     * - Dersom Metaforce feiler, kastes en spesifikk `MetaforceException` og funksjonen returnerer HTTP 502 (Bad Gateway).
     *
     * Dette gjør det enklere å skille mellom feil i vår applikasjon og feil hos ekstern dokumentgenerator.
     *
     * @param GenererFørstesideRequestDto Informasjon om bruker og dokument som skal genereres.
     * @return ByteArrayOutputStream med generert førsteside i PDF-format.
     * @throws MetaforceException dersom ekstern dokumenttjeneste (Metaforce) feiler.
     */
    fun genererFørsteside(dto: GenererFørstesideRequestDto): GenererFørstesideResultatDto =
        medApplikasjonsKontekst {
            try {
                val (førstesideResultatDto, varighet) = measureTimedValue {
                    postForEntity<GenererFørstesideResultatDto>(genererFørstesideUrl, dto, headers)
                        ?: throw RuntimeException("Tom respons fra førstesidegenerator")
                }

                logger.info { "Kall til førstesidegenerator OK (varighet_ms=${varighet.inWholeMilliseconds})" }
                førstesideResultatDto
            } catch (e: HttpClientErrorException) {
                logger.error{ "Kall til foerstesidegenerator feilet (status=${e.statusCode.value()})" }
                secureLogger.warn(e) {
                    "Feil fra foerstesidegenerator: status=${e.statusCode.value()}, body='${e.responseBodyAsString.take(4000)}'"
                }

                throw RuntimeException("Generering av førsteside feilet (klientfeil ${e.statusCode.value()})", e)
            } catch (e: Exception) {
                logger.error{ "Uventet feil ved kall til foerstesidegenerator" }
                secureLogger.error(e) { "Uventet feil ved kall til foerstesidegenerator: ${e.message}" }

                if (e.message?.contains("Metaforce:GS_CreateDocument", ignoreCase = true) == true) {
                    throw MetaforceException("Metaforce feilet ved dokumentgenerering", e)
                }

                throw RuntimeException("Generering av førsteside feilet", e)
            }
        }
}
