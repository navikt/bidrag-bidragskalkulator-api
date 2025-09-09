package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.runBlocking
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
    @MockkBean(relaxUnitFun = true)
    private lateinit var beregningService: BeregningService

    @Test
    fun `skal bekrefte at mock OAuth2-server kjører`() {
        val issuerUrl = mockOAuth2Server.issuerUrl("tokenx").toString()
        println("Issuer URL: $issuerUrl")
        assert(issuerUrl.contains("localhost"))
    }

    @BeforeEach
    fun setupMocks() {
        every { runBlocking { beregningService.beregnBarnebidrag(mockGyldigRequest) } } returns mockRespons
    }

    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = mockGyldigRequest.copy(inntektForelder1 = -500000.0)

        postRequest("/api/v1/beregning/barnebidrag/åpen", request, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `skal returnere 400 for et tom barn liste`() {
        val request = mockGyldigRequest.copy(barn = emptyList())

        postRequest("/api/v1/beregning/barnebidrag/åpen", request, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("barn: Liste over barn kan ikke være tom"))
    }

    @Test
    fun `skal returnere 400 hvis dittBoforhold mangler for PLIKTIG`() {
        val ugyldigRequest = mockGyldigRequest.copy(dittBoforhold = null)

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'dittBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragspliktig."))
    }

    @Test
    fun `skal returnere 400 hvis medforelderBoforhold mangler for MOTTAKER`() {
        val ugyldigRequest = mockGyldigRequest.copy(
            barn = listOf(BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.MOTTAKER))
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'medforelderBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragsmottaker."))
    }

    @Test
    fun `skal returnere 400 hvis bruker er både bidragsmottaker og bidragspliktig, og begge boforhold mangler`() {
        val ugyldigRequest = mockGyldigRequest.copy(
            barn = listOf(
                BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.MOTTAKER),
                BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.PLIKTIG)
            ),
            dittBoforhold = null,
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Både 'dittBoforhold' og 'medforelderBoforhold' mangler, men må være satt når forespørselen inneholder barn der du er bidragspliktig og/eller bidragsmottaker."))
    }

    @Test
    fun `skal returnere 400 hvis bruker er både bidragsmottaker og bidragspliktig, og medforelderBoforhold mangler i forespørselen`() {
        val ugyldigRequest = mockGyldigRequest.copy(
            barn = listOf(
                BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.MOTTAKER),
                BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.PLIKTIG)
            ),
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'medforelderBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragsmottaker."))
    }


    companion object TestData {
        private val personIdent = "12345678910"

        val mockGyldigRequest = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(BarnMedIdentDto(ident = Personident(personIdent), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0, bidragstype = BidragsType.PLIKTIG)),
            dittBoforhold = BoforholdDto(antallBarnBorFast = 0, antallBarnDeltBosted = 0, borMedAnnenVoksen = false)
        )

        val mockRespons = BeregningsresultatDto(
            resultater = listOf(
                BeregningsresultatBarnDto(sum = BigDecimal(100), ident = Personident(personIdent), fulltNavn = "Navn Navnesen", fornavn = "Navn", alder = 10, bidragstype = mockGyldigRequest.barn.first().bidragstype)
            )
        )
    }
}
