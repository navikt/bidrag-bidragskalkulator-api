package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BeregningsgrunnlagBuilderTest {

    private val builder = BeregningsgrunnlagBuilder()

    @Nested
    inner class BostatusgrunnlagTest {

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig med barn som bor fast`() {
            val beregningRequestMedBarnBorFast: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontekst = lagKontekst(beregningRequestMedBarnBorFast, 0, beregningRequestMedBarnBorFast.barn.first().bidragstype,
                BigDecimal.ZERO)

            val result = builder.byggBostatusgrunnlag(kontekst)

            val barnBoFast = result.filter { it.referanse.startsWith("Bostatus_med_forelder_") }
            val barnDeltBosted = result.filter { it.referanse.startsWith("Bostatus_delt_bosted_") }

            assertThat(result).isNotEmpty
            assertThat(barnBoFast).isNotEmpty
            assertThat(barnDeltBosted).isNotEmpty
            assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
            // i beregningRequestDto har 2 barn som bor fast
            assertThat(barnBoFast.size).isEqualTo(2)
            // i beregningRequestDto har 1 barn med delt bosted
            assertThat(barnDeltBosted.size).isEqualTo(1)
        }

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig med delt bolig med annen voksen`() {
            val beregningRequestDeltBolig: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontekst = lagKontekst(beregningRequestDeltBolig, 0, beregningRequestDeltBolig.barn.first().bidragstype,
                BigDecimal.ZERO)

            val result = builder.byggBostatusgrunnlag(kontekst)
            val deltBoligGrunnlag = result.find { it.type == Grunnlagstype.BOSTATUS_PERIODE }?.innholdTilObjekt<BostatusPeriode>()?.bostatus == Bostatuskode.BOR_MED_ANDRE_VOKSNE

            assertThat(result).isNotEmpty
            assertNotNull(deltBoligGrunnlag)
            assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
        }

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig som bor alene`() {
            val beregningRequestBorIkkeMedAndreVoksne: BeregningRequestDto =
                JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

            beregningRequestBorIkkeMedAndreVoksne.barn.map {
                val kontekst = lagKontekst(beregningRequestBorIkkeMedAndreVoksne, 0, it.bidragstype,
                    BigDecimal.ZERO)
                val result = builder.byggBostatusgrunnlag(kontekst)
                val borIkkeMedAndreVoksneGrunnlag = result
                    .find { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE }

                assertThat(result).isNotEmpty
                assertNotNull(borIkkeMedAndreVoksneGrunnlag)
                assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
            }
        }

        @Test
        fun `skal ikke lage grunnlag for barn i samme husstand hvis personen ikke har barn som bor fast`() {
            val beregningRequestIngenBarnBorFast: BeregningRequestDto =
                JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

            beregningRequestIngenBarnBorFast.barn.map {
                val kontekst = lagKontekst(beregningRequestIngenBarnBorFast, 0, it.bidragstype,
                    BigDecimal.ZERO)
                val result = builder.byggBostatusgrunnlag(kontekst)
                val harBarnBorFast = result
                    .find { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }

                assertNull(harBarnBorFast)
            }
        }


        @Test
        fun `skal bygge bostatusgrunnlag korrekt når samme person er både bidragspliktig og bidragsmottaker`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_person_er_baade_bp_og_bm.json")

            val bostatusgrunnlagAlleBarn = request.barn.flatMapIndexed { index, barn ->
                val kontekst = BeregningKontekst(
                    barnReferanse = "Person_Søknadsbarn_$index",
                    bidragsmottakerInntekt = request.bidragsmottakerInntekt,
                    bidragspliktigInntekt = request.bidragspliktigInntekt,
                    bidragstype = barn.bidragstype,
                    dittBoforhold = request.dittBoforhold,
                    medforelderBoforhold = request.medforelderBoforhold,
                    bmTilleggÅrlig = BmTilleggÅrlig(
                        kontantstøtteÅrlig = BigDecimal.ZERO,
                        utvidetBarnetrygdÅrlig = BigDecimal.ZERO,
                        småbarnstilleggÅrlig = BigDecimal.ZERO
                    )
                )

                builder.byggBostatusgrunnlag(kontekst)
            }

            // Verifiser at vi har generert grunnlag for begge barn og at det finnes bostatus for bidragspliktig
            val grunnlagReferanser = bostatusgrunnlagAlleBarn.map { it.referanse }

            assertThat(bostatusgrunnlagAlleBarn).isNotEmpty
            assertThat(grunnlagReferanser).anyMatch { it.contains("Bostatus_Bidragspliktig") }
            assertThat(grunnlagReferanser).anyMatch { it.contains("Bostatus_Person_Søknadsbarn_0") }
            assertThat(grunnlagReferanser).anyMatch { it.contains("Bostatus_Person_Søknadsbarn_1") }
        }

        @Test
        fun `skal bruke dittBoforhold for bidragspliktig og medforelderBoforhold for bidragsmottaker`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_person_er_baade_bp_og_bm.json")

            // barn indeks 0 = personen er bidragspliktig
            val kontekstPliktig = lagKontekst(request, 0, request.barn[0].bidragstype,
                BigDecimal.ZERO)
            // barn indeks 1 = personen er bidragsmottaker
            val kontekstMottaker = lagKontekst(request, 1, request.barn[1].bidragstype,
                BigDecimal.ZERO)

            val grunnlagPliktig = builder.byggBostatusgrunnlag(kontekstPliktig)
            val grunnlagMottaker = builder.byggBostatusgrunnlag(kontekstMottaker)

            val bostatusPliktig = grunnlagPliktig.filter { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }
            val bostatusMottaker = grunnlagMottaker.filter { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }

            assertThat(bostatusPliktig.size)
                .withFailMessage("Forventet antall barn fra dittBoforhold å være ${request.dittBoforhold?.antallBarnBorFast}")
                .isEqualTo(request.dittBoforhold?.antallBarnBorFast)

            assertThat(bostatusMottaker.size)
                .withFailMessage("Forventet antall barn fra medforelderBoforhold å være ${request.medforelderBoforhold?.antallBarnBorFast}")
                .isEqualTo(request.medforelderBoforhold?.antallBarnBorFast)

        }
    }

    @Nested
    inner class InntektsgrunnlagTest {
        @Test
        fun `skal bygge inntektsgrunnlag med riktige beløp og referanser`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
            val kontekst = lagKontekst(beregningRequest, 0, beregningRequest.barn.first().bidragstype,
                BigDecimal.ZERO)

            val result = builder.byggInntektsgrunnlag(kontekst)

            // BP inntekt og BM inntekt
            assertThat(result).hasSize(2)
            assertThat(result).anyMatch { it.referanse == "Inntekt_Bidragspliktig" && it.gjelderReferanse == "Person_Bidragspliktig" }
            assertThat(result).anyMatch { it.referanse == "Inntekt_Bidragsmottaker" && it.gjelderReferanse == "Person_Bidragsmottaker" }
        }

        @Test
        fun `skal legge kontantstøtte til BM inntekt i inntektsgrunnlag`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontantstøtte = BigDecimal("12000")
            val kontekst = lagKontekst(beregningRequest, 0, beregningRequest.barn.first().bidragstype, kontantstøtte)

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bm = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = beregningRequest.bidragsmottakerInntekt.inntekt + kontantstøtte
            assertThat(bm).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal legge kontantstøtte til BM inntekt også når bidragstype er MOTTAKER`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_person_er_baade_bp_og_bm.json")

            val kontantstøtte = BigDecimal("240")
            // barn[1] er MOTTAKER => BM er forelder1 i builder-logikken din
            val kontekst = lagKontekst(request, 1, request.barn[1].bidragstype, kontantstøtte)

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = request.bidragsmottakerInntekt.inntekt + kontantstøtte
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal legge utvidet barnetrygd til BM inntekt`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val utvidetBarnetrygdÅrlig = BigDecimal("24000")
            val kontekst = lagKontekst(
                dto = request,
                barnIndex = 0,
                bidragstype = request.barn.first().bidragstype,
                kontantstøtteÅrlig = BigDecimal.ZERO,
                utvidetBarnetrygdÅrlig = utvidetBarnetrygdÅrlig,
                småbarnstilleggÅrlig = BigDecimal.ZERO
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = request.bidragsmottakerInntekt.inntekt + utvidetBarnetrygdÅrlig
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal legge småbarnstillegg til BM inntekt`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val småbarnstilleggÅrlig = BigDecimal("18000")
            val kontekst = lagKontekst(
                dto = request,
                barnIndex = 0,
                bidragstype = request.barn.first().bidragstype,
                kontantstøtteÅrlig = BigDecimal.ZERO,
                utvidetBarnetrygdÅrlig = BigDecimal.ZERO,
                småbarnstilleggÅrlig = småbarnstilleggÅrlig
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = request.bidragsmottakerInntekt.inntekt + småbarnstilleggÅrlig
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal summere kontantstøtte, utvidet barnetrygd og småbarnstillegg i BM inntekt`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontantstøtteÅrlig = BigDecimal("1200")
            val utvidetBarnetrygdÅrlig = BigDecimal("24000")
            val småbarnstilleggÅrlig = BigDecimal("18000")

            val kontekst = lagKontekst(
                dto = request,
                barnIndex = 0,
                bidragstype = request.barn.first().bidragstype,
                kontantstøtteÅrlig = kontantstøtteÅrlig,
                utvidetBarnetrygdÅrlig = utvidetBarnetrygdÅrlig,
                småbarnstilleggÅrlig = småbarnstilleggÅrlig
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventet = request.bidragsmottakerInntekt.inntekt +
                    kontantstøtteÅrlig + utvidetBarnetrygdÅrlig + småbarnstilleggÅrlig

            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }

        @Test
        fun `skal ikke legge til kapitalinntekt i BM inntekt når nettoPositivKapitalinntekt er under 10 000`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontekst = BeregningKontekst(
                barnReferanse = "Person_Søknadsbarn_0",
                bidragsmottakerInntekt = request.bidragsmottakerInntekt.copy(
                    nettoPositivKapitalinntekt = BigDecimal("9000")
                ),
                bidragspliktigInntekt = request.bidragspliktigInntekt,
                bidragstype = request.barn.first().bidragstype,
                dittBoforhold = request.dittBoforhold,
                medforelderBoforhold = request.medforelderBoforhold,
                bmTilleggÅrlig = BmTilleggÅrlig(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            assertThat(bmBeløp).isEqualByComparingTo(request.bidragsmottakerInntekt.inntekt)
        }

        @Test
        fun `skal legge til kapitalinntekt over 10 000 minus terskel i BM inntekt`() {
            val request: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val kontekst = BeregningKontekst(
                barnReferanse = "Person_Søknadsbarn_0",
                bidragsmottakerInntekt = request.bidragsmottakerInntekt.copy(
                    nettoPositivKapitalinntekt = BigDecimal("25000")
                ),
                bidragspliktigInntekt = request.bidragspliktigInntekt,
                bidragstype = request.barn.first().bidragstype,
                dittBoforhold = request.dittBoforhold,
                medforelderBoforhold = request.medforelderBoforhold,
                bmTilleggÅrlig = BmTilleggÅrlig(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            )

            val result = builder.byggInntektsgrunnlag(kontekst)

            val bmBeløp = result.first { it.referanse == "Inntekt_Bidragsmottaker" }
                .innholdTilObjekt<InntektsrapporteringPeriode>()
                .beløp

            val forventetTillegg = BigDecimal("25000") - BigDecimal("10000") // = 15000
            val forventet = request.bidragsmottakerInntekt.inntekt + forventetTillegg
            assertThat(bmBeløp).isEqualByComparingTo(forventet)
        }
    }

    @Nested
    inner class SamvaersgrunnlagTest {

        @Test
        fun `skal bygge samværsgrunnlag med korrekt klasse og referanser`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

            val result = builder.byggSamværsgrunnlag(beregningRequest.barn.first().samværsklasse, "Person_Søknadsbarn_0")

            assertThat(result.type).isEqualTo(Grunnlagstype.SAMVÆRSPERIODE)
            assertThat(result.gjelderReferanse).isEqualTo("Person_Bidragspliktig")
            assertThat(result.gjelderBarnReferanse).isEqualTo("Person_Søknadsbarn_0")
            assertThat(result.innhold["samværsklasse"].asText()).isEqualTo("SAMVÆRSKLASSE_1")
        }
    }

    @Nested
    inner class MottattFaktiskUtgiftgrunnlagTest {

        @Test
        fun `skal bygge mottatt faktisk utgift med riktig grunnlagstype og referanser`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
            val barn = beregningRequest.barn.first()

            val resultat = builder.byggMottattFaktiskUtgift(
                barn.ident.fødselsdato(), "Person_Søknadsbarn_0", barn.barnetilsyn?.månedligUtgift ?: BigDecimal.ZERO
            )

            assertThat(resultat.type).isEqualTo(Grunnlagstype.FAKTISK_UTGIFT_PERIODE)
            assertThat(resultat.gjelderReferanse).isEqualTo(BeregningsgrunnlagMapper.BIDRAGSMOTTAKER)
        }
    }

    private fun lagKontekst(dto: BeregningRequestDto,
                            barnIndex: Int,
                            bidragstype: BidragsType,
                            kontantstøtteÅrlig: BigDecimal = BigDecimal.ZERO,
                            utvidetBarnetrygdÅrlig: BigDecimal = BigDecimal.ZERO,
                            småbarnstilleggÅrlig: BigDecimal = BigDecimal.ZERO
    ): BeregningKontekst {
        return BeregningKontekst(
            barnReferanse = "Person_Søknadsbarn_$barnIndex",
            bidragsmottakerInntekt = dto.bidragsmottakerInntekt,
            bidragspliktigInntekt = dto.bidragspliktigInntekt,
            bidragstype = bidragstype,
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
