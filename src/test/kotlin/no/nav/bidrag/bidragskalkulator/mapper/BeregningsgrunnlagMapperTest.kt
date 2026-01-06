package no.nav.bidrag.bidragskalkulator.mapper

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.bidragskalkulator.dto.BarnetilsynDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.UtvidetBarnetrygdDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.service.SjablonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.person.PersonDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BeregningsgrunnlagMapperTest {

    @MockK(relaxed = true)
    lateinit var mockPersonService: PersonService

    // Bruk ekte builder
    private val mockBeregningsgrunnlagBuilder = BeregningsgrunnlagBuilder()

    @MockK
    lateinit var sjablonService: SjablonService

    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    @BeforeEach
    fun setup() {
        val fødselsdato = LocalDate.now().minusYears(10)

        every { mockPersonService.hentPersoninformasjon(any()) } returns PersonDto(
            fødselsdato = fødselsdato,
            ident = genererPersonident(),
            fornavn = "Navn",
            visningsnavn = "Navn Navnesen",
        )

        every { sjablonService.hentSjablontall() } returns emptyList()

        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(mockBeregningsgrunnlagBuilder, sjablonService)
    }

    @Test
    fun `skal mappe BeregningRequestDto med ett barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertBarnetsAlderOgReferanse(result.first(), beregningRequest, 0)
    }

    @Test
    fun `skal mappe BeregningRequestDto med to barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlagMedAlder ->
            assertBarnetsAlderOgReferanse(beregnGrunnlagMedAlder, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(8, result.first().grunnlag.grunnlagListe.size, "Forventet 8 grunnlagselementer")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG18AAR for barn over 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_barn_over_18.json", barn1Fnr = genererFødselsnummer(LocalDate.now().minusYears(19)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertEquals(Stønadstype.BIDRAG18AAR, result.first().grunnlag.stønadstype, "Stønadstype skal være BIDRAG18AAR for barn over 18")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG for barn under 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_et_barn.json", barn1Fnr = genererFødselsnummer(
            LocalDate.now().minusYears(10)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(Stønadstype.BIDRAG, result.first().grunnlag.stønadstype)
    }

    @Test
    fun `skal inkludere faktisk utgift grunnlag når brnetilsynsutgift er satt`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNotNull(faktiskUtgiftGrunnlag, "Forventet grunnlag for faktisk utgift til barnetilsyn")
    }

    @Test
    fun `skal ikke inkludere faktisk utgift grunnlag når brnetilsynsutgift ikke er satt`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_barn_over_18.json", barn1Fnr = genererFødselsnummer(LocalDate.now().minusYears(19)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNull(faktiskUtgiftGrunnlag, "Forventet ikke grunnlag for faktisk utgift til barnetilsyn når barnetilsynsutgift ikke er satt")
    }

    @Test
    fun `skal legge kontantstøtte til BM inntekt`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        // Legg kontantstøtte på alle barn (måned)
        val oppdatertRequest = beregningRequest.copy(
            barn = beregningRequest.barn.map { b ->
                b.copy(kontantstøtte = BigDecimal("100"))
            }
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(oppdatertRequest)

        // kontantstøtteTilleggBm = 100 * 12
        val forventetTilleggÅr = BigDecimal("100").multiply(BigDecimal("12"))

        val forventetBmInntekt = beregningRequest.bidragsmottakerInntekt.inntekt + forventetTilleggÅr

        val inntektBmGrunnlag = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }

        val beløp = inntektBmGrunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().beløp
        assertThat(beløp).isEqualByComparingTo(forventetBmInntekt)
    }

    @Test
    fun `skal legge utvidet barnetrygd til BM inntekt basert på sjablon`() {
        val json: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
        val request: BeregningRequestDto = json.copy(
            utvidetBarnetrygd = UtvidetBarnetrygdDto(
                harUtvidetBarnetrygd = true,
                delerMedMedforelder = false
            )
        )

        // 0042 = per måned
        every { sjablonService.hentSjablontall() } returns listOf(
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0042",
                verdi = BigDecimal("2000"),
                datoFom = null,
                datoTom = null
            )
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val inntektBmGrunnlag = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }

        val beløp = inntektBmGrunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().beløp

        val forventetUtvidetÅrlig = BigDecimal("2000").multiply(BigDecimal("12"))
        val forventet = request.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrlig

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal halvere utvidet barnetrygd når den deles med medforelder`() {
        val json: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
        val request: BeregningRequestDto = json.copy(
            utvidetBarnetrygd = UtvidetBarnetrygdDto(
                harUtvidetBarnetrygd = true,
                delerMedMedforelder = true
            )
        )

        every { sjablonService.hentSjablontall() } returns listOf(
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0042",
                verdi = BigDecimal("2000"),
                datoFom = null,
                datoTom = null
            )
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventetUtvidetÅrligHalv = BigDecimal("2000").multiply(BigDecimal("12"))
            .divide(BigDecimal("2"))

        val forventet = request.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrligHalv
        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal legge småbarnstillegg til BM inntekt basert på sjablon`() {
        val json: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
        val request: BeregningRequestDto = json.copy(
            småbarnstillegg = true
        )

        // 0032 = småbarnstillegg per måned
        every { sjablonService.hentSjablontall() } returns listOf(
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0032",
                verdi = BigDecimal("1500"),
                datoFom = null,
                datoTom = null
            )
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventetSmåbarnÅrlig = BigDecimal("1500").multiply(BigDecimal("12"))
        val forventet = request.bidragsmottakerInntekt.inntekt + forventetSmåbarnÅrlig

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal summere kontantstøtte og sjablonbaserte tillegg inn i BM inntekt`() {
        val base: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        // kontantstøtte pr måned på barnet
        val request = base.copy(
            barn = base.barn.map { it.copy(kontantstøtte = BigDecimal("100")) },
            utvidetBarnetrygd = UtvidetBarnetrygdDto(true, false),
            småbarnstillegg = true
        )

        every { sjablonService.hentSjablontall() } returns listOf(
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0042",
                verdi = BigDecimal("2000"),
                datoFom = null,
                datoTom = null
            ),
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0032",
                verdi = BigDecimal("1500"),
                datoFom = null,
                datoTom = null
            )
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val kontantstøtteÅrlig = BigDecimal("100").multiply(BigDecimal("12"))
        val utvidetÅrlig = BigDecimal("2000").multiply(BigDecimal("12"))
        val småbarnÅrlig = BigDecimal("1500").multiply(BigDecimal("12"))

        val forventet = request.bidragsmottakerInntekt.inntekt +
                kontantstøtteÅrlig + utvidetÅrlig + småbarnÅrlig

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal ikke legge småbarnstillegg til BM inntekt når småbarnstillegg er false`() {
        val base: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        val request = base.copy(
            småbarnstillegg = false,
            utvidetBarnetrygd = null,
            barn = base.barn.map { it.copy(kontantstøtte = null) }
        )

        // Sjablon finnes, men skal IKKE brukes når flagget er false
        every { sjablonService.hentSjablontall() } returns listOf(
            no.nav.bidrag.commons.service.sjablon.Sjablontall(
                typeSjablon = "0032",
                verdi = BigDecimal("1500"),
                datoFom = null,
                datoTom = null
            )
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventet = request.bidragsmottakerInntekt.inntekt

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal legge til grunnlag for mottatt barnepassplass når barnetilsyn plassType er satt`() {
        val base: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        val request = base.copy(
            barn = base.barn.map { it.copy(barnetilsyn = BarnetilsynDto(
                månedligUtgift = null,
                plassType = Tilsynstype.DELTID,
            ))
            }
        )
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val barnetilsynMedStønadGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNotNull(barnetilsynMedStønadGrunnlag)
        assertNull(faktiskUtgiftGrunnlag)
    }

    @Test
    fun `skal ikke legge til barnetilsyn-grunnlag når barnetilsyn er null`() {
        val base: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        val request = base.copy(
            barn = base.barn.map { it.copy(barnetilsyn = null)
            }
        )
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(request)

        val barnetilsynMedStønadGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNull(barnetilsynMedStønadGrunnlag)
        assertNull(faktiskUtgiftGrunnlag)
    }


        private fun assertBarnetsAlderOgReferanse(
        grunnlagOgBarnInformasjon: PersonBeregningsgrunnlag,
        beregningRequest: BeregningRequestDto,
        index: Int
    ) {
        assertEquals(beregningRequest.barn[index].ident, grunnlagOgBarnInformasjon.ident)
        assertEquals("Person_Søknadsbarn_$index", grunnlagOgBarnInformasjon.grunnlag.søknadsbarnReferanse)
    }
}
