package no.nav.bidrag.bidragskalkulator.mapper

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.bidragskalkulator.dto.BarnetilsynDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.ForelderInntektDto
import no.nav.bidrag.bidragskalkulator.dto.KontantstøtteDto
import no.nav.bidrag.bidragskalkulator.dto.UtvidetBarnetrygdDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.service.SjablonService
import no.nav.bidrag.bidragskalkulator.utils.lagBarnDto
import no.nav.bidrag.bidragskalkulator.utils.lagBereningRequestDto
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
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
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertBarnetsAlderOgReferanse(result.first(), beregningRequest, 0)
    }

    @Test
    fun `skal mappe BeregningRequestDto med to barn til BeregnGrunnlag`() {
        val barn1 = lagBarnDto(alder = 1)
        val barn2 = lagBarnDto(alder = 2)
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn1, barn2),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlagMedAlder ->
            assertBarnetsAlderOgReferanse(beregnGrunnlagMedAlder, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val barn1 = lagBarnDto(alder = 1)
        val barn2 = lagBarnDto(alder = 2)
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn1, barn2),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(8, result.first().grunnlag.grunnlagListe.size, "Forventet 8 grunnlagselementer")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG18AAR for barn over 18`() {
        val barn = lagBarnDto(alder = 18)
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertEquals(Stønadstype.BIDRAG18AAR, result.first().grunnlag.stønadstype, "Stønadstype skal være BIDRAG18AAR for barn over 18")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG for barn under 18`() {
        val barn = lagBarnDto(alder = 17)
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn),
        )
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        assertEquals(Stønadstype.BIDRAG, result.first().grunnlag.stønadstype)
    }

    @Test
    fun `skal inkludere faktisk utgift grunnlag når brnetilsynsutgift er satt`() {
        val barn = lagBarnDto(alder = 1, samværklasse = Samværsklasse.SAMVÆRSKLASSE_2, barnetilsyn = BarnetilsynDto(
            BigDecimal("1200")))
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNotNull(faktiskUtgiftGrunnlag, "Forventet grunnlag for faktisk utgift til barnetilsyn")
    }

    @Test
    fun `skal ikke inkludere faktisk utgift grunnlag når brnetilsynsutgift ikke er satt`() {
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNull(faktiskUtgiftGrunnlag, "Forventet ikke grunnlag for faktisk utgift til barnetilsyn når barnetilsynsutgift ikke er satt")
    }

    @Test
    fun `skal legge kontantstøtte til BM inntekt`() {
        val kontantstøtte = BigDecimal("100")
        val barn = lagBarnDto(kontantstøtte = KontantstøtteDto(kontantstøtte, deles = false))
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

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
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
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

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        val inntektBmGrunnlag = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }

        val beløp = inntektBmGrunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().beløp

        val forventetUtvidetÅrlig = BigDecimal("2000").multiply(BigDecimal("12"))
        val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrlig

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal halvere utvidet barnetrygd når den deles med medforelder`() {
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
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

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventetUtvidetÅrligHalv = BigDecimal("2000").multiply(BigDecimal("12"))
            .divide(BigDecimal("2"))

        val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrligHalv
        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal legge småbarnstillegg til BM inntekt basert på sjablon`() {
        val beregningRequest = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
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

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventetSmåbarnÅrlig = BigDecimal("1500").multiply(BigDecimal("12"))
        val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetSmåbarnÅrlig

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal halvere kontantstøtte når deles er true og bruke full beløp når deles er false`() {
        val beløp = BigDecimal("1200")

        // deles = true
        val barnMedDeltKontantstøtte = lagBarnDto(kontantstøtte = KontantstøtteDto(beløp = beløp, deles = true))
        val requestDelt = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barnMedDeltKontantstøtte)
        )

        val resultDelt = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(requestDelt)
        val bmInntektDelt = resultDelt.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>().beløp

        val forventetDelt = requestDelt.bidragsmottakerInntekt.inntekt + beløp.multiply(BigDecimal("12")).divide(BigDecimal("2"))
        assertThat(bmInntektDelt).isEqualByComparingTo(forventetDelt)

        // deles = false
        val barnMedFullKontantstøtte = lagBarnDto(kontantstøtte = KontantstøtteDto(beløp = beløp, deles = false))
        val requestFull = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barnMedFullKontantstøtte)
        )

        val resultFull = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(requestFull)
        val bmInntektFull = resultFull.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>().beløp

        val forventetFull = requestFull.bidragsmottakerInntekt.inntekt + beløp.multiply(BigDecimal("12"))
        assertThat(bmInntektFull).isEqualByComparingTo(forventetFull)
    }

    @Test
    fun `skal ikke legge småbarnstillegg til BM inntekt når småbarnstillegg er false`() {
        val request = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(lagBarnDto()),
            småbarnstillegg = false
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

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

        val beløp = result.first().grunnlag.grunnlagListe
            .first { it.referanse == "Inntekt_Bidragsmottaker" }
            .innholdTilObjekt<InntektsrapporteringPeriode>()
            .beløp

        val forventet = request.bidragsmottakerInntekt.inntekt

        assertThat(beløp).isEqualByComparingTo(forventet)
    }

    @Test
    fun `skal legge til grunnlag for mottatt barnepassplass når barnetilsyn plassType er satt`() {
        val barn = lagBarnDto(barnetilsyn = BarnetilsynDto(
            månedligUtgift = null,
            plassType = Tilsynstype.DELTID,
        ))
        val request = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn)
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

        val barnetilsynMedStønadGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNotNull(barnetilsynMedStønadGrunnlag)
        assertNull(faktiskUtgiftGrunnlag)
    }

    @Test
    fun `skal ikke legge til barnetilsyn-grunnlag når barnetilsyn er null`() {
        val barn = lagBarnDto(barnetilsyn = null)
        val request = lagBereningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn)
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

        val barnetilsynMedStønadGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNull(barnetilsynMedStønadGrunnlag)
        assertNull(faktiskUtgiftGrunnlag)
    }


        private fun assertBarnetsAlderOgReferanse(
        grunnlagOgBarnInformasjon: PersonBeregningsgrunnlagAnonym,
        beregningRequest: ÅpenBeregningRequestDto,
        index: Int
    ) {
        assertEquals(beregningRequest.barn[index].alder, grunnlagOgBarnInformasjon.alder)
        assertEquals("Person_Søknadsbarn_$index", grunnlagOgBarnInformasjon.grunnlag.søknadsbarnReferanse)
    }
}
