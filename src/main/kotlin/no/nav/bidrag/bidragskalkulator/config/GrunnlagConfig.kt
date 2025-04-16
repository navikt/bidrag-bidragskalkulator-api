package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.service.GrunnlagService
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.inntekt.InntektApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(GrunnlagConfigurationProperties::class)
@ConfigurationPropertiesScan
@Import(RestOperationsAzure::class, AppContext::class, SikkerhetsKontekst::class, InntektApi::class)
class GrunnlagConfig {

    @Bean
    fun provideGrunnlagConsumer(
        grunnlagConfig: GrunnlagConfigurationProperties,
        @Qualifier("azure") restTemplate: RestTemplate,
        securityTokenService: SecurityTokenService
    ): BidragGrunnlagConsumer {
        restTemplate.interceptors.add(securityTokenService.navConsumerTokenInterceptor())
        return BidragGrunnlagConsumer(grunnlagConfig, restTemplate)
    }

}

@ConfigurationProperties(prefix = "bidrag.grunnlag")
data class GrunnlagConfigurationProperties(
    val url: String,
    val hentGrunnlagPath: String
)
