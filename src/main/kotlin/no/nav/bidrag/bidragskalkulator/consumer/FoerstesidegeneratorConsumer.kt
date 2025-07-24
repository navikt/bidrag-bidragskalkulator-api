package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.FoerstesidegeneratorConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideBrukerDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideResultatDto
import no.nav.bidrag.bidragskalkulator.exception.MetaforceException
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.util.StreamUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64

class FoerstesidegeneratorConsumer(
    private val foerstesidegeneratorConfigurationProperties: FoerstesidegeneratorConfigurationProperties,
    restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag.foerstesidegenerator") {

    val logger = LoggerFactory.getLogger(FoerstesidegeneratorConsumer::class.java)

    val genererFoerstesideUrl: URI by lazy {
        UriComponentsBuilder
            .fromUriString(foerstesidegeneratorConfigurationProperties.url)
            .path(foerstesidegeneratorConfigurationProperties.genererFoerstesidePath)
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
     * @param genererFoerstesideRequestDto Informasjon om bruker og dokument som skal genereres.
     * @return ByteArrayOutputStream med generert førsteside i PDF-format.
     * @throws MetaforceException dersom ekstern dokumenttjeneste (Metaforce) feiler.
     */
    fun genererFoersteside(genererFoerstesideRequestDto: GenererFoerstesideRequestDto): ByteArrayOutputStream {
        val headers = HttpHeaders().apply {
            this.accept = listOf(APPLICATION_JSON)
            this.contentType = APPLICATION_JSON
        }

        headers.add("Nav-Consumer-Id", "bidrag-bidragskalkulator-api")

        val outputStream = ByteArrayOutputStream()
        val payload = FoerstesideDto(
            spraakkode = genererFoerstesideRequestDto.spraakkode,
            netsPostboks = "1400",
            bruker = FoerstesideBrukerDto(
                brukerId = genererFoerstesideRequestDto.ident,
                brukerType = "PERSON"
            ),
            tema = "BID",
            vedleggsliste = listOf(
                "${genererFoerstesideRequestDto.navSkjemaId.kode} ${genererFoerstesideRequestDto.arkivtittel}"
            ),
            dokumentlisteFoersteside = listOf(
                "${genererFoerstesideRequestDto.navSkjemaId.kode} ${genererFoerstesideRequestDto.arkivtittel}"
            ),
            arkivtittel = genererFoerstesideRequestDto.arkivtittel,
            navSkjemaId = genererFoerstesideRequestDto.navSkjemaId.kode,
            overskriftstittel = "${genererFoerstesideRequestDto.navSkjemaId.kode} ${genererFoerstesideRequestDto.arkivtittel}",
            foerstesidetype = "SKJEMA"
        )

        outputStream.use {
            try {
                postForNonNullEntity<GenererFoerstesideResultatDto>(genererFoerstesideUrl, payload, headers).let {
                    val b64string = it.foersteside
                    val decoded = Base64.getDecoder().decode(b64string)
                    StreamUtils.copy(decoded, outputStream)
                }
            } catch (httpException: HttpClientErrorException) {
                logger.error("Feil ved generering av førsteside", httpException)
                if (httpException.responseBodyAsString.contains("Metaforce:GS_CreateDocument", ignoreCase = true)) {
                    throw MetaforceException("Failed to generate document due to Metaforce service error.", httpException)
                }

                throw RuntimeException("Kunne ikke generere førsteside: ${httpException.message}", httpException)
            } catch (e: Exception) {
                logger.error("Feil ved generering av førsteside", e)
                throw RuntimeException("Kunne ikke generere førsteside", e)
            }

        }
        return outputStream
    }
}
