package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BeregningsgrunnlagMapper(
    private val beregningsgrunnlagBuilder: BeregningsgrunnlagBuilder
) {

    companion object Referanser {
        const val BIDRAGSMOTTAKER = "Person_Bidragsmottaker"
        const val BIDRAGSPLIKTIG = "Person_Bidragspliktig"
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
        val kontekst = BeregningKontekst(
            barnReferanse = barnReferanse,
            bidragstype = søknadsbarn.bidragstype,
            dittBoforhold = dto.dittBoforhold,
            medforelderBoforhold = dto.medforelderBoforhold,
            inntektForelder1 = dto.inntektForelder1,
            inntektForelder2 = dto.inntektForelder2
        )

        return buildList{
            add(byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato))
            addAll(beregningsgrunnlagBuilder.byggInntektsgrunnlag(kontekst))
            addAll(beregningsgrunnlagBuilder.byggBostatusgrunnlag(kontekst))
            add(beregningsgrunnlagBuilder.byggMottattFaktiskUtgift(fødselsdato, barnReferanse, søknadsbarn.barnetilsynsutgift))
            add(beregningsgrunnlagBuilder.byggSamværsgrunnlag(søknadsbarn.samværsklasse, barnReferanse))
        }
    }

    fun mapTilUnderholdkostnadsgrunnlag(fødselsdato: LocalDate, barnReferanse: String): BeregnGrunnlag {
        val grunnlagListe = buildList{
            add(byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, fødselsdato))
        }

        return beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, fødselsdato, grunnlagListe)
    }

    private fun byggGrunnlag(referanse: String, type: Grunnlagstype, fødselsdato: LocalDate? = null) =
        beregningsgrunnlagBuilder.byggPersongrunnlag(referanse, type, fødselsdato)

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
)