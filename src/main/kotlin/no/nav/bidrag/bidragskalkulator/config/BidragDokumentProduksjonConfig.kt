package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.prosessor.PrivatAvtalePdfProsessor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(GrunnlagConfigurationProperties::class)
@ConfigurationPropertiesScan
class DokumentProduksjonConfiguration {

    @Bean
    fun provideDokumentproduksjonConsumer(
        properties: DokumentproduksjonConfigurationProperties,
    ): BidragDokumentProduksjonConsumer {
        val restTemplate = RestTemplate()
        return BidragDokumentProduksjonConsumer(properties, restTemplate, dokumentProduksjonHeader())
    }

    @Bean("PrivatAvtalePdfProsessor")
    fun providePdfProsessor(): PrivatAvtalePdfProsessor {
        return PrivatAvtalePdfProsessor()
    }

    @Bean
    fun dokumentProduksjonHeader(): HttpHeaders = HttpHeaders().apply {
        accept = listOf(APPLICATION_JSON)
        contentType = APPLICATION_JSON
    }
}

@ConfigurationProperties(prefix = "bidrag.dokumentproduksjon")
data class DokumentproduksjonConfigurationProperties(
    val url: String,
    val genererPdfPath: String
)
