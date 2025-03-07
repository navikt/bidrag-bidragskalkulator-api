package no.nav.bidrag.bidragskalkulator.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.YearMonth

@Component
class BeregnGrunnlagMapper {

    companion object {
        private const val BIDRAGSMOTTAKER_REFERANSE = "Person_Bidragsmottaker"
        private const val BIDRAGSPLIKTIG_REFERANSE = "Person_Bidragspliktig"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }

    fun mapToBeregnGrunnlag(beregningRequestDto: BeregningRequestDto): List<BeregnGrunnlagMedAlder> {
        val fasteGrunnlagList = createFasteGrunnlagListe(beregningRequestDto)
        val beregningsperiode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1))

        return beregningRequestDto.barn.mapIndexed { index, søknadsbarn ->
            BeregnGrunnlagMedAlder(
                barnetsAlder = søknadsbarn.alder,
                beregnGrunnlag = BeregnGrunnlag(
                    periode = beregningsperiode,
                    søknadsbarnReferanse = "Person_Søknadsbarn_$index",
                    opphørSistePeriode = false,
                    stønadstype = Stønadstype.BIDRAG,
                    grunnlagListe = createGrunnlagListe(søknadsbarn, "Person_Søknadsbarn_$index") + fasteGrunnlagList
                )
            )

        }
    }

    private fun createFasteGrunnlagListe(beregningRequestDto: BeregningRequestDto) = listOf(
        createEmptyGrunnlag(BIDRAGSMOTTAKER_REFERANSE, Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
        createEmptyGrunnlag(BIDRAGSPLIKTIG_REFERANSE, Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
        createInntektGrunnlag("Inntekt_Bidragspliktig", beregningRequestDto.inntektForelder2.toBigDecimal(), BIDRAGSPLIKTIG_REFERANSE),
        createInntektGrunnlag("Inntekt_Bidragsmottaker", beregningRequestDto.inntektForelder1.toBigDecimal(), BIDRAGSMOTTAKER_REFERANSE),
        createBostatusGrunnlag("Bostatus_Bidragspliktig", Bostatuskode.BOR_MED_ANDRE_VOKSNE, null, BIDRAGSPLIKTIG_REFERANSE)
    )

    private fun createGrunnlagListe(søknadsbarn: BarnDto, søknadsbarnReferanse: String) = listOf(
        createSøknadsbarnGrunnlag(søknadsbarnReferanse, søknadsbarn),
        createInntektGrunnlag("Inntekt_$søknadsbarnReferanse", BigDecimal.ZERO, søknadsbarnReferanse),
        //TODO: bruk riktig verdi for gjelderReferanse som sier bosted til barn
        createBostatusGrunnlag("Bostatus_Søknadsbarn", Bostatuskode.IKKE_MED_FORELDER, søknadsbarnReferanse, BIDRAGSPLIKTIG_REFERANSE),
        createSamværsgrunnlag(søknadsbarn, søknadsbarnReferanse, BIDRAGSPLIKTIG_REFERANSE)
    )

    private fun createEmptyGrunnlag(referanse: String, type: Grunnlagstype) =
        GrunnlagDto(referanse, type, objectMapper.createObjectNode())

    private fun createSøknadsbarnGrunnlag(søknadsbarnReferanse: String, søknadsbarn: BarnDto) =
        GrunnlagDto(
            referanse = søknadsbarnReferanse,
            type = Grunnlagstype.PERSON_SØKNADSBARN,
            innhold = objectMapper.valueToTree(Person(fødselsdato = søknadsbarn.getEstimertFødselsdato()))
        )

    private fun createInntektGrunnlag(referanse: String, beløp: BigDecimal, eierReferanse: String) =
        GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            innhold = objectMapper.valueToTree(
                InntektsrapporteringPeriode(
                    periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(2)),
                    inntektsrapportering = Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                    beløp = beløp,
                    manueltRegistrert = true,
                    valgt = true
                )
            ),
            gjelderReferanse = eierReferanse
        )

    private fun createBostatusGrunnlag(referanse: String, bostatus: Bostatuskode, gjelderBarnReferanse: String?, gjelderReferanse: String? = null) =
        GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.BOSTATUS_PERIODE,
            innhold = objectMapper.valueToTree(
                (gjelderReferanse ?: gjelderBarnReferanse)?.let {
                    BostatusPeriode(
                        bostatus = bostatus,
                        periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(2)),
                        relatertTilPart = it,
                        manueltRegistrert = true
                    )
                }
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = gjelderReferanse
        )

    private fun createSamværsgrunnlag(søknadsbarn: BarnDto, gjelderBarnReferanse: String, gjelderReferanse: String) =
        GrunnlagDto(
            referanse = "Mottatt_Samværsperiode",
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = objectMapper.valueToTree(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(2)),
                    samværsklasse = søknadsbarn.samværsklasse,
                    manueltRegistrert = true
                )
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = gjelderReferanse
        )
}

data class BeregnGrunnlagMedAlder(
    val barnetsAlder: Int,
    val beregnGrunnlag: BeregnGrunnlag
)