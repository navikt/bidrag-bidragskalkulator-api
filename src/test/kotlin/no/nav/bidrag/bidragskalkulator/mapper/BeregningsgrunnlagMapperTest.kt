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
import no.nav.bidrag.bidragskalkulator.service.SjablonService
import no.nav.bidrag.bidragskalkulator.utils.lagBarnDto
import no.nav.bidrag.bidragskalkulator.utils.lagBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.utils.lagBoforhold
import no.nav.bidrag.commons.service.sjablon.Sjablontall
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BeregningsgrunnlagMapperTest {

    // Bruk ekte builder
    private val beregningsgrunnlagBuilder = BeregningsgrunnlagBuilder()

    @MockK
    lateinit var sjablonService: SjablonService

    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    @BeforeEach
    fun setup() {
        every { sjablonService.hentSjablontall() } returns emptyList()
        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(beregningsgrunnlagBuilder, sjablonService)
    }

    @Test
    fun `mapTilBoOgForbruksutgiftsgrunnlag skal bygge grunnlag med BM BP og søknadsbarn`() {
        val fødselsdato = LocalDate.now().minusYears(5)
        val barnRef = "${BeregningsgrunnlagMapper.Referanser.SØKNADSBARN}1"

        val result = beregningsgrunnlagMapper.mapTilBoOgForbruksutgiftsgrunnlag(fødselsdato, barnRef)

        assertEquals(barnRef, result.søknadsbarnReferanse)

        val typer = result.grunnlagListe.map { it.type }.toSet()
        assertThat(typer).contains(
            Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
            Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
            Grunnlagstype.PERSON_SØKNADSBARN
        )
    }

    @Test
    fun `skal mappe ÅpenBeregningRequestDto med ett barn til BeregnGrunnlag`() {
        val beregningRequest = lagBeregningRequestDto(
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
    fun `skal mappe ÅpenBeregningRequestDto med to barn til BeregnGrunnlag`() {
        val barn1 = lagBarnDto(fødselsdato = LocalDate.now().minusYears(1))
        val barn2 = lagBarnDto(fødselsdato = LocalDate.now().minusYears(2))
        val beregningRequest = lagBeregningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn1, barn2),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlag ->
            assertBarnetsAlderOgReferanse(beregnGrunnlag, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val barn1 = lagBarnDto(fødselsdato = LocalDate.now().minusYears(1))
        val barn2 = lagBarnDto(fødselsdato = LocalDate.now().minusYears(2))
        val beregningRequest = lagBeregningRequestDto(
            bmInntekt = ForelderInntektDto(BigDecimal("300000")),
            bpInntekt = ForelderInntektDto(BigDecimal("700000")),
            bidragstype = BidragsType.MOTTAKER,
            barn = listOf(barn1, barn2),
        )

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(8, result.first().grunnlagListe.size, "Forventet 8 grunnlagselementer")
    }

    @Nested
    inner class StønadstypeGrunnlag {
        @Test
        fun `skal sette stønadstype til BIDRAG18AAR for barn over 18`() {
            val barn = lagBarnDto(fødselsdato = LocalDate.now().minusYears(18))
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            assertEquals(1, result.size, "Forventet én beregning")
            assertEquals(
                Stønadstype.BIDRAG18AAR,
                result.first().stønadstype,
                "Stønadstype skal være BIDRAG18AAR for barn over 18"
            )
        }

        @Test
        fun `skal sette stønadstype til BIDRAG for barn under 18`() {
            val barn = lagBarnDto(fødselsdato = LocalDate.now().minusYears(17))
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            assertEquals(Stønadstype.BIDRAG, result.first().stønadstype)
        }
    }

    @Nested
    inner class FaktiskUtgiftBarnetilsyn {
        @Test
        fun `skal inkludere faktisk utgift grunnlag når barnetilsynsutgift er satt`() {
            val barn = lagBarnDto(
                fødselsdato = LocalDate.now().minusYears(1),
                samværklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                barnetilsyn = BarnetilsynDto(BigDecimal("1200"))
            )
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
            val faktiskUtgiftGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

            assertNotNull(faktiskUtgiftGrunnlag, "Forventet grunnlag for faktisk utgift til barnetilsyn")
        }

        @Test
        fun `skal ikke inkludere faktisk utgift grunnlag når barnetilsynsutgift ikke er satt`() {
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
            val faktiskUtgiftGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

            assertNull(
                faktiskUtgiftGrunnlag,
                "Forventet ikke grunnlag for faktisk utgift til barnetilsyn når barnetilsynsutgift ikke er satt"
            )
        }

        @Test
        fun `skal legge til grunnlag for mottatt barnepassplass når barnetilsyn plassType er satt`() {
            val barn = lagBarnDto(
                barnetilsyn = BarnetilsynDto(
                    månedligUtgift = null,
                    plassType = Tilsynstype.DELTID,
                )
            )
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn)
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val barnetilsynMedStønadGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

            val faktiskUtgiftGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

            assertNotNull(barnetilsynMedStønadGrunnlag)
            assertNull(faktiskUtgiftGrunnlag)
        }

        @Test
        fun `skal ikke legge til barnetilsyn-grunnlag når barnetilsyn er null`() {
            val barn = lagBarnDto(barnetilsyn = null)
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn)
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val barnetilsynMedStønadGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE }

            val faktiskUtgiftGrunnlag = result.first().grunnlagListe
                .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

            assertNull(barnetilsynMedStønadGrunnlag)
            assertNull(faktiskUtgiftGrunnlag)
        }
    }

    @Nested
    inner class Kontantstøtte {
        @Test
        fun `skal legge kontantstøtte til BM inntekt`() {
            val kontantstøtte = BigDecimal("100")
            val barn = lagBarnDto(kontantstøtte = KontantstøtteDto(kontantstøtte, deles = false))
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            // kontantstøtteTilleggBm = 100 * 12
            val forventetTilleggÅr = BigDecimal("100").multiply(BigDecimal("12"))
            val forventetBmInntekt = beregningRequest.bidragsmottakerInntekt.inntekt + forventetTilleggÅr

            val inntektBmGrunnlag = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }

            val beløp = inntektBmGrunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().beløp
            assertThat(beløp).isEqualByComparingTo(forventetBmInntekt)
        }

        @Test
        fun `skal halvere kontantstøtte når deles er true og bruke full beløp når deles er false`() {
            val beløp = BigDecimal("1200")

            // deles = true
            val barnMedDeltKontantstøtte = lagBarnDto(kontantstøtte = KontantstøtteDto(beløp = beløp, deles = true))
            val requestDelt = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barnMedDeltKontantstøtte)
            )

            val resultDelt = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(requestDelt)
            val bmInntektDelt = resultDelt.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>().beløp

            val forventetDelt =
                requestDelt.bidragsmottakerInntekt.inntekt + beløp.multiply(BigDecimal("12")).divide(BigDecimal("2"))
            assertThat(bmInntektDelt).isEqualByComparingTo(forventetDelt)

            // deles = false
            val barnMedFullKontantstøtte = lagBarnDto(kontantstøtte = KontantstøtteDto(beløp = beløp, deles = false))
            val requestFull = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barnMedFullKontantstøtte)
            )

            val resultFull = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(requestFull)
            val bmInntektFull = resultFull.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>().beløp

            val forventetFull = requestFull.bidragsmottakerInntekt.inntekt + beløp.multiply(BigDecimal("12"))
            assertThat(bmInntektFull).isEqualByComparingTo(forventetFull)
        }
    }

    @Nested
    inner class Småbarnstillegg {
        @Test
        fun `skal legge småbarnstillegg til BM inntekt basert på sjablon`() {
            val beregningRequest = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                småbarnstillegg = true
            )

            // 0032 = småbarnstillegg per måned
            every { sjablonService.hentSjablontall() } returns listOf(
                Sjablontall(
                    typeSjablon = "0032",
                    verdi = BigDecimal("1500"),
                    datoFom = null,
                    datoTom = null
                )
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            val beløp = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventetSmåbarnÅrlig = BigDecimal("1500").multiply(BigDecimal("12"))
            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetSmåbarnÅrlig

            assertThat(beløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal ikke legge småbarnstillegg til BM inntekt når småbarnstillegg er false`() {
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                småbarnstillegg = false
            )

            // Sjablon finnes, men skal IKKE brukes når flagget er false
            every { sjablonService.hentSjablontall() } returns listOf(
                Sjablontall(
                    typeSjablon = "0032",
                    verdi = BigDecimal("1500"),
                    datoFom = null,
                    datoTom = null
                )
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val beløp = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = request.bidragsmottakerInntekt.inntekt

            assertThat(beløp).isEqualByComparingTo(forventet)
        }
    }

    @Nested
    inner class BarnInntekt {
        @Test
        fun `skal inkludere barnets inntekt grunnlag når barn inntekt er satt`() {
            val barn = lagBarnDto(inntekt = BigDecimal("5000"))
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val barnRef = "${BeregningsgrunnlagMapper.Referanser.SØKNADSBARN}${request.barn.first().fødselsdato}_0"
            val barnInntekt = result.first().grunnlagListe
                .find { it.referanse == "${BeregningsgrunnlagKonstant.INNTEKT_PREFIX}$barnRef" }

            assertNotNull(barnInntekt)
            assertEquals(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE, barnInntekt!!.type)
        }

        @Test
        fun `skal ikke inkludere barnets inntekt grunnlag når barn inntekt er null`() {
            val barn = lagBarnDto(inntekt = null)
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val barnRef = "${BeregningsgrunnlagMapper.Referanser.SØKNADSBARN}${request.barn.first().fødselsdato}"
            val barnInntekt = result.first().grunnlagListe
                .find { it.referanse == "${BeregningsgrunnlagKonstant.INNTEKT_PREFIX}$barnRef" }

            assertNull(barnInntekt)
        }
    }

    @Nested
    inner class UtvidetBarnetrygd {
        @Test
        fun `skal ikke legge utvidet barnetrygd til BM inntekt når harUtvidetBarnetrygd er false selv om delesMedMedforelder er true`() {
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                utvidetBarnetrygd = UtvidetBarnetrygdDto(
                    harUtvidetBarnetrygd = false,
                    delerMedMedforelder = true
                )
            )

            every { sjablonService.hentSjablontall() } returns listOf(
                Sjablontall(
                    typeSjablon = "0042",
                    verdi = BigDecimal("2000"),
                    datoFom = null,
                    datoTom = null
                )
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)

            val bmBeløp = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            assertThat(bmBeløp).isEqualByComparingTo(request.bidragsmottakerInntekt.inntekt)
        }

        @Test
        fun `skal legge utvidet barnetrygd til BM inntekt basert på sjablon`() {
            val beregningRequest = lagBeregningRequestDto(
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
                Sjablontall(
                    typeSjablon = "0042",
                    verdi = BigDecimal("2000"),
                    datoFom = null,
                    datoTom = null
                )
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            val inntektBmGrunnlag = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }

            val beløp = inntektBmGrunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().beløp

            val forventetUtvidetÅrlig = BigDecimal("2000").multiply(BigDecimal("12"))
            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrlig

            assertThat(beløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal halvere utvidet barnetrygd når den deles med medforelder`() {
            val beregningRequest = lagBeregningRequestDto(
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
                Sjablontall(
                    typeSjablon = "0042",
                    verdi = BigDecimal("2000"),
                    datoFom = null,
                    datoTom = null
                )
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

            val beløp = result.first().grunnlagListe
                .first { it.referanse == BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventetUtvidetÅrligHalv = BigDecimal("2000").multiply(BigDecimal("12"))
                .divide(BigDecimal("2"))

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetUtvidetÅrligHalv
            assertThat(beløp).isEqualByComparingTo(forventet)
        }
    }

    @Nested
    inner class Boforhold {
        @Test
        fun `skal alltid inkludere bostatus grunnlag for bidragspliktig og søknadsbarn`() {
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = lagBoforhold(antallBarnUnder18BorFast = 0)
            )

            val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request)
            val grunnlag = result.first().grunnlagListe

            val barnRef = "${BeregningsgrunnlagMapper.Referanser.SØKNADSBARN}${request.barn.first().fødselsdato}_0"

            // Mapperen skal alltid legge inn disse to bostatusene:
            assertThat(grunnlag).anyMatch { it.referanse == BeregningsgrunnlagKonstant.BOSTATUS_BIDRAGSPLIKTIG }
            assertThat(grunnlag).anyMatch { it.referanse == "${BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX}$barnRef" }
        }

        @Test
        fun `når bidragstype er MOTTAKER skal bostatus bygge egne barn under 18 fra medforelderBoforhold`() {
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = lagBoforhold(antallBarnUnder18BorFast = 2),
                dittBoforhold = lagBoforhold(antallBarnUnder18BorFast = 0),
            )

            val grunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request).first().grunnlagListe

            val under18BorFast = grunnlag.filter {
                it.referanse.startsWith(
                    "${BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX}${BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST}"
                )
            }

            assertThat(under18BorFast).hasSize(2)
        }

        @Test
        fun `når bidragstype er PLIKTIG skal bostatus bygge egne barn under 18 fra dittBoforhold`() {
            val request = lagBeregningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.PLIKTIG,
                barn = listOf(lagBarnDto()),
                dittBoforhold = lagBoforhold(antallBarnUnder18BorFast = 3),
                medforelderBoforhold = lagBoforhold(antallBarnUnder18BorFast = 0),
            )

            val grunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(request).first().grunnlagListe

            val under18BorFast = grunnlag.filter {
                it.referanse.startsWith(
                    "${BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX}${BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST}"
                )
            }

            assertThat(under18BorFast).hasSize(3)
        }
    }

    private fun assertBarnetsAlderOgReferanse(
        grunnlag: BeregnGrunnlag,
        beregningRequest: ÅpenBeregningRequestDto,
        index: Int
    ) {
        val forventetAlder = beregningRequest.barn[index].fødselsdato
        assertEquals("${BeregningsgrunnlagMapper.Referanser.SØKNADSBARN}${forventetAlder}_${index}", grunnlag.søknadsbarnReferanse)
    }
}
