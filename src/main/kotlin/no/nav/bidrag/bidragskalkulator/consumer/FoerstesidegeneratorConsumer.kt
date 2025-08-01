package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.FoerstesidegeneratorConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.*
import no.nav.bidrag.bidragskalkulator.exception.MetaforceException
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class FoerstesidegeneratorConsumer(
    private val config: FoerstesidegeneratorConfigurationProperties,
    restTemplate: RestTemplate,
    private val headers: HttpHeaders
) : AbstractRestClient(restTemplate, "bidrag.foerstesidegenerator") {

    val logger = LoggerFactory.getLogger(FoerstesidegeneratorConsumer::class.java)

    val genererFoerstesideUrl: URI by lazy {
        UriComponentsBuilder
            .fromUriString(config.url)
            .path(config.genererFoerstesidePath)
            .build()
            .toUri()
    }

    fun <T : Any> medApplikasjonsKontekst(fn: () -> T): T {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            fn()
        }
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
    fun genererFoersteside(dto: GenererFoerstesideRequestDto): GenererFoerstesideResultatDto =
        medApplikasjonsKontekst {
            val payload = FoerstesideDto(
                spraakkode = dto.spraakkode,
                netsPostboks = "1400",
                bruker = FoerstesideBrukerDto(
                    brukerId = dto.ident,
                    brukerType = "PERSON"
                ),
                tema = "BID",
                vedleggsliste = listOf("${dto.navSkjemaId.kode} ${dto.arkivtittel}"),
                dokumentlisteFoersteside = listOf("${dto.navSkjemaId.kode} ${dto.arkivtittel}"),
                arkivtittel = dto.arkivtittel,
                navSkjemaId = dto.navSkjemaId.kode,
                overskriftstittel = "${dto.navSkjemaId.kode} ${dto.arkivtittel}",
                foerstesidetype = Foerstesidetype.SKJEMA
            )

            try {
                postForEntity<GenererFoerstesideResultatDto>(genererFoerstesideUrl, payload, headers)
                    ?: throw RuntimeException("Generering av førsteside feilet: tom respons fra server")
            } catch (e: HttpClientErrorException) {
                logger.error("Feil fra foerstesidegenerator", e)
                throw RuntimeException("Generering av førsteside feilet: ${e.message}", e)
            } catch (e: Exception) {
                logger.error("Uventet feil ved generering av førsteside", e)
                if (e.message?.contains("Metaforce:GS_CreateDocument", ignoreCase = true) == true) {
                    throw MetaforceException("Metaforce feilet ved dokumentgenerering", e)
                }
                throw RuntimeException("Generering av førsteside feilet", e)
            }
        }
}
