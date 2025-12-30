package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.SjablonService
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.commons.service.sjablon.Sjablontall
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private const val TYPE_SJABLON_UTVIDET_BARNETRYGD_PER_MND = "0042"
private const val TYPE_SJABLON_SMÅBARNSTILLEGG = "0032"

@Component
class BeregningsgrunnlagMapper(
    private val beregningsgrunnlagBuilder: BeregningsgrunnlagBuilder,
    private val sjablonService: SjablonService,
) {

    companion object Referanser {
        const val BIDRAGSMOTTAKER = "Person_Bidragsmottaker"
        const val BIDRAGSPLIKTIG = "Person_Bidragspliktig"
    }

    fun mapTilBeregningsgrunnlag(dto: BeregningRequestDto): List<PersonBeregningsgrunnlag> {
        val bmTilleggÅrlig = beregnBmTilleggÅrlig(dto)

        return dto.barn.mapIndexed { index, barn ->
            val barnReferanse = barnReferanse(index)
            val fødselsdato = barn.ident.fødselsdato()

            val grunnlagListe = lagGrunnlagsliste(
                barn = barn,
                fødselsdato = fødselsdato,
                dto = dto,
                barnReferanse = barnReferanse,
                bmTilleggÅrlig = bmTilleggÅrlig
            )

            PersonBeregningsgrunnlag(
                ident = barn.ident,
                bidragsType = barn.bidragstype,
                alder = kalkulerAlder(fødselsdato),
                grunnlag = beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, fødselsdato, grunnlagListe)
            )
        }
    }

    fun mapTilBeregningsgrunnlagAnonym(dto: ÅpenBeregningRequestDto): List<PersonBeregningsgrunnlagAnonym> {
        val bmTilleggÅrlig = beregnBmTilleggÅrlig(dto)

        return dto.barn.mapIndexed { index, barn ->
            val barnReferanse = barnReferanse(index)
            val fødselsdato = barn.getEstimertFødselsdato()

            val grunnlagListe = lagGrunnlagsliste(
                barn = barn,
                fødselsdato = fødselsdato,
                dto = dto,
                barnReferanse = barnReferanse,
                bmTilleggÅrlig = bmTilleggÅrlig
            )

            PersonBeregningsgrunnlagAnonym(
                alder = barn.alder,
                bidragsType = barn.bidragstype,
                grunnlag = beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, fødselsdato, grunnlagListe)
            )
        }
    }

    fun mapTilBoOgForbruksutgiftsgrunnlag(fødselsdato: LocalDate, barnReferanse: String): BeregnGrunnlag {
        val grunnlagListe = listOf(
            byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
            byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
            byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato),
        )
        return beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, fødselsdato, grunnlagListe)
    }

    private fun <T : IFellesBarnDto, R : FellesBeregningRequestDto<T>> lagGrunnlagsliste(
        barn: T,
        fødselsdato: LocalDate,
        dto: R,
        barnReferanse: String,
        bmTilleggÅrlig: BmTilleggÅrlig
    ): List<GrunnlagDto> {
        val kontekst = BeregningKontekst(
            barnReferanse = barnReferanse,
            bidragstype = barn.bidragstype,
            dittBoforhold = dto.dittBoforhold,
            medforelderBoforhold = dto.medforelderBoforhold,
            inntektForelder1 = dto.inntektForelder1,
            inntektForelder2 = dto.inntektForelder2,
            bmTilleggÅrlig = bmTilleggÅrlig
        )

        return buildList {
            add(byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato))

            addAll(beregningsgrunnlagBuilder.byggInntektsgrunnlag(kontekst))
            beregningsgrunnlagBuilder.byggBarnInntektsgrunnlag(barn, barnReferanse)?.let { add(it) }

            addAll(beregningsgrunnlagBuilder.byggBostatusgrunnlag(kontekst))

            barn.barnetilsynsutgift?.let {
                add(beregningsgrunnlagBuilder.byggMottattFaktiskUtgift(fødselsdato, barnReferanse, it))
            }

            add(beregningsgrunnlagBuilder.byggSamværsgrunnlag(barn.samværsklasse, barnReferanse))
        }
    }

    /** Beregner årlig tillegg for bidragsmottaker basert på kontantstøtte og utvidet barnetrygd for alle barn i beregningsgrunnlaget.
     * Kontantstøtte og årlig utvidet barnetrygd er en del av inntekten til bidragsmottaker, uansett hvilket barn det gjelder
     */
    private fun beregnBmTilleggÅrlig(dto: FellesBeregningRequestDto<*>): BmTilleggÅrlig {
        val kontantstøtteÅrlig =
            dto.barn.sumOf { it.kontantstøtte ?: BigDecimal.ZERO }
                .multiply(BigDecimal.valueOf(12L))

        val sjablontall = sjablonService.hentSjablontall()

        val utvidetBarnetrygdÅrlig = beregnÅrligUtvidetBarnetrygd(
            sjablontall = sjablontall,
            utvidetBarnetrygd = dto.utvidetBarnetrygd
        )

        val småbarnstillegg = hentSmåbarnstilleggÅrlig(sjablontall)

        return BmTilleggÅrlig(kontantstøtteÅrlig, utvidetBarnetrygdÅrlig, småbarnstillegg)
    }

    private fun barnReferanse(index: Int) = "Person_Søknadsbarn_$index"

    private fun byggGrunnlag(referanse: String, type: Grunnlagstype, fødselsdato: LocalDate? = null): GrunnlagDto =
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

    private fun hentSmåbarnstilleggÅrlig(sjablontall: List<Sjablontall>): BigDecimal {
        val månedlig = sjablontall
            .firstOrNull { it.typeSjablon == TYPE_SJABLON_SMÅBARNSTILLEGG }
            ?.verdi
            ?: BigDecimal.ZERO

        return månedlig.multiply(BigDecimal.valueOf(12L))
    }

}

data class PersonBeregningsgrunnlag(
    val ident: Personident,
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag,
)

data class PersonBeregningsgrunnlagAnonym(
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag,
)


data class BmTilleggÅrlig(
    val kontantstøtteÅrlig: BigDecimal,
    val utvidetBarnetrygdÅrlig: BigDecimal,
    val småbarnstilleggÅrlig: BigDecimal,
)

data class BeregningKontekst(
    val barnReferanse: String,
    val inntektForelder1: Double,
    val inntektForelder2: Double,
    val bidragstype: BidragsType,
    val dittBoforhold: BoforholdDto?,
    val medforelderBoforhold: BoforholdDto?,
    val bmTilleggÅrlig: BmTilleggÅrlig
)
