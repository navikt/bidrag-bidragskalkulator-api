package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.inntekt.InntektApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(GrunnlagConfigurationProperties::class)
@Import(InntektApi::class)
class GrunnlagConfig {

    @Bean
    fun provideGrunnlagConsumer(
        config: GrunnlagConfigurationProperties,
        @Qualifier("restTemplateWithInterceptor") restTemplate: RestTemplate
    ) = BidragGrunnlagConsumer(config, restTemplate)

}

@ConfigurationProperties(prefix = "bidrag.grunnlag")
data class GrunnlagConfigurationProperties(
    val url: String,
    val hentGrunnlagPath: String
)
