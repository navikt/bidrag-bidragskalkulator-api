import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.GrunnlagOgBarnInformasjon
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.YearMonth
import org.mockito.kotlin.anyOrNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class BeregningServiceTest {

    @Mock
    private lateinit var beregnBarnebidragApi: BeregnBarnebidragApi

    @Mock
    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    @InjectMocks
    private lateinit var beregningService: BeregningService

    @Nested
    inner class BeregningForEttBarn {

        private lateinit var beregningRequest: BeregningRequestDto
        private lateinit var beregningResultat: BeregningsresultatDto

        @BeforeEach
        fun oppsett() {
            beregningRequest = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

            val grunnlagOgAlder = beregningRequest.barn.mapIndexed { index, barnDto ->
                GrunnlagOgBarnInformasjon(
                    ident = barnDto.ident,
                    fulltNavn = "Navn Navnesen",
                    fornavn = "Navn",
                    alder = kalkulereAlder(barnDto.ident.fødselsdato()),
                    bidragsType = barnDto.bidragstype,
                    grunnlag = BeregnGrunnlag(
                        periode = ÅrMånedsperiode(YearMonth.now(), null),
                        søknadsbarnReferanse = "Person_Søknadsbarn_$index",
                    )
                )
            }

            val beregnetResultat = BeregnetBarnebidragResultat(beregnetBarnebidragPeriodeListe = listOf(createResultatPeriode()))

            Mockito.`when`(beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)).thenReturn(grunnlagOgAlder)
            Mockito.`when`(beregnBarnebidragApi.beregn(anyOrNull())).thenReturn(beregnetResultat)

            beregningResultat = beregningService.beregnBarnebidrag(beregningRequest)

        }

        @Test
        fun `skal returnere tomt resultat dersom ingen barn er oppgitt`() {
            val beregningRequest = BeregningRequestDto(
                inntektForelder1 = 500000.0,
                inntektForelder2 = 600000.0,
                barn = emptyList()
            )

            val resultat = beregningService.beregnBarnebidrag(beregningRequest)

            assertTrue(resultat.resultater.isEmpty(), "Forventet tomt resultat når ingen barn er oppgitt")
        }

        @Test
        fun `skal returnere ett beregningsresultat for ett barn`() {
            assertEquals(1, beregningResultat.resultater.size)
            assertEquals(beregningRequest.barn.first().ident, beregningResultat.resultater.first().ident)
        }

        @Test
        fun `skal runder bidraget til nærmeste hundre`() {
            beregningResultat.resultater.forEach { res ->
                val roundedValue = res.sum.divide(BigDecimal(100))
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))

                assertEquals(roundedValue, res.sum, "Summen er ikke rundet korrekt til nærmeste 100")
            }
        }
    }

    private fun createResultatPeriode(): ResultatPeriode {
        return ResultatPeriode(
            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            resultat = ResultatBeregning(BigDecimal(3220)),
            grunnlagsreferanseListe = emptyList()
        )
    }

    @Test
    fun `skal returnere to beregningsresultater for to barn`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_to_barn.json")

        val grunnlagOgAlder = beregningRequest.barn.mapIndexed { index, barnDto ->
            GrunnlagOgBarnInformasjon(
                ident = barnDto.ident,
                fulltNavn = "Navn Navnesen",
                fornavn = "Navn",
                alder = kalkulereAlder(barnDto.ident.fødselsdato()),
                bidragsType = barnDto.bidragstype,
                grunnlag = BeregnGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
                    søknadsbarnReferanse = "Person_Søknadsbarn_$index",
                )
            )
        }

        val beregnetResultat = BeregnetBarnebidragResultat(beregnetBarnebidragPeriodeListe = listOf(createResultatPeriode()))

        Mockito.`when`(beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)).thenReturn(grunnlagOgAlder)
        Mockito.`when`(beregnBarnebidragApi.beregn(anyOrNull())).thenReturn(beregnetResultat)

        val resultat = beregningService.beregnBarnebidrag(beregningRequest)

        assertEquals(2, resultat.resultater.size)
    }
}
