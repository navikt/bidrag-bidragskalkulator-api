package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.GrunnlagConfigurationProperties
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.transport.behandling.grunnlag.request.GrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service("bidragGrunnlagConsumer")
class BidragGrunnlagConsumer(
    val grunnlagConfig: GrunnlagConfigurationProperties,
    @Qualifier("azure") val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag.grunnlag") {

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

    fun hentGrunnlag(ident: String, antallAar: Int = HENT_INNTEKT_ANTALl_ÅR_DEFAULT): HentGrunnlagDto {
        return try {
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
        } catch(e: HttpServerErrorException) {
            when (e.statusCode.value()) {
                404 -> {
                    secureLogger.warn("Fant ikke person med ident $ident")
                    throw e
                }
                else -> {
                    secureLogger.error("Feil ved serverkall til bidrag-grunnlag for ident $ident", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            secureLogger.error("Uventet feil ved kall til bidrag-grunnlag for ident $ident - ${e.localizedMessage}", e)
            throw e
        }
    }
}