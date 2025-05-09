package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal


class BeregningControllerTest: AbstractControllerTest() {
    @MockkBean
    private lateinit var beregningService: BeregningService


    @BeforeEach
    fun setupMocks() {
        every { beregningService.beregnBarnebidrag(mockGyldigRequest) } returns mockRespons
    }

    @Test
    fun `skal bekrefte at mock OAuth2-server kjører`() {
        val issuerUrl = mockOAuth2Server.issuerUrl("tokenx").toString()
        println("Issuer URL: $issuerUrl")
        assert(issuerUrl.contains("localhost"))
    }

    @Test
    fun `skal returnere 200 OK med gyldig OAuth2-token`() {
        postRequest("/api/v1/beregning/barnebidrag", mockGyldigRequest, gyldigOAuth2Token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }

    @Test
    fun `skal returnere 401 Unauthorized når ingen token er gitt`() {
        postRequest("/api/v1/beregning/barnebidrag", mockGyldigRequest)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = mockGyldigRequest.copy(inntektForelder1 = -500000.0)

        postRequest("/api/v1/beregning/barnebidrag", request, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `skal returnere 400 for et tom barn liste`() {
        val request = mockGyldigRequest.copy(barn = emptyList())

        postRequest("/api/v1/beregning/barnebidrag", request, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn: Liste over barn kan ikke være tom"))
    }

    companion object TestData {
        private val personIdent = "12345678910"

        val mockGyldigRequest = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(BarnDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG))
        )

        val mockRespons = BeregningsresultatDto(
            resultater = listOf(
                BeregningsresultatBarnDto(sum = BigDecimal(100), ident = Personident(personIdent), fulltNavn = "Navn Navnesen", fornavn = "Navn", alder = 5, underholdskostnad = BigDecimal(8471), bidragstype = mockGyldigRequest.barn.first().bidragstype)
            )
        )
    }
}