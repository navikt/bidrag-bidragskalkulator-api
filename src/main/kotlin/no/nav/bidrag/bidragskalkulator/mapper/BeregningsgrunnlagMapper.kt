package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.tilBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.tilBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
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

    fun mapTilBeregningsgrunnlagAnonym(dto: ÅpenBeregningRequestDto): List<PersonBeregningsgrunnlag> {
        return dto.barn.mapIndexed { index, søknadsbarn ->
            val barnReferanse = "Person_Søknadsbarn_$index"
            val barn = søknadsbarn.tilBarnDto()

            PersonBeregningsgrunnlag(
                ident = barn.ident,
                bidragsType = søknadsbarn.bidragstype,
                grunnlag = beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, barn.ident, lagGrunnlagsliste(barn, dto.tilBeregningRequestDto(), barnReferanse))
            )
        }
    }

    private fun byggPersongrunnlagListe(barnReferanse: String, søknadsbarnIdent: Personident) = listOf(
        byggGrunnlag(BIDRAGSMOTTAKER, Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
        byggGrunnlag(BIDRAGSPLIKTIG, Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
        byggGrunnlag(barnReferanse, Grunnlagstype.PERSON_SØKNADSBARN, søknadsbarnIdent)
    )

    private fun byggGrunnlag(referanse: String, type: Grunnlagstype, ident: Personident? = null) =
        beregningsgrunnlagBuilder.byggPersongrunnlag(referanse, type, ident)

    private fun lagGrunnlagsliste(
        søknadsbarn: BarnDto,
        dto: BeregningRequestDto,
        barnReferanse: String
    ): List<GrunnlagDto> {
        val kontekst = BeregningKontekst(dto, søknadsbarn, barnReferanse)

        return buildList{
            addAll(byggPersongrunnlagListe(barnReferanse, søknadsbarn.ident))
            addAll(beregningsgrunnlagBuilder.byggInntektsgrunnlag(kontekst))
            addAll(beregningsgrunnlagBuilder.byggBostatusgrunnlag(kontekst))
            add(beregningsgrunnlagBuilder.byggSamværsgrunnlag(søknadsbarn, barnReferanse))
        }
    }

    fun mapTilUnderholdkostnadsgrunnlag(søknadsbarnIdent: Personident, barnReferanse: String): BeregnGrunnlag {
        val grunnlagListe = byggPersongrunnlagListe(barnReferanse, søknadsbarnIdent)
        return beregningsgrunnlagBuilder.byggFellesBeregnGrunnlag(barnReferanse, søknadsbarnIdent, grunnlagListe)
    }
}

data class PersonBeregningsgrunnlag(
    val ident: Personident,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)

data class BeregningKontekst(
    val request: BeregningRequestDto,
    val barn: BarnDto,
    val barnReferanse: String,
)