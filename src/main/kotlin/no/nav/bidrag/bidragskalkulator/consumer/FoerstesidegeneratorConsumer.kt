package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.FoerstesidegeneratorConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideBrukerDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.FoerstesideDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideResultatDto
import no.nav.bidrag.commons.web.client.AbstractRestClient
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
    private val restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag.foerstesidegenerator") {

    val genererFoerstesideUrl: URI by lazy {
        UriComponentsBuilder
            .fromUriString(foerstesidegeneratorConfigurationProperties.url)
            .path(foerstesidegeneratorConfigurationProperties.genererFoerstesidePath)
            .build()
            .toUri()
    }

    fun genererFoersteside(genererFoerstesideRequestDto: GenererFoerstesideRequestDto): ByteArrayOutputStream {
        val headers = HttpHeaders().apply {
            this.accept = listOf(APPLICATION_JSON)
            this.contentType = APPLICATION_JSON
        }

        headers.add("Nav-Consumer-Id", "bidragskalkulator")

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
                val response = postForNonNullEntity<GenererFoerstesideResultatDto>(genererFoerstesideUrl, payload, headers).let {
                    val b64string = it.foersteside
                    val decoded = Base64.getDecoder().decode(b64string)
                    StreamUtils.copy(decoded, outputStream)
                }
            } catch (httpException: HttpClientErrorException) {
                throw RuntimeException("Kunne ikke generere forstsideside: ${httpException.message}", httpException)
            } catch (e: Exception) {
                secureLogger.error("Feil ved generering av forstsideside: ${e.message}", e)
                throw RuntimeException("Kunne ikke generere forstsideside", e)
            }

        }
        return outputStream
    }


}