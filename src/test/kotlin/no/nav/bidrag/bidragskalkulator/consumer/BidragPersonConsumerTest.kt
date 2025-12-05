package no.nav.bidrag.bidragskalkulator.consumer

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.config.BidragPersonConfigurationProperties
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity


class BidragPersonbidragPersonConsumerTest {

    private val restTemplate: RestTemplate = mockk()
    private lateinit var bidragPersonConsumer: BidragPersonConsumer
    private lateinit var bidragPersonConfig: BidragPersonConfigurationProperties

    @BeforeEach
    fun setUp() {
        bidragPersonConfig = BidragPersonConfigurationProperties(
            url = "http://dummy-url.no",
            hentMotpartbarnrelasjonPath = "hentMotpartbarnrelasjon",
            hentPersoninformasjonPath = "informasjon"
        )

        bidragPersonConsumer = BidragPersonConsumer(bidragPersonConfig, restTemplate)
    }

    @Test
    fun `skal returnere familierelasjon`() {
        val ident = genererFødselsnummer()
        val forventetRespons: MotpartBarnRelasjonDto =
            JsonUtils.lesJsonFil(filnavn = "/person/person_med_barn_et_motpart.json")

       mockPostCall(forventetRespons)

        val faktisk = bidragPersonConsumer.hentFamilierelasjon(ident)

        assertThat(faktisk).isEqualTo(forventetRespons)
    }

    @Test
    fun `skal kaste NoContentException når 404 fra bidrag-person`() {
        val ident = genererFødselsnummer()
        val exception = HttpServerErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null)

        mockPostCallThrows(exception)

        assertThatThrownBy {
            bidragPersonConsumer.hentFamilierelasjon(ident)
        }.isInstanceOf(NoContentException::class.java)
    }

    @Test
    fun `skal kaste original feil ved 500`() {
        val ident = genererFødselsnummer()
        val exception = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Serverfeil", null, null, null)

        mockPostCallThrows(exception)

        assertThatThrownBy {
            bidragPersonConsumer.hentFamilierelasjon(ident)
        }.isSameAs(exception)
    }

    private inline fun <reified T : Any> mockPostCall(response: T) {
        every {
            restTemplate.exchange(
                any<URI>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<T>>()
            )
        } returns ResponseEntity.ok(response)
    }

    private fun mockPostCallThrows(exception: Exception) {
        every {
            restTemplate.exchange(
                any<URI>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>()
            )
        } throws exception
    }
}
