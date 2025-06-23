package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties(value = [FoerstesidegeneratorConfigurationProperties::class])
class FoerstesidegeneratorConfig {

    @Bean
    fun provideFoerstesidegeneratorConsumer(
        foerstesidegeneratorConfigurationProperties: FoerstesidegeneratorConfigurationProperties,
        @Qualifier("azure") azureRestTemplate: RestTemplate,
    ): FoerstesidegeneratorConsumer {

        return FoerstesidegeneratorConsumer(foerstesidegeneratorConfigurationProperties, azureRestTemplate)
    }
}

@ConfigurationProperties(prefix = "foerstesidegenerator")
data class FoerstesidegeneratorConfigurationProperties(
    val url: String,
    val genererFoerstesidePath: String
)