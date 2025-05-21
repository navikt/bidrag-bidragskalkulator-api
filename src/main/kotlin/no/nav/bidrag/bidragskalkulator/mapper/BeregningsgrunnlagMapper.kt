package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component

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

            PersonBeregningsgrunnlag(
                ident = søknadsbarn.ident,
                bidragsType = søknadsbarn.bidragstype,
                grunnlag = beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, søknadsbarn.ident, lagGrunnlagsliste(søknadsbarn, dto, barnReferanse))
            )
        }
    }

    private fun lagGrunnlagsliste(
        søknadsbarn: BarnDto,
        dto: BeregningRequestDto,
        barnReferanse: String
    ): List<GrunnlagDto> {
        val kontekst = Beregningskontekst(dto, søknadsbarn, barnReferanse)

        return buildList{
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, søknadsbarn.ident))
            addAll(beregningsgrunnlagBuilder.byggInntektsgrunnlag(kontekst))
            addAll(beregningsgrunnlagBuilder.byggBostatusgrunnlag(kontekst))
            add(beregningsgrunnlagBuilder.byggSamværsgrunnlag(søknadsbarn, barnReferanse))
        }
    }

    fun mapTilUnderholdkostnadsgrunnlag(søknadsbarnIdent: Personident, barnReferanse: String): BeregnGrunnlag {
        val grunnlagListe = buildList{
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(BIDRAGSMOTTAKER,Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(BIDRAGSPLIKTIG,Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(beregningsgrunnlagBuilder.byggPersongrunnlag(barnReferanse,Grunnlagstype.PERSON_SØKNADSBARN, søknadsbarnIdent))
        }

        return beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, søknadsbarnIdent, grunnlagListe)
    }
}

data class PersonBeregningsgrunnlag(
    val ident: Personident,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)

data class Beregningskontekst(
    val dto: BeregningRequestDto,
    val barn: BarnDto,
    val barnReferanse: String,
)