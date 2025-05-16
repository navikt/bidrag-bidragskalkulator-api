package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BeregningsgrunnlagBuilderTest {

    private val builder = BeregningsgrunnlagBuilder()

    @Nested
    inner class BostatusgrunnlagTest {

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig med barn som bor fast`() {
            val beregningRequestMedBarnBorFast: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

            val kontekst = lagKontekst(beregningRequestMedBarnBorFast, 0, "Person_Søknadsbarn_0")

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
            val beregningRequestDeltBolig: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

            val kontekst = lagKontekst(beregningRequestDeltBolig, 0, "Person_Søknadsbarn_0")

            val result = builder.byggBostatusgrunnlag(kontekst)
            val deltBoligGrunnlag = result.find { it.type == Grunnlagstype.BOSTATUS_PERIODE }?.innholdTilObjekt<BostatusPeriode>()?.bostatus == Bostatuskode.BOR_MED_ANDRE_VOKSNE

            assertThat(result).isNotEmpty
            assertNotNull(deltBoligGrunnlag)
            assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
        }

        @Test
        fun `skal bygge bostatusgrunnlag for bidragspliktig som bor alene`() {
            val beregningRequestBorIkkeMedAndreVoksne: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_to_barn.json")

            val kontekst = lagKontekst(beregningRequestBorIkkeMedAndreVoksne, 0, "Person_Søknadsbarn_0")

            val result = builder.byggBostatusgrunnlag(kontekst)
            val borIkkeMedAndreVoksneGrunnlag = result
                .find { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE }

            assertThat(result).isNotEmpty
            assertNotNull(borIkkeMedAndreVoksneGrunnlag)
            assertThat(result).anyMatch { it.referanse == "Bostatus_Bidragspliktig" }
        }

        @Test
        fun `skal ikke lage grunnlag for barn i samme husstand hvis personen ikke har barn som bor fast`() {
            val beregningRequestIngenBarnBorFast: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_to_barn.json")

            val kontekst = lagKontekst(beregningRequestIngenBarnBorFast, 0, "Person_Søknadsbarn_0")

            val result = builder.byggBostatusgrunnlag(kontekst)
            val harBarnBorFast = result
                .find { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }

            assertNull(harBarnBorFast)
        }

        @Test
        fun `skal bygge bostatusgrunnlag korrekt når samme person er både bidragspliktig og bidragsmottaker`() {
            val request: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_person_er_baade_bp_og_bm.json")

            val bostatusgrunnlagAlleBarn = request.barn.flatMapIndexed { index, barn ->
                val kontekst = Beregningskontekst(
                    dto = request,
                    barn = barn,
                    barnReferanse = "Person_Søknadsbarn_$index",
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
        fun `skal bruke dinBoforhold for bidragspliktig og medforelderBoforhold for bidragsmottaker`() {
            val request: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_person_er_baade_bp_og_bm.json")

            // barn indeks 0 = personen er bidragspliktig
            val kontekstPliktig = lagKontekst(request, 0, "Person_Søknadsbarn_0")
            // barn indeks 1 = personen er bidragsmottaker
            val kontekstMottaker = lagKontekst(request, 1, "Person_Søknadsbarn_1")

            val grunnlagPliktig = builder.byggBostatusgrunnlag(kontekstPliktig)
            val grunnlagMottaker = builder.byggBostatusgrunnlag(kontekstMottaker)

            val bostatusPliktig = grunnlagPliktig.filter { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }
            val bostatusMottaker = grunnlagMottaker.filter { it.innholdTilObjekt<BostatusPeriode>().bostatus == Bostatuskode.MED_FORELDER }

            assertThat(bostatusPliktig.size)
                .withFailMessage("Forventet antall barn fra dinBoforhold å være ${request.dinBoforhold?.antallBarnBorFast}")
                .isEqualTo(request.dinBoforhold?.antallBarnBorFast)

            assertThat(bostatusMottaker.size)
                .withFailMessage("Forventet antall barn fra medforelderBoforhold å være ${request.medforelderBoforhold?.antallBarnBorFast}")
                .isEqualTo(request.medforelderBoforhold?.antallBarnBorFast)

        }
    }

    @Nested
    inner class InntektsgrunnlagTest {
        @Test
        fun `skal bygge inntektsgrunnlag med riktige beløp og referanser`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")
            val kontekst = lagKontekst(beregningRequest, 0, "Person_Søknadsbarn_0")

            val result = builder.byggInntektsgrunnlag(kontekst)

            assertThat(result).hasSize(3)
            assertThat(result).anyMatch { it.referanse == "Inntekt_Bidragspliktig" && it.gjelderReferanse == "Person_Bidragspliktig" }
            assertThat(result).anyMatch { it.referanse == "Inntekt_Bidragsmottaker" && it.gjelderReferanse == "Person_Bidragsmottaker" }
            assertThat(result).anyMatch { it.referanse == "Inntekt_Person_Søknadsbarn_0" && it.gjelderReferanse == "Person_Søknadsbarn_0" }
        }
    }

    @Nested
    inner class SamvaersgrunnlagTest {

        @Test
        fun `skal bygge samværsgrunnlag med korrekt klasse og referanser`() {
            val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

            val result = builder.byggSamværsgrunnlag(beregningRequest.barn.first(), "Person_Søknadsbarn_0")

            assertThat(result.type).isEqualTo(Grunnlagstype.SAMVÆRSPERIODE)
            assertThat(result.gjelderReferanse).isEqualTo("Person_Bidragspliktig")
            assertThat(result.gjelderBarnReferanse).isEqualTo("Person_Søknadsbarn_0")
            assertThat(result.innhold["samværsklasse"].asText()).isEqualTo("SAMVÆRSKLASSE_1")
        }
    }

    private fun lagKontekst(dto: BeregningRequestDto, barnIndex: Int, referanse: String): Beregningskontekst {
        return Beregningskontekst(
            dto = dto,
            barn = dto.barn[barnIndex],
            barnReferanse = referanse
        )
    }
}