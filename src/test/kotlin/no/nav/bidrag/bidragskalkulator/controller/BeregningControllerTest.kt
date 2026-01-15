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
import no.nav.bidrag.bidragskalkulator.dto.BarnetilsynDto
import no.nav.bidrag.bidragskalkulator.dto.ForelderInntektDto
import no.nav.bidrag.bidragskalkulator.dto.KontantstøtteDto
import no.nav.bidrag.bidragskalkulator.dto.UtvidetBarnetrygdDto
import no.nav.bidrag.bidragskalkulator.dto.VoksneOver18Type
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype

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
        val request = mockGyldigÅpenRequest.copy(bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("-100000")))

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("bidragsmottakerInntekt.inntekt: Inntekt kan ikke være negativ"))
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
        val ugyldigRequest = mockGyldigÅpenRequest.copy(bidragstype = BidragsType.PLIKTIG, dittBoforhold = null)

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'dittBoforhold' må være satt fordi du er bidragspliktig i forespørselen."))
    }

    @Test
    fun `skal returnere 400 hvis medforelderBoforhold mangler for MOTTAKER`() {
        val ugyldigRequest = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                )
            ),
            medforelderBoforhold = null
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", ugyldigRequest)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'medforelderBoforhold' må være satt fordi du er bidragsmottaker i forespørselen."))
    }

    @Test
    fun `skal returnere 400 hvis antallBarnOver18Vgs mangler når voksneOver18Type inneholder EGNE_BARN_OVER_18`() {
        val request = mockGyldigÅpenRequest.copy(
            dittBoforhold = BoforholdDto(
                antallBarnUnder18BorFast = 1,
                voksneOver18Type = setOf(VoksneOver18Type.EGNE_BARN_OVER_18),
                antallBarnOver18Vgs = null
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("'antallBarnOver18Vgs' må være satt når 'voksneOver18Type' inneholder 'EGNE_BARN_OVER_18'."))
    }

    @Test
    fun `skal returnere 200 hvis voksneOver18Type inneholder EGNE_BARN_OVER_18 og antallBarnOver18Vgs ikke er null`() {
        val request = mockGyldigÅpenRequest.copy(
            dittBoforhold = BoforholdDto(
                antallBarnUnder18BorFast = 1,
                voksneOver18Type = setOf(VoksneOver18Type.EGNE_BARN_OVER_18),
                antallBarnOver18Vgs = 2
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 400 hvis kontantstøtte er satt og alder ikke er 1`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 2,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                    kontantstøtte = KontantstøtteDto(beløp = BigDecimal("500"))
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
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                    kontantstøtte = KontantstøtteDto(beløp = BigDecimal("500"))
                ),
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isOk)

        verify(exactly = 1) { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } }
    }

    @Test
    fun `skal returnere 400 hvis kontantstøtte deles er satt uten beløp`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                    kontantstøtte = KontantstøtteDto(beløp = null, deles = true)
                )
            )
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail")
                .value("kontantstøtte.deles kan ikke settes uten at beløp også er satt (barn[0])"))
    }


    @Test
    fun `skal returnere 400 nar utvidetBarnetrygd delerMedMedforelder er true men harUtvidetBarnetrygd er false`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
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
            bidragstype = BidragsType.MOTTAKER,
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

    @Test
    fun `skal gi 200 ved gyldig barnetilsyn i beregning request`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = mockGyldigÅpenRequest.barn
                .map { it.copy(alder = 1, barnetilsyn = BarnetilsynDto(månedligUtgift = BigDecimal("1200"))) },
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isOk)

        verify(exactly = 1) { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } }
    }

    @Test
    fun `skal gi 400 når barnetilsyn har både månedligUtgift=1200 og plassType=DELTID`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = mockGyldigÅpenRequest.barn.mapIndexed { idx, barn ->
                if (idx == 0) {
                    barn.copy(
                        barnetilsyn = barn.barnetilsyn?.copy(
                            månedligUtgift = BigDecimal("1200"),
                            plassType = Tilsynstype.DELTID,
                        )
                    )
                } else barn }
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.detail").value(
                    "Ugyldig barnetilsyn: kan ikke oppgi både månedligUtgift og plassType samtidig."
                )
            )
    }

    @Test
    fun `skal gi 400 når barn er over 10 år og barnetilsyn månedligUtgift er gitt`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = mockGyldigÅpenRequest.barn.mapIndexed { idx, barn ->
                if (idx == 0) {
                    barn.copy(
                        alder = 11,
                        barnetilsyn = barn.barnetilsyn?.copy(
                            månedligUtgift = BigDecimal("1200")
                        )
                    )
                } else barn }
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.detail").value(
                    "Barnetilsyn kan ikke oppgis for barn over 10 år (barnets alder=11)."
                )
            )
    }

    @Test
    fun `skal gi 200 når barnetilsyn har kun plassType=DELTID`() {
        val request = mockGyldigÅpenRequest.copy(
            bidragstype = BidragsType.MOTTAKER,
            barn = mockGyldigÅpenRequest.barn.mapIndexed { idx, barn ->
                if (idx == 0) {
                    barn.copy(
                        barnetilsyn = BarnetilsynDto(
                            månedligUtgift = null,
                            plassType = Tilsynstype.DELTID
                        )
                    )
                } else barn
            }
        )

        postRequest("/api/v1/beregning/barnebidrag/åpen", request)
            .andExpect(status().isOk)

        verify(exactly = 1) { runBlocking { beregningService.beregnBarnebidragAnonym(any()) } }
    }


    companion object TestData {
        private val personIdent = genererPersonident()

        val mockGyldigRequest = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("400000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(
                BarnMedIdentDto(
                    ident = personIdent,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                )
            ),
            dittBoforhold = BoforholdDto(antallBarnUnder18BorFast = 0, voksneOver18Type = null, antallBarnOver18Vgs = 0),
        )

        val mockGyldigÅpenRequest = ÅpenBeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("400000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                    barnetilsyn = BarnetilsynDto(månedligUtgift = BigDecimal("1200")),
                    inntekt = BigDecimal.ZERO,
                    kontantstøtte = KontantstøtteDto(beløp = BigDecimal.ZERO)
                )
            ),
            dittBoforhold = BoforholdDto(antallBarnUnder18BorFast = 0, voksneOver18Type = null, antallBarnOver18Vgs = 0),
            medforelderBoforhold = BoforholdDto(antallBarnUnder18BorFast = 0, voksneOver18Type = null, antallBarnOver18Vgs = 0),
        )

        val mockRespons = BeregningsresultatDto(
            resultater = listOf(
                BeregningsresultatBarnDto(
                    sum = BigDecimal(100),
                    ident = personIdent,
                    fulltNavn = "Navn Navnesen",
                    fornavn = "Navn",
                    alder = 10,
                )
            )
        )

        val mockÅpenRespons = ÅpenBeregningsresultatDto(
            resultater = listOf(
                ÅpenBeregningsresultatBarnDto(
                    sum = BigDecimal(100),
                    alder = 10,
                )
            )
        )
    }
}
