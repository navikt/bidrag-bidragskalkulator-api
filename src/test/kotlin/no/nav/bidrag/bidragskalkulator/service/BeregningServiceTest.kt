package no.nav.bidrag.bidragskalkulator.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.ForelderInntektDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedFødselsdatoDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagBuilder
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.tilFamilieRelasjon
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BeregningServiceTest {

    private val beregnBarnebidragApi = mockk<BeregnBarnebidragApi>()
    private val personService = mockk<PersonService>()
    private val boOgForbruksutgiftServiceMock = mockk<BoOgForbruksutgiftService>()
    private val sjablonService = mockk<SjablonService>()

    // Ekte builder + mapper
    private val beregningsgrunnlagBuilder = BeregningsgrunnlagBuilder()
    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun setup() {
        // Vi tester ikke sjablon i denne testen – hold det stabilt
        every { sjablonService.hentSjablontall() } returns emptyList()

        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(beregningsgrunnlagBuilder, sjablonService)
        beregningService = BeregningService(
            beregnBarnebidragApi,
            boOgForbruksutgiftServiceMock,
            beregningsgrunnlagMapper,
            personService
        )
    }

    @Nested
    inner class BeregningBarnebidragForEttBarn {

        private lateinit var beregningRequest: ÅpenBeregningRequestDto
        private lateinit var beregningResultat: ÅpenBeregningsresultatDto

        @BeforeEach
        fun oppsett() = runTest {
            beregningRequest = mockOppsett(
                listOf(BarnMedFødselsdatoDto(fødselsdato = LocalDate.now().minusYears(1), samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2))
            )
            beregningResultat = beregningService.beregnBarnebidragAnonym(beregningRequest)
        }

        @Test
        fun `skal returnere tomt resultat dersom ingen barn er oppgitt`() = runTest {
            val beregningRequest = ÅpenBeregningRequestDto(
                bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
                bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
                bidragstype = BidragsType.PLIKTIG,
                barn = emptyList()
            )

            coEvery { beregnBarnebidragApi.beregnV2(any(), any()) } returns emptyList()

            val resultat = beregningService.beregnBarnebidragAnonym(beregningRequest)

            assertEquals(true, resultat.resultater.isEmpty())
        }

        @Test
        fun `skal returnere ett beregningsresultat for ett barn`() {
            assertEquals(1, beregningResultat.resultater.size)
        }

        @Test
        fun `skal mappe fødselsdato riktig fra grunnlag`() {
            val forventetFødselsdato = beregningRequest.barn.first().fødselsdato
            assertEquals(forventetFødselsdato, beregningResultat.resultater.first().fødselsdato)
        }
    }

    @Nested
    inner class BeregningBarnebidragForTreBarn {

        private lateinit var beregningRequest: ÅpenBeregningRequestDto
        private lateinit var beregningResultat: ÅpenBeregningsresultatDto
        private val fødselsdato4År = LocalDate.now().minusYears(4)
        private val fødselsdato7År = LocalDate.now().minusYears(7)

        @BeforeEach
        fun oppsett() = runTest {
            beregningRequest = mockOppsett(
                listOf(
                    BarnMedFødselsdatoDto(fødselsdato = fødselsdato4År, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2),
                    BarnMedFødselsdatoDto(fødselsdato = fødselsdato4År, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2),
                    BarnMedFødselsdatoDto(fødselsdato = fødselsdato7År, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_3)
                )
            )
            beregningResultat = beregningService.beregnBarnebidragAnonym(beregningRequest)
        }

        @Test
        fun `skal returnere tre beregningsresultater for tre barn`() = runTest {
            val resultat = beregningService.beregnBarnebidragAnonym(beregningRequest)
            assertEquals(3, resultat.resultater.size)
        }

        @Test
        fun `skal mappe fødselsdato riktig for alle barn og kunne håndtere barn med samme fødselsdato`() {
            val fødselsdatoer = beregningResultat.resultater.map { it.fødselsdato }.sorted()
            assertEquals(listOf(fødselsdato7År, fødselsdato4År, fødselsdato4År), fødselsdatoer)
        }
    }

    @Nested
    inner class BeregningUnderholdskostnad {

        @Test
        fun `skal beregne underholdskostnad for en person`() {
            every { boOgForbruksutgiftServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns 8471.toBigDecimal()

            val personident = genererPersonident()
            val forventetBeløp = BigDecimal(8471)

            val resultat = beregningService.beregnPersonUnderholdskostnad(personident)

            assertEquals(forventetBeløp, resultat)
        }

        @Test
        fun `skal returnere 0 dersom ingen underholdskostnad finnes`() {
            every { boOgForbruksutgiftServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns BigDecimal.ZERO

            val result = beregningService.beregnPersonUnderholdskostnad(genererPersonident())

            assertEquals(BigDecimal.ZERO, result, "Forventet underholdskostnad skal være 0 når ingen data finnes")
        }

        @Test
        fun `skal beregne underholdskostnader for barnerelasjoner og sortere etter alder`() = runTest {
            val motpartBarnRelasjon: MotpartBarnRelasjonDto = JsonUtils.lesJsonFil("/person/person_med_barn_en_motpart.json")
            val fellesBarn = motpartBarnRelasjon.personensMotpartBarnRelasjon.first().fellesBarn

            val forventetUnderholdskostnad = BigDecimal(8471)
            every { boOgForbruksutgiftServiceMock.beregnCachedPersonBoOgForbruksutgiftskostnad(any()) } returns forventetUnderholdskostnad

            val resultat = beregningService.beregnUnderholdskostnaderForBarnerelasjoner(
                motpartBarnRelasjon.personensMotpartBarnRelasjon.tilFamilieRelasjon()
            )

            assertEquals(1, resultat.size, "Skal være én relasjon til motpart med barn")

            val relasjon = resultat.first()
            assertEquals(2, relasjon.fellesBarn.size, "Skal være to felles barn i relasjonen")

            val forventetSortertAldre = fellesBarn
                .mapNotNull { it.fødselsdato }
                .map { kalkulerAlder(it) }
                .sortedDescending()

            val faktiskeAldre = relasjon.fellesBarn.map { it.alder }
            assertEquals(forventetSortertAldre, faktiskeAldre, "Barna skal være sortert etter alder synkende")

            assertEquals(forventetUnderholdskostnad, relasjon.fellesBarn[0].underholdskostnad)
            assertEquals(forventetUnderholdskostnad, relasjon.fellesBarn[1].underholdskostnad)
        }
    }

    private fun lagResultatPeriode(): ResultatPeriode {
        return ResultatPeriode(
            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            resultat = ResultatBeregning(BigDecimal(3220)),
            grunnlagsreferanseListe = emptyList()
        )
    }

    private fun mockOppsett(barn: List<BarnMedFødselsdatoDto>): ÅpenBeregningRequestDto {
        val beregningRequest = ÅpenBeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("800000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = barn,
            dittBoforhold = null,
            medforelderBoforhold = null,
            utvidetBarnetrygd = null,
            småbarnstillegg = false
        )

        val beregnetResultat = BeregnetBarnebidragResultat(
            beregnetBarnebidragPeriodeListe = listOf(lagResultatPeriode())
        )

        coEvery { beregnBarnebidragApi.beregnV2(any(), any()) } answers {
            val grunnlag = secondArg<List<BeregnGrunnlag>>()
            grunnlag.map { g ->
                BeregnetBarnebidragResultatV2(
                    søknadsbarnreferanse = g.søknadsbarnReferanse,
                    beregnetBarnebidragResultat = beregnetResultat
                )
            }
        }

        return beregningRequest
    }
}
