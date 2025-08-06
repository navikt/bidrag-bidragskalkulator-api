package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.SafSelvbetjeningConsumer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class SafSelvbetjeningConfiguration {

    @Bean
    fun provideSafSelvbetjeningConsumer(
        properties: SafSelvbetjeningConfigurationProperties,
        @Qualifier("azure") azureRestTemplate: RestTemplate,
    ): SafSelvbetjeningConsumer {
        return SafSelvbetjeningConsumer(properties, azureRestTemplate)
    }
}

@ConfigurationProperties(prefix = "saf.selvbetjening")
class SafSelvbetjeningConfigurationProperties(
    val url: String,
)
