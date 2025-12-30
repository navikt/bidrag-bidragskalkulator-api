package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.dto.BarnMedIdentDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.BoforholdDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedAlderDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.generer.testdata.person.genererPersonident
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.dto.UtvidetBarnetrygdDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto

class BeregningControllerTest : AbstractControllerTest() {
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
        every { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } } returns
                mockÅpenRespons
    }

    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = mockGyldigÅpenRequest.copy(inntektForelder1 = -500000.0)

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `skal returnere 400 for et tom barn liste`() {
        val request = mockGyldigÅpenRequest.copy(barn = emptyList())

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("barn: Liste over barn kan ikke være tom"))
    }

    @Test
    fun `skal returnere 400 hvis dittBoforhold mangler for PLIKTIG`() {
        val ugyldigRequest = mockGyldigÅpenRequest.copy(dittBoforhold = null)

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'dittBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragspliktig."))
    }

    @Test
    fun `skal returnere 400 hvis medforelderBoforhold mangler for MOTTAKER`() {
        val ugyldigRequest = mockGyldigÅpenRequest.copy(
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.MOTTAKER
                )
            ),
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'medforelderBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragsmottaker."))
    }

    @Test
    fun `skal returnere 400 hvis bruker er både bidragsmottaker og bidragspliktig, og begge boforhold mangler`() {
        val ugyldigRequest = mockGyldigÅpenRequest.copy(
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.MOTTAKER
                ),
                BarnMedAlderDto(
                    alder = 3,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.PLIKTIG
                )
            ),
            dittBoforhold = null,
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Både 'dittBoforhold' og 'medforelderBoforhold' mangler, men må være satt når forespørselen inneholder barn der du er bidragspliktig og/eller bidragsmottaker."))
    }

    @Test
    fun `skal returnere 400 hvis bruker er både bidragsmottaker og bidragspliktig, og medforelderBoforhold mangler i forespørselen`() {
        val ugyldigRequest = mockGyldigÅpenRequest.copy(
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.MOTTAKER
                ),
                BarnMedAlderDto(
                    alder = 3,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.PLIKTIG
                )
            ),
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'medforelderBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragsmottaker."))
    }

    @Test
    fun `skal returnere 400 hvis kontantstøtte er satt og alder ikke er 1`() {
        val request = mockGyldigÅpenRequest.copy(
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                    bidragstype = BidragsType.MOTTAKER,
                    kontantstøtte = BigDecimal("500")
                ),
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Kontantstøtte kan kun settes for barn som er 1 år (barn[0] har alder=2)"))

        verify(exactly = 0) { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } }
    }

    @Test
    fun `skal returnere 200 OK når kontantstøtte er satt for barn som er 1 år`() {
        val request = mockGyldigÅpenRequest.copy(
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                    bidragstype = BidragsType.MOTTAKER,
                    kontantstøtte = BigDecimal("500")
                ),
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isOk)

        verify(exactly = 1) { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } }
    }

    @Test
    fun `skal returnere 400 nar utvidetBarnetrygd delerMedMedforelder er true men harUtvidetBarnetrygd er false`() {
        val request = mockGyldigÅpenRequest.copy(
            utvidetBarnetrygd = UtvidetBarnetrygdDto(
                harUtvidetBarnetrygd = false,
                delerMedMedforelder = true
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.detail").value(
                    "utvidetBarnetrygd.delerMedMedforelder kan ikke være true når utvidetBarnetrygd.harUtvidetBarnetrygd = false"
                )
            )
    }

    @Test
    fun `skal returnere 400 når småbarnstillegg er true uten barn 0-3 år`() {
        val request = mockGyldigÅpenRequest.copy(
            småbarnstillegg = true,
            barn = mockGyldigÅpenRequest.barn.map { it.copy(alder = 4, kontantstøtte = null) },
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.detail").value(
                    "småbarnstillegg kan kun settes til true når det finnes minst ett barn med alder 0-3 år"
                )
            )
    }

    companion object TestData {
        private val personIdent = genererPersonident()

        val mockGyldigRequest = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnMedIdentDto(
                    ident = personIdent,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.PLIKTIG
                )
            ),
            dittBoforhold = BoforholdDto(antallBarnBorFast = 0, antallBarnDeltBosted = 0, borMedAnnenVoksen = false)
        )

        val mockGyldigÅpenRequest = ÅpenBeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    bidragstype = BidragsType.PLIKTIG,
                    barnetilsynsutgift = BigDecimal.ZERO,
                    inntekt = BigDecimal.ZERO,
                    kontantstøtte = BigDecimal.ZERO
                )
            ),
            dittBoforhold = BoforholdDto(antallBarnBorFast = 0, antallBarnDeltBosted = 0, borMedAnnenVoksen = false),
            medforelderBoforhold = BoforholdDto(antallBarnBorFast = 0, antallBarnDeltBosted = 0, borMedAnnenVoksen = false)
        )

        val mockRespons = BeregningsresultatDto(
            resultater = listOf(
                BeregningsresultatBarnDto(
                    sum = BigDecimal(100),
                    ident = personIdent,
                    fulltNavn = "Navn Navnesen",
                    fornavn = "Navn",
                    alder = 10,
                    bidragstype = mockGyldigRequest.barn.first().bidragstype
                )
            )
        )

        val mockÅpenRespons = ÅpenBeregningsresultatDto(
            resultater = listOf(
                ÅpenBeregningsresultatBarnDto(
                    sum = BigDecimal(100),
                    alder = 10,
                    bidragstype = mockGyldigÅpenRequest.barn.first().bidragstype
                )
            )
        )
    }
}
