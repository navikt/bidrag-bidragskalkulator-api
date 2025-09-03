package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.GrunnlagConfigurationProperties
import no.nav.bidrag.bidragskalkulator.exception.GrunnlagNotFoundException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.transport.behandling.grunnlag.request.GrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

class BidragGrunnlagConsumer(
    val grunnlagConfig: GrunnlagConfigurationProperties,
    restTemplate: RestTemplate,
) : BaseConsumer(restTemplate, "bidrag.grunnlag") {

    companion object {
        /**
         * Hvor mange år tilbake i tid inntektsgrunnlag skal hentes
         */
        const val HENT_INNTEKT_ANTALl_ÅR_DEFAULT = 2
    }

    init {
        check(grunnlagConfig.url.isNotEmpty()) { "grunnlag url mangler i konfigurasjon" }
        check(grunnlagConfig.hentGrunnlagPath.isNotEmpty()) { "hentGrunnlagPath mangler i konfigurasjon" }
    }

    private val grunnlagUri by lazy { UriComponentsBuilder
        .fromUri(URI.create(grunnlagConfig.url))
        .pathSegment(grunnlagConfig.hentGrunnlagPath)
        .build()
        .toUri()
    }

    fun hentGrunnlag(ident: String, antallAar: Int = HENT_INNTEKT_ANTALl_ÅR_DEFAULT): HentGrunnlagDto =
        medApplikasjonsKontekst {
        try {
            val (output, varighet) = measureTimedValue {
                postForNonNullEntity<HentGrunnlagDto>(grunnlagUri,
                    HentGrunnlagRequestDto(
                        Formål.FORSKUDD,
                        listOf(GrunnlagRequestDto(
                            type = GrunnlagRequestType.AINNTEKT,
                            personId = ident,
                            periodeFra = LocalDate.now().minusYears(antallAar.toLong()),
                            periodeTil = LocalDate.now()
                        ))
                    )
                )
            }

            logger.info { "Kall til bidrag-grunnlag OK (varighet_ms=${varighet.inWholeMilliseconds})" }
            output
        } catch(e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    logger.warn { "Fant ikke grunnlag for person i bidrag-grunnlag" }
                    throw GrunnlagNotFoundException("Ingen grunnlag funnet", e)
                }
                else -> {
                    logger.error{ "Feil ved serverkall til bidrag-grunnlag" }
                    secureLogger.error(e) { "Kall til bidrag-grunnlag feilet: ${e.message}" }
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error{ "Uventet feil ved kall til bidrag-grunnlag" }
            secureLogger.error(e) { "Uventet feil ved kall til bidrag-grunnlag: ${e.message}" }
            throw e
        }
    }
}
