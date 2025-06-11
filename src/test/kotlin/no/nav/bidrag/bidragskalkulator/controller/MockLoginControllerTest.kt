package no.nav.bidrag.bidragskalkulator.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.dto.MockLoginResponseDto
import no.nav.bidrag.bidragskalkulator.service.MockLoginService
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class MockLoginControllerTest {

    private lateinit var mockLoginController: MockLoginController
    private val mockLoginService = mockk<MockLoginService>()

    @BeforeEach
    fun setUp() {
        mockLoginController = MockLoginController(mockLoginService)
    }

    @Test
    fun `mockLogin should return token from service`() {
        // Given
        val ident = Personident("18489011049")
        val expectedToken = "mock-token-value"
        val expectedResponse = MockLoginResponseDto(expectedToken)

        every { 
            mockLoginService.genererMockTokenXToken(ident)
        } returns expectedResponse

        // When
        val result = mockLoginController.mockLogin(ident)

        // Then
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `mockLogin should use default ident if not provided`() {
        // Given
        val defaultIdent = Personident("12345678901")
        val expectedToken = "default-mock-token"
        val expectedResponse = MockLoginResponseDto(expectedToken)

        every { 
            mockLoginService.genererMockTokenXToken(defaultIdent)
        } returns expectedResponse

        // When
        val result = mockLoginController.mockLogin(defaultIdent)

        // Then
        assertEquals(expectedResponse, result)
    }
}
