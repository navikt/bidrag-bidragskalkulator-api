package no.nav.bidrag.bidragskalkulator.bidragPersonConsumer

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersondetaljerDto
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

    private val baseUrl = URI("http://localhost")

    @BeforeEach
    fun setUp() {
        bidragPersonConsumer = BidragPersonConsumer(baseUrl, restTemplate)
    }

    @Test
    fun `skal returnere familierelasjon`() {
        val ident = "12345678901"
        val forventetRespons: MotpartBarnRelasjonDto =
            JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")

       mockPostCall(forventetRespons)

        val faktisk = bidragPersonConsumer.hentFamilierelasjon(ident)

        assertThat(faktisk).isEqualTo(forventetRespons)
    }

    @Test
    fun `skal returnere detaljert informasjon`() {
        val ident = "12345678901"

        val forventetRespons: PersondetaljerDto =
            JsonUtils.readJsonFile("/person/person_detaljert_informasjon.json")

        mockPostCall(forventetRespons)

        val faktisk = bidragPersonConsumer.hentDetaljertInformasjon(ident)

        assertThat(faktisk).isEqualTo(forventetRespons)
    }

    @Test
    fun `skal kaste NoContentException når 404 fra bidrag-person`() {
        val ident = "12345678901"
        val exception = HttpServerErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null)

        mockPostCallThrows(exception)

        assertThatThrownBy {
            bidragPersonConsumer.hentFamilierelasjon(ident)
        }.isInstanceOf(NoContentException::class.java)
    }

    @Test
    fun `skal kaste original feil ved 500`() {
        val ident = "12345678901"
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