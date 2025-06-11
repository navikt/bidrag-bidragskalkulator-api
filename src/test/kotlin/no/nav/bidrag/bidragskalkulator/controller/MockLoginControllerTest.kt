package no.nav.bidrag.bidragskalkulator.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.dto.MockLoginResponseDto
import no.nav.bidrag.bidragskalkulator.service.MockLoginService
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockLoginControllerTest {

    private lateinit var mockLoginController: MockLoginController
    private val mockLoginService = mockk<MockLoginService>()

    @BeforeEach
    fun setUp() {
        mockLoginController = MockLoginController(mockLoginService)
    }

    @Test
    fun `mockLogin burde returnere tokenet fra servicen`() {
        // Given
        val ident = Personident("18489011049")
        val forventetToken = "mock-token-verdi"
        val forventetRespons = MockLoginResponseDto(forventetToken)

        every { 
            mockLoginService.genererMockTokenXToken(ident)
        } returns forventetRespons

        val result = mockLoginController.mockLogin(ident)

        assertEquals(forventetRespons, result)
    }
}
