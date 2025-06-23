package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.FoerstesidegeneratorConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideAdresseDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideArkivsakDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideAvsenderDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideBrukerDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideDto
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayOutputStream
import java.net.URI


class FoerstesidegeneratorConsumer(
    private val foerstesidegeneratorConfigurationProperties: FoerstesidegeneratorConfigurationProperties,
    private val restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag.foerstesidegenerator") {

    val genererFoerstesideUrl: URI by lazy {
        UriComponentsBuilder
            .fromUriString(foerstesidegeneratorConfigurationProperties.url)
            .path(foerstesidegeneratorConfigurationProperties.genererFoerstesidePath)
            .build()
            .toUri()
    }

    fun genererFoersteside(): ByteArrayOutputStream {
        val headers = HttpHeaders()
        val outputStream = ByteArrayOutputStream()
        val payload = FoerstesideDto(
            spraakkode = "NB",
            adresse = FoerstesideAdresseDto(
                adresselinje1 = "Gateveien 1",
                adresselinje2 = "string",
                adresselinje3 = "string",
                postnummer = "1234",
                poststed = "Oslo"
            ),
            netsPostboks = "1234",
            avsender = FoerstesideAvsenderDto(
                avsenderId = "01234567890",
                avsenderNavn = "Per Hansen"
            ),
            bruker = FoerstesideBrukerDto(
                brukerId = "01234567890",
                brukerType = "PERSON"
            ),
            ukjentBrukerPersoninfo = "string",
            tema = "FOR",
            behandlingstema = "ab0001",
            arkivtittel = "Søknad om foreldrepenger ved fødsel",
            vedleggsliste = "[Terminbekreftelse, Dokumentasjon av inntekt]",
            navSkjemaId = "NAV 14.05-07",
            overskriftstittel = "Søknad om foreldrepenger ved fødsel - NAV 14.05-07",
            dokumentlisteFoersteside = "[Søknad om foreldrepenger ved fødsel, Terminbekreftelse, Dokumentasjon av inntekt]",
            foerstesidetype = "SKJEMA",
            enhetsnummer = "9999",
            arkivsak = FoerstesideArkivsakDto(
                arkivsaksystem = "GSAK",
                arkivsaksnummer = "abc123456"
            )
        )

        outputStream.use {
            try {
                val response = postForNonNullEntity<ByteArray>(genererFoerstesideUrl, payload, headers).let {
                    StreamUtils.copy(it, outputStream)
                }
            } catch (e: Exception) {
                secureLogger.error("Feil ved generering av forstsideside: ${e.message}", e)
                throw RuntimeException("Kunne ikke generere forstsideside", e)
            }

        }
        return outputStream
    }


}