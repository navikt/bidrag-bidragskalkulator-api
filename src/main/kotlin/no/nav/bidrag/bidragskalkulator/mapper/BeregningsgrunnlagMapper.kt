package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.SjablonService
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.commons.service.sjablon.Sjablontall
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val TYPE_SJABLON_UTVIDET_BARNETRYGD_PER_MND = "0042"

@Component
class BeregningsgrunnlagMapper(
    private val beregningsgrunnlagBuilder: BeregningsgrunnlagBuilder,
    private val sjablonService: SjablonService
) {

    companion object Referanser {
        const val BIDRAGSMOTTAKER = "Person_Bidragsmottaker"
        const val BIDRAGSPLIKTIG = "Person_Bidragspliktig"
    }

    private fun kontantstøtteTilleggBm(barn: List<IFellesBarnDto>): BigDecimal {
        val sumPerMnd = barn.asSequence()
            .map { it.kontantstøtte ?: BigDecimal.ZERO }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        return sumPerMnd.multiply(BigDecimal.valueOf(12L))
    }


    fun mapTilBeregningsgrunnlag(dto: BeregningRequestDto): List<PersonBeregningsgrunnlag> {
        return dto.barn.mapIndexed { index, søknadsbarn ->
            val barnReferanse = "Person_Søknadsbarn_$index"
            val grunnlagListe = lagGrunnlagsliste(
                søknadsbarn,
                søknadsbarn.ident.fødselsdato(),
                dto,
                barnReferanse
                )

            PersonBeregningsgrunnlag(
                ident = søknadsbarn.ident,
                bidragsType = søknadsbarn.bidragstype,
                alder = kalkulerAlder(søknadsbarn.ident.fødselsdato()),
                grunnlag = beregningsgrunnlagBuilder
                    .byggFellesBeregnGrunnlag(barnReferanse, søknadsbarn.ident.fødselsdato(), grunnlagListe)
            )
        }
    }

    fun mapTilBeregningsgrunnlagAnonym(dto: ÅpenBeregningRequestDto): List<PersonBeregningsgrunnlagAnonym> {
        return dto.barn.mapIndexed { index, søknadsbarn ->
            val barnReferanse = "Person_Søknadsbarn_$index"
            val grunnlagListe = lagGrunnlagsliste(søknadsbarn,
                søknadsbarn.getEstimertFødselsdato(),
                dto, barnReferanse
            )

            PersonBeregningsgrunnlagAnonym(
                bidragsType = søknadsbarn.bidragstype,
                alder = søknadsbarn.alder,
                grunnlag = beregningsgrunnlagBuilder
                    .byggFellesBeregnGrunnlag(barnReferanse, søknadsbarn.getEstimertFødselsdato(), grunnlagListe)
            )
        }
    }

    private fun <T: IFellesBarnDto, R: FellesBeregningRequestDto<T>> lagGrunnlagsliste(
        søknadsbarn: T,
        fødselsdato: LocalDate,
        dto: R,
        barnReferanse: String
    ): List<GrunnlagDto> {

        val årligUtvidetBarnetrygd = beregnÅrligUtvidetBarnetrygd(
            sjablontall = sjablonService.hentSjablontall(),
            utvidetBarnetrygd = dto.utvidetBarnetrygd
        )

        val kontekst = BeregningKontekst(
            barnReferanse = barnReferanse,
            bidragstype = søknadsbarn.bidragstype,
            dittBoforhold = dto.dittBoforhold,
            medforelderBoforhold = dto.medforelderBoforhold,
            inntektForelder1 = dto.inntektForelder1,
            inntektForelder2 = dto.inntektForelder2,
            // kontantstøtte og årlig utvidet barnetrygd er en del av inntekten til bidragsmottaker,
            // uansett hvilket barn det gjelder
            kontantstøtte = kontantstøtteTilleggBm(dto.barn),
            årligUtvidetBarnetrygd = årligUtvidetBarnetrygd
        )

        return buildList{
            add(byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato))
            addAll(beregningsgrunnlagBuilder.byggInntektsgrunnlag(kontekst))
            beregningsgrunnlagBuilder.byggBarnInntektsgrunnlag(søknadsbarn, barnReferanse)?.let { add(it) }
            addAll(beregningsgrunnlagBuilder.byggBostatusgrunnlag(kontekst))
            søknadsbarn.barnetilsynsutgift?.let { add(beregningsgrunnlagBuilder.byggMottattFaktiskUtgift(fødselsdato, barnReferanse, it)) }
            add(beregningsgrunnlagBuilder.byggSamværsgrunnlag(søknadsbarn.samværsklasse, barnReferanse))
        }
    }

    fun mapTilBoOgForbruksutgiftsgrunnlag(fødselsdato: LocalDate, barnReferanse: String): BeregnGrunnlag {
        val grunnlagListe = buildList{
            add(byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato))
        }

        return beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, fødselsdato, grunnlagListe)
    }

    private fun byggGrunnlag(referanse: String, type: Grunnlagstype, fødselsdato: LocalDate? = null) =
        beregningsgrunnlagBuilder.byggPersongrunnlag(referanse, type, fødselsdato)

    private fun beregnÅrligUtvidetBarnetrygd(
        sjablontall: List<Sjablontall>,
        utvidetBarnetrygd: UtvidetBarnetrygdDto?,
    ): BigDecimal {
        if (utvidetBarnetrygd?.harUtvidetBarnetrygd != true) return BigDecimal.ZERO

        val perMåned = sjablontall
            .firstOrNull { it.typeSjablon == TYPE_SJABLON_UTVIDET_BARNETRYGD_PER_MND }
            ?.verdi
            ?: BigDecimal.ZERO

        val årlig = perMåned.multiply(BigDecimal.valueOf(12L))

        return if (utvidetBarnetrygd.delerMedMedforelder) {
            årlig.divide(BigDecimal.valueOf(2L))
        } else {
            årlig
        }
    }

}

data class PersonBeregningsgrunnlag(
    val ident: Personident,
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)

data class PersonBeregningsgrunnlagAnonym(
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)

data class BeregningKontekst(
    val barnReferanse: String,
    val inntektForelder1: Double,
    val inntektForelder2: Double,
    val bidragstype: BidragsType,
    val dittBoforhold: BoforholdDto?,
    val medforelderBoforhold: BoforholdDto?,
    val kontantstøtte: BigDecimal,
    val årligUtvidetBarnetrygd: BigDecimal
)
