import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagBuilder
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.tilFamilieRelasjon
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.service.UnderholdskostnadService
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class BeregningServiceTest {

    private val beregnBarnebidragApi = mockk<BeregnBarnebidragApi>()
    private val personService = mockk<PersonService>()
    private val underholdskostnadServiceMock = mockk<UnderholdskostnadService>()

    // bruk ekte
    private val mockBeregningsgrunnlagBuilder = BeregningsgrunnlagBuilder()
    private val beregningsgrunnlagMapper = BeregningsgrunnlagMapper(mockBeregningsgrunnlagBuilder)

    private val beregningService = BeregningService(beregnBarnebidragApi, underholdskostnadServiceMock, beregningsgrunnlagMapper, personService)

    @Nested
    inner class BeregningBarnebidragForEttBarn {

        private lateinit var beregningRequest: BeregningRequestDto
        private lateinit var beregningResultat: BeregningsresultatDto

        @BeforeEach
        fun oppsett() = runTest {
            beregningRequest = mockOppsett("/barnebidrag/beregning_et_barn.json")

            beregningResultat = beregningService.beregnBarnebidrag(beregningRequest)
        }

        @Test
        fun `skal returnere tomt resultat dersom ingen barn er oppgitt`() = runTest {
            val beregningRequest = BeregningRequestDto(
                inntektForelder1 = 500000.0,
                inntektForelder2 = 600000.0,
                barn = emptyList()
            )

            val resultat = beregningService.beregnBarnebidrag(beregningRequest)

            assertEquals(true,  resultat.resultater.isEmpty())
        }

        @Test
        fun `skal returnere ett beregningsresultat for ett barn`() {
            assertEquals(1, beregningResultat.resultater.size)
            assertEquals(beregningRequest.barn.first().ident, beregningResultat.resultater.first().ident)
        }

    }

    @Nested
    inner class BeregningBarnebidragForToBarn {

        private lateinit var beregningRequest: BeregningRequestDto
        private lateinit var beregningResultat: BeregningsresultatDto

        @BeforeEach
        fun oppsett() = runTest {
            beregningRequest = mockOppsett("/barnebidrag/beregning_to_barn.json")

            beregningResultat = beregningService.beregnBarnebidrag(beregningRequest)
        }

        @Test
        fun `skal returnere to beregningsresultater for to barn`() = runTest {
            val resultat = beregningService.beregnBarnebidrag(beregningRequest)

            assertEquals(2, resultat.resultater.size)
        }
    }

    @Nested
    inner class BeregningUnderholdskostnad {

        @Test
        fun `skal beregne underholdskostnad for en person`() {
            val beregnUnderholdskostnadRespons: List<GrunnlagDto> = JsonUtils.readJsonFile("/underholdskostnad/beregn_underholdskostnad_respons.json")
            every { underholdskostnadServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns 8471.toBigDecimal()

            val personident = Personident("29891198289")
            val forventetBeløp = BigDecimal(8471)

            val resultat = beregningService.beregnPersonUnderholdskostnad(personident)

            assertEquals(forventetBeløp, resultat)
        }

        @Test
        fun `skal returnere 0 dersom ingen underholdskostnad finnes`() {
            every { underholdskostnadServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns BigDecimal.ZERO

            val result = beregningService.beregnPersonUnderholdskostnad(Personident("29891198289"))

            assertEquals(BigDecimal.ZERO, result, "Forventet underholdskostnad skal være 0 når ingen data finnes")
        }

        @Test
        fun `skal beregne underholdskostnader for barnerelasjoner og sortere etter alder`() = runTest {
            // Arrange
            val motpartBarnRelasjon: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")
            val underholdskostnadRespons: List<GrunnlagDto> = JsonUtils.readJsonFile("/underholdskostnad/beregn_underholdskostnad_respons.json")
            val fellesBarn = motpartBarnRelasjon.personensMotpartBarnRelasjon.first().fellesBarn

            val barn1Ident = fellesBarn[0].ident
            val barn2Ident = fellesBarn[1].ident
            val forventetUnderholdskostnad = BigDecimal(8471)

            every { underholdskostnadServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns BigDecimal.ZERO
            every { beregningService.beregnPersonUnderholdskostnad(barn1Ident) } returns forventetUnderholdskostnad
            every { beregningService.beregnPersonUnderholdskostnad(barn2Ident) } returns forventetUnderholdskostnad

            // Act
            val resultat = beregningService.beregnUnderholdskostnaderForBarnerelasjoner(motpartBarnRelasjon.personensMotpartBarnRelasjon.tilFamilieRelasjon())

            // Assert
            assertEquals(1, resultat.size, "Skal være én relasjon til motpart med barn")

            val relasjon = resultat.first()
            assertEquals(2, relasjon.fellesBarn.size, "Skal være to felles barn i relasjonen")

            val forventetSortertAldre = fellesBarn
                .mapNotNull { it.fødselsdato }
                .map { kalkulerAlder(it) }
                .sortedDescending()

            val faktiskeAldre = relasjon.fellesBarn.map { it.alder }

            assertEquals(forventetSortertAldre, faktiskeAldre, "Barna skal være sortert etter alder synkende")
        }

    }

    private fun lagResultatPeriode(): ResultatPeriode {
        return ResultatPeriode(
            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            resultat = ResultatBeregning(BigDecimal(3220)),
            grunnlagsreferanseListe = emptyList()
        )
    }

    private fun mockOppsett(filNavn: String): BeregningRequestDto {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile(filNavn)

        val result: List<PersonDto> = beregningRequest.barn.mapIndexed  { index, barn ->
            PersonDto(
                ident = barn.ident,
                navn = "Navn Navnesen",
                fornavn = "Navn",
                etternavn = "Navnesen",
                dødsdato = null,
                fødselsdato = barn.ident.fødselsdato(),
                visningsnavn = "Navn Navnesen"
            )
        }

        val beregnetResultat = BeregnetBarnebidragResultat(
            beregnetBarnebidragPeriodeListe = listOf(lagResultatPeriode())
        )

        result.forEach {
            every { beregnBarnebidragApi.beregn(any()) } returns beregnetResultat
            every { personService.hentPersoninformasjon(any()) } returns it
        }

        return beregningRequest;
    }


}
