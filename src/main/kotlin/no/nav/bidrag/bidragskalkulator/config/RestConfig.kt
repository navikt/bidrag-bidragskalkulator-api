package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
@Import(RestOperationsAzure::class, AppContext::class, SikkerhetsKontekst::class)
class RestConfig {

    @Bean
    fun defaultRestTemplateCustomizer(): RestTemplateCustomizer = RestTemplateCustomizer { restTemplate ->
        val factory = restTemplate.requestFactory
        if (factory is HttpComponentsClientHttpRequestFactory) {
            factory.setConnectTimeout(30_000)
            factory.setReadTimeout(30_000)
        }
    }

    @Bean
    fun restTemplateWithInterceptor(
        @Qualifier("azure") azureRestTemplate: RestTemplate,
        securityTokenService: SecurityTokenService
    ): RestTemplate {
        azureRestTemplate.interceptors.add(securityTokenService.navConsumerTokenInterceptor())
        return azureRestTemplate
    }

}
