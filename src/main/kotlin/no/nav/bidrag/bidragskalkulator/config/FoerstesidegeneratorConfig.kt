package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties(value = [FoerstesidegeneratorConfigurationProperties::class])
class FoerstesidegeneratorConfig {

    @Bean
    fun foerstesidegeneratorConsumer(
        props: FoerstesidegeneratorConfigurationProperties,
        @Qualifier("azure") azureRestTemplate: RestTemplate
    ) = FoerstesidegeneratorConsumer(props, azureRestTemplate, foerstesidegeneratorHeaders())

    @Bean
    fun foerstesidegeneratorHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            accept = listOf(APPLICATION_JSON)
            contentType = APPLICATION_JSON
            set("Nav-Consumer-Id", "bidrag-bidragskalkulator-api")
        }
    }
}

@ConfigurationProperties(prefix = "foerstesidegenerator")
data class FoerstesidegeneratorConfigurationProperties(
    val url: String,
    val genererFoerstesidePath: String
)
