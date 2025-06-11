package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.bidragskalkulator.dto.MockLoginResponseDto
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate

class MockLoginServiceTest {

    private lateinit var mockLoginService: MockLoginService
    private val mockRestTemplate = mockk<RestTemplate>()

    @BeforeEach
    fun setUp() {
        mockLoginService = MockLoginService(mockRestTemplate)
    }

    @Test
    fun `genererMockTokenXToken skal gjøre et POST request og returnere et token`() {
        // Given
        val ident = Personident("18489011049")
        val forventetToken = "mock-token-verdi"
        val requestEntitySlot = slot<HttpEntity<*>>()

        every { 
            mockRestTemplate.postForObject(
                "https://tokenx-token-generator.intern.dev.nav.no/api/public/obo", 
                capture(requestEntitySlot), 
                String::class.java
            ) 
        } returns forventetToken

        val resultat = mockLoginService.genererMockTokenXToken(ident)

        assertEquals(MockLoginResponseDto(forventetToken), resultat)

        val requestBody = requestEntitySlot.captured.body as org.springframework.util.MultiValueMap<*, *>
        assertEquals("dev-gcp:bidrag:bidrag-bidragskalkulator-api", requestBody["aud"]?.get(0))

        assertNotNull(requestBody["pid"]?.get(0))
        assertTrue((requestBody["pid"]?.get(0) as String).isNotEmpty())
    }

    @Test
    fun `genererMockTokenXToken burde kaste feil når man får null-respons fra token-generatoren`() {
        // Given
        val ident = Personident("18489011049")

        every { 
            mockRestTemplate.postForObject(
                "https://tokenx-token-generator.intern.dev.nav.no/api/public/obo", 
                any<HttpEntity<*>>(), 
                String::class.java
            ) 
        } returns null

        val exception = assertThrows<IllegalStateException> {
            mockLoginService.genererMockTokenXToken(ident)
        }

        assertEquals("Mottok tomt svar fra TokenX token generator", exception.message)
    }
}
