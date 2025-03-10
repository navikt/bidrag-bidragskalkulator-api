import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregnGrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.BeregnGrunnlagMedAlder
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var beregnGrunnlagMapper: BeregnGrunnlagMapper

    @InjectMocks
    private lateinit var beregningService: BeregningService

    private fun createResultatPeriode(): ResultatPeriode {
        return ResultatPeriode(
            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            resultat = ResultatBeregning(BigDecimal(3200)),
            grunnlagsreferanseListe = emptyList()
        )
    }

    @Test
    fun `skal returnere tomt resultat dersom ingen barn er oppgitt`() {
        val beregningRequest = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 600000.0,
            barn = emptyList()
        )

        val resultat = beregningService.beregnBarnebidrag(beregningRequest)

        assertTrue(resultat.beregningsResultater.isEmpty(), "Forventet tomt resultat når ingen barn er oppgitt")
    }

    @Test
    fun `skal returnere ett beregningsresultat for ett barn`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("beregning_et_barn.json")

        val beregnGrunnlagMedAlder = beregningRequest.barn.mapIndexed { index, barnDto ->
            BeregnGrunnlagMedAlder(
                barnetsAlder = barnDto.alder,
                beregnGrunnlag = BeregnGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
                    søknadsbarnReferanse = "Person_Søknadsbarn_$index",
                )
            )
        }

        val beregnetResultat = BeregnetBarnebidragResultat(beregnetBarnebidragPeriodeListe = listOf(createResultatPeriode()))

        Mockito.`when`(beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)).thenReturn(beregnGrunnlagMedAlder)
        Mockito.`when`(beregnBarnebidragApi.beregn(anyOrNull())).thenReturn(beregnetResultat)

        val resultat = beregningService.beregnBarnebidrag(beregningRequest)

        assertEquals(1, resultat.beregningsResultater.size)
        assertEquals(beregningRequest.barn.first().alder, resultat.beregningsResultater.first().barnetsAlder)
    }

    @Test
    fun `skal returnere to beregningsresultater for to barn`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("beregning_to_barn.json")

        val beregnGrunnlagMedAlder = beregningRequest.barn.mapIndexed { index, barnDto ->
            BeregnGrunnlagMedAlder(
                barnetsAlder = barnDto.alder,
                beregnGrunnlag = BeregnGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
                    søknadsbarnReferanse = "Person_Søknadsbarn_$index",
                )
            )
        }

        val beregnetResultat = BeregnetBarnebidragResultat(beregnetBarnebidragPeriodeListe = listOf(createResultatPeriode()))

        Mockito.`when`(beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)).thenReturn(beregnGrunnlagMedAlder)
        Mockito.`when`(beregnBarnebidragApi.beregn(anyOrNull())).thenReturn(beregnetResultat)

        val resultat = beregningService.beregnBarnebidrag(beregningRequest)

        assertEquals(2, resultat.beregningsResultater.size)
    }
}