package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_OVER18_VGS
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSPLIKTIG
import no.nav.bidrag.bidragskalkulator.utils.lagBarnDto
import no.nav.bidrag.bidragskalkulator.utils.lagBereningRequestDto
import no.nav.bidrag.bidragskalkulator.utils.lagBoforhold
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import java.math.BigDecimal
import kotlin.test.Test

class BeregningsgrunnlagBuilderTest {

    private val builder = BeregningsgrunnlagBuilder()

    @Nested
    inner class BostatusgrunnlagTest {

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig med barn under 18 som bor fast`() {
            // 2 barn under 18 som bor fast
            val medforelderBoforhold = lagBoforhold(antallBarnUnder18BorFast = 2)
            val beregningRequestMedBarnBorFast = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = medforelderBoforhold
            )

            val kontekst = lagKontekst(
                beregningRequestMedBarnBorFast,
                0,
                BigDecimal.ZERO
            )

            val result = builder.byggBostatusgrunnlag(kontekst)

            val barnUnder18BorFast = result.filter { it.referanse.startsWith("${BOSTATUS_BARN_PREFIX}${BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST}") }

            assertThat(result).isNotEmpty
            assertThat(barnUnder18BorFast).isNotEmpty
            assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
            // i beregningRequestDto har 2 barn som bor fast
            assertThat(barnUnder18BorFast.size).isEqualTo(2)
        }

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig som ikke bor med andre voksne`() {
            val medforelderBoforhold = lagBoforhold()
            val beregningRequestBorIkkeMedAndreVoksne = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = medforelderBoforhold
            )

            val kontekst = lagKontekst(beregningRequestBorIkkeMedAndreVoksne, 0)
            val result = builder.byggBostatusgrunnlag(kontekst)

            val bidragspliktigBostatus = result.first { it.referanse == BeregningsgrunnlagKonstant.BOSTATUS_BIDRAGSPLIKTIG }
            val bostatusPeriode = bidragspliktigBostatus.innholdTilObjekt<BostatusPeriode>()
            assertThat(bostatusPeriode.bostatus).isEqualTo(Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE)
        }

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig som bor med andre voksne`() {
            val medforelderBoforhold = lagBoforhold(
                voksneOver18Type = setOf(VoksneOver18Type.SAMBOER_ELLER_EKTEFELLE)
            )
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = medforelderBoforhold
            )

            val kontekst = lagKontekst(beregningRequest, 0)
            val result = builder.byggBostatusgrunnlag(kontekst)

            val bidragspliktigBostatus = result.first { it.referanse == BeregningsgrunnlagKonstant.BOSTATUS_BIDRAGSPLIKTIG }
            val bostatusPeriode = bidragspliktigBostatus.innholdTilObjekt<BostatusPeriode>()
            assertThat(bostatusPeriode.bostatus).isEqualTo(Bostatuskode.BOR_MED_ANDRE_VOKSNE)
        }

        @Test
        fun `skal bygge bostatusgrunnlag for egne barn over 18 i vgs`() {
            val medforelderBoforhold = lagBoforhold(
                voksneOver18Type = setOf(VoksneOver18Type.EGNE_BARN_OVER_18),
                antallBarnOver18Vgs = 3
            )
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = medforelderBoforhold
            )

            val kontekst = lagKontekst(beregningRequest, 0)
            val result = builder.byggBostatusgrunnlag(kontekst)

            val barnOver18Vgs = result.filter { it.referanse.startsWith("${BOSTATUS_BARN_PREFIX}${BOSTATUS_EGNE_BARN_OVER18_VGS}") }
            assertThat(barnOver18Vgs).hasSize(3)
            barnOver18Vgs.forEach {
                val bostatusPeriode = it.innholdTilObjekt<BostatusPeriode>()
                assertThat(bostatusPeriode.bostatus).isEqualTo(Bostatuskode.DOKUMENTERT_SKOLEGANG)
            }
        }

        @Test
        fun `skal bygge bostatusgrunnlag for barn under 18 og egne barn over 18 i vgs samtidig`() {
            val medforelderBoforhold = lagBoforhold(
                antallBarnUnder18BorFast = 2,
                voksneOver18Type = setOf(VoksneOver18Type.EGNE_BARN_OVER_18, VoksneOver18Type.SAMBOER_ELLER_EKTEFELLE),
                antallBarnOver18Vgs = 1
            )
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                medforelderBoforhold = medforelderBoforhold
            )

            val kontekst = lagKontekst(beregningRequest, 0)
            val result = builder.byggBostatusgrunnlag(kontekst)

            // Sjekk bostatus for voksne

            // totalt 5 grunnlag: 1 søknadsbarn bor ikke med BP, 1 BP bor med andre vokne + 2 barn under 18 + 1 barn over 18 i vgs = 5
            assertThat(result.size).isEqualTo(5)

            val bidragspliktigBostatus = result.first { it.referanse == BeregningsgrunnlagKonstant.BOSTATUS_BIDRAGSPLIKTIG }
            val bostatusPeriode = bidragspliktigBostatus.innholdTilObjekt<BostatusPeriode>()
            // voksneOver18Type inneholder SAMBOER_ELLER_EKTEFELLE
            assertThat(bostatusPeriode.bostatus).isEqualTo(Bostatuskode.BOR_MED_ANDRE_VOKSNE)

            // Sjekk barn under 18
            val barnUnder18BorFast = result.filter { it.referanse.startsWith("${BOSTATUS_BARN_PREFIX}${BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST}") }
            assertThat(barnUnder18BorFast).hasSize(2)

            // Sjekk barn over 18 i vgs
            val barnOver18Vgs = result.filter { it.referanse.startsWith("${BOSTATUS_BARN_PREFIX}${BOSTATUS_EGNE_BARN_OVER18_VGS}") }
            assertThat(barnOver18Vgs).hasSize(1)
            barnOver18Vgs.forEach {
                val bostatusPeriodeOver18 = it.innholdTilObjekt<BostatusPeriode>()
                assertThat(bostatusPeriodeOver18.bostatus).isEqualTo(Bostatuskode.DOKUMENTERT_SKOLEGANG)
            }
        }
    }

    @Nested
    inner class InntektsgrunnlagTest {
        @Test
        fun `skal bygge inntektsgrunnlag med riktige beløp og referanser`() {
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
            )

            val kontekst = lagKontekst(beregningRequest, 0)

            val result = builder.byggInntektsgrunnlag(kontekst)

            // BP inntekt og BM inntekt
            assertThat(result).hasSize(2)
            assertThat(result).anyMatch { it.referanse == INNTEKT_BIDRAGSPLIKTIG && it.gjelderReferanse == BeregningsgrunnlagMapper.Referanser.BIDRAGSPLIKTIG }
            assertThat(result).anyMatch { it.referanse == INNTEKT_BIDRAGSMOTTAKER && it.gjelderReferanse == BeregningsgrunnlagMapper.Referanser.BIDRAGSMOTTAKER }
        }

        @Test
        fun `skal legge kontantstøtte til BM inntekt i inntektsgrunnlag`() {
            val kontantstøtte = BigDecimal("12000")

            val barn = lagBarnDto(kontantstøtte = KontantstøtteDto(kontantstøtte, deles = false))
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
            )

            val kontekst = lagKontekst(beregningRequest, 0, kontantstøtte)

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bm = result.first { it.referanse == INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + kontantstøtte
            assertThat(bm).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal legge utvidet barnetrygd til BM inntekt`() {
            val utvidetBarnetrygdÅrlig = BigDecimal("24000")

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

            val kontekst = lagKontekst(
                dto = beregningRequest,
                barnIndex = 0,
                utvidetBarnetrygdÅrlig = utvidetBarnetrygdÅrlig,
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + utvidetBarnetrygdÅrlig
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal legge småbarnstillegg til BM inntekt`() {
            val småbarnstilleggÅrlig = BigDecimal("18000")

            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
                småbarnstillegg = true
            )

            val kontekst = lagKontekst(
                dto = beregningRequest,
                barnIndex = 0,
                småbarnstilleggÅrlig = småbarnstilleggÅrlig
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + småbarnstilleggÅrlig
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal summere kontantstøtte, utvidet barnetrygd og småbarnstillegg i BM inntekt`() {
            val kontantstøtte = BigDecimal("100")

            val barn = lagBarnDto(kontantstøtte = KontantstøtteDto(kontantstøtte, deles = false))
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn),
                småbarnstillegg = true,
                utvidetBarnetrygd = UtvidetBarnetrygdDto(
                    harUtvidetBarnetrygd = true,
                    delerMedMedforelder = false
                )
            )

            val kontantstøtteÅrlig = BigDecimal("1200")
            val utvidetBarnetrygdÅrlig = BigDecimal("24000")
            val småbarnstilleggÅrlig = BigDecimal("18000")

            val kontekst = lagKontekst(
                dto = beregningRequest,
                barnIndex = 0,
                kontantstøtteÅrlig = kontantstøtteÅrlig,
                utvidetBarnetrygdÅrlig = utvidetBarnetrygdÅrlig,
                småbarnstilleggÅrlig = småbarnstilleggÅrlig
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt +
                    kontantstøtteÅrlig + utvidetBarnetrygdÅrlig + småbarnstilleggÅrlig

            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal ikke legge til kapitalinntekt i BM inntekt når nettoPositivKapitalinntekt er under 10 000`() {
            val nettoPositivKapitalinntekt = BigDecimal("9000")

            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000"), nettoPositivKapitalinntekt),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
            )

            val kontekst = BeregningKontekst(
                barnReferanse = "Person_Søknadsbarn_0",
                bidragsmottakerInntekt = beregningRequest.bidragsmottakerInntekt,
                bidragspliktigInntekt = beregningRequest.bidragspliktigInntekt,
                bidragstype = beregningRequest.bidragstype,
                dittBoforhold = beregningRequest.dittBoforhold,
                medforelderBoforhold = beregningRequest.medforelderBoforhold,
                bmTilleggÅrlig = BmTilleggÅrlig(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == INNTEKT_BIDRAGSMOTTAKER }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            assertThat(bmBeløp).isEqualByComparingTo(beregningRequest.bidragsmottakerInntekt.inntekt)
        }

        @Test
        fun `skal legge til kapitalinntekt over 10 000 minus terskel i BM inntekt`() {
            val nettoPositivKapitalinntekt = BigDecimal("25000")

            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000"), nettoPositivKapitalinntekt),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(lagBarnDto()),
            )

            val kontekst = BeregningKontekst(
                barnReferanse = "Person_Søknadsbarn_0",
                bidragsmottakerInntekt = beregningRequest.bidragsmottakerInntekt,
                bidragspliktigInntekt = beregningRequest.bidragspliktigInntekt,
                bidragstype = beregningRequest.bidragstype,
                dittBoforhold = beregningRequest.dittBoforhold,
                medforelderBoforhold = beregningRequest.medforelderBoforhold,
                bmTilleggÅrlig = BmTilleggÅrlig(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventetTillegg = BigDecimal("25000") - BigDecimal("10000") // = 15000
            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + forventetTillegg
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }
    }

    @Nested
    inner class SamvaersgrunnlagTest {

        @Test
        fun `skal bygge samværsgrunnlag med korrekt klasse`() {
            val barn = lagBarnDto(alder = 1, samværklasse = Samværsklasse.SAMVÆRSKLASSE_3)
            val beregningRequest = lagBereningRequestDto(
                bmInntekt = ForelderInntektDto(BigDecimal("300000")),
                bpInntekt = ForelderInntektDto(BigDecimal("700000")),
                bidragstype = BidragsType.MOTTAKER,
                barn = listOf(barn)
            )

            val result = builder.byggSamværsgrunnlag(beregningRequest.barn.first().samværsklasse, "Person_Søknadsbarn_0")

            assertThat(result.type).isEqualTo(Grunnlagstype.SAMVÆRSPERIODE)
            assertThat(result.gjelderReferanse).isEqualTo("Person_Bidragspliktig")
            assertThat(result.gjelderBarnReferanse).isEqualTo("Person_Søknadsbarn_0")
            assertThat(result.innhold["samværsklasse"].asText()).isEqualTo("SAMVÆRSKLASSE_3")
        }
    }

    @Nested
    inner class MottattFaktiskUtgiftgrunnlagTest {

        @Test
        fun `skal bygge mottatt faktisk utgift med riktig grunnlagstype`() {
            val barn = lagBarnDto(alder = 1, samværklasse = Samværsklasse.SAMVÆRSKLASSE_3, barnetilsyn = BarnetilsynDto(
                BigDecimal("1200")))

            val resultat = builder.byggMottattFaktiskUtgift(
                barn.getEstimertFødselsdato(), "Person_Søknadsbarn_0", barn.barnetilsyn?.månedligUtgift ?: BigDecimal.ZERO
            )

            assertThat(resultat.type).isEqualTo(Grunnlagstype.FAKTISK_UTGIFT_PERIODE)
            assertThat(resultat.gjelderReferanse).isEqualTo(BeregningsgrunnlagMapper.BIDRAGSMOTTAKER)
        }
    }

    @Nested
    inner class BarnetilsynMedStønadgrunnlagTest {

        @Test
        fun `skal bygge barnetilsyn med stønad med riktig grunnlagstype og referanser`() {
            val resultat = builder.byggMottattBarnetilsyn(
                "Person_Søknadsbarn_0", Tilsynstype.DELTID
            )

            assertThat(resultat.type).isEqualTo(Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE)
            assertThat(resultat.gjelderReferanse).isEqualTo(BeregningsgrunnlagMapper.BIDRAGSMOTTAKER)
        }
    }

    private fun lagKontekst(dto: ÅpenBeregningRequestDto,
                            barnIndex: Int,
                            kontantstøtteÅrlig: BigDecimal = BigDecimal.ZERO,
                            utvidetBarnetrygdÅrlig: BigDecimal = BigDecimal.ZERO,
                            småbarnstilleggÅrlig: BigDecimal = BigDecimal.ZERO
    ): BeregningKontekst {
        return BeregningKontekst(
            barnReferanse = "Person_Søknadsbarn_$barnIndex",
            bidragsmottakerInntekt = dto.bidragsmottakerInntekt,
            bidragspliktigInntekt = dto.bidragspliktigInntekt,
            bidragstype = dto.bidragstype,
            dittBoforhold = dto.dittBoforhold,
            medforelderBoforhold = dto.medforelderBoforhold,
            bmTilleggÅrlig = BmTilleggÅrlig(
                kontantstøtteÅrlig = kontantstøtteÅrlig,
                utvidetBarnetrygdÅrlig = utvidetBarnetrygdÅrlig,
                småbarnstilleggÅrlig = småbarnstilleggÅrlig
            )
        )
    }
}
