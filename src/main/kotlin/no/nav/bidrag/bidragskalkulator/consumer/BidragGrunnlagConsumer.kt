package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.bidragskalkulator.config.GrunnlagConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.GrunnlagRequestDto
import no.nav.bidrag.bidragskalkulator.dto.GrunnlagRequestItemDto
import no.nav.bidrag.bidragskalkulator.dto.GrunnlagResponseDto
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BidragGrunnlagConsumer(
    val grunnlagConfig: GrunnlagConfigurationProperties,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag.grunnlag") {

    val logger = getLogger(BidragGrunnlagConsumer::class.java)

    init {
        checkNotNull(grunnlagConfig.url) { "grunnlag url is not set" }
        checkNotNull(grunnlagConfig.hentGrunnlagPath) { "hentGrunnlagPath is not set" }
    }

    val grunnlagUri by lazy { UriComponentsBuilder
        .fromUri(URI.create(grunnlagConfig.url))
        .pathSegment(grunnlagConfig.hentGrunnlagPath)
        .build()
        .toUri()
    }

    val grunnlagDatoFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    fun hentGrunnlag(ident: String): String {
        // make post request to grunnlag api
        postForEntity<GrunnlagResponseDto>(grunnlagUri, GrunnlagRequestDto("FORSKUDD", arrayListOf(
            GrunnlagRequestItemDto(
                type = "AINNTEKT",
                personId = ident,
                periodeFra = LocalDate.now().minusYears(2).format(grunnlagDatoFormatter).toString(),
                periodeTil = LocalDate.now().format(grunnlagDatoFormatter).toString(),
            )
        )))
        secureLogger.info("Henter grunnlag for person med ident: $ident")
        return "Grunnlag"
    }
}