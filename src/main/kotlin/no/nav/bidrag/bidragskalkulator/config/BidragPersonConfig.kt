package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(BidragPersonConfigurationProperties::class)
class BidragPersonConfig {

    @Bean
    fun bidragPersonConsumer(
        config: BidragPersonConfigurationProperties,
        @Qualifier("azure") restTemplate: RestTemplate
    ) = BidragPersonConsumer(config, restTemplate)

}

@ConfigurationProperties(prefix = "bidrag.person")
data class BidragPersonConfigurationProperties(
    val url: String,
    val hentMotpartbarnrelasjonPath: String,
    val hentPersoninformasjonPath: String,
)
