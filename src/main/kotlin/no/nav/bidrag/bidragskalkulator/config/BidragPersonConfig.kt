package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate


@Configuration
@EnableConfigurationProperties(BidragPersonConfigurationProperties::class)
@ConfigurationPropertiesScan
@Import(RestOperationsAzure::class, AppContext::class, SikkerhetsKontekst::class)
class BidragPersonConfig {

    @Bean
    fun provideBidragPersonConsumer(
        bidragPersonConfig: BidragPersonConfigurationProperties,
        @Qualifier("azure") restTemplate: RestTemplate,
        securityTokenService: SecurityTokenService
    ): BidragPersonConsumer {
        restTemplate.interceptors.add(securityTokenService.navConsumerTokenInterceptor())
        return BidragPersonConsumer(bidragPersonConfig, restTemplate)
    }

}

@ConfigurationProperties(prefix = "bidrag.person")
data class BidragPersonConfigurationProperties(
    val url: String,
    val hentMotpartbarnrelasjonPath: String,
    val hentPersoninformasjonPath: String,
    val hentDetaljerOmPersonPath: String
)
