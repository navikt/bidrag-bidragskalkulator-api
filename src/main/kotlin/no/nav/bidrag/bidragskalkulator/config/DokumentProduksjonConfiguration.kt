package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.prosessor.PrivatAvtalePdfProsessor
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
@EnableConfigurationProperties(GrunnlagConfigurationProperties::class)
@ConfigurationPropertiesScan
@Import(RestOperationsAzure::class, AppContext::class)
class DokumentProduksjonConfiguration {

    @Bean
    fun provideDokumentproduksjonConsumer(
        properties: DokumentproduksjonConfigurationProperties,
        @Qualifier("azure") restTemplate: RestTemplate
    ): BidragDokumentProduksjonConsumer {
        return BidragDokumentProduksjonConsumer(properties, restTemplate)
    }

    @Bean("PrivatAvtalePdfProsessor")
    fun providePdfProsessor(): PrivatAvtalePdfProsessor {
        return PrivatAvtalePdfProsessor()
    }
}

@ConfigurationProperties(prefix = "bidrag.dokumentproduksjon")
data class DokumentproduksjonConfigurationProperties(
    val url: String,
    val genererPdfPath: String
)
