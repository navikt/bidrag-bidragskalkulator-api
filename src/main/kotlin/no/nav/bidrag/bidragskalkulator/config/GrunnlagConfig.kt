package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(GrunnlagConfigurationProperties::class)
@ConfigurationPropertiesScan
@Import(RestOperationsAzure::class, AppContext::class, SikkerhetsKontekst::class)
class GrunnlagConfig

@ConfigurationProperties(prefix = "bidrag.grunnlag")
data class GrunnlagConfigurationProperties(
    val url: String,
    val hentGrunnlagPath: String
)
