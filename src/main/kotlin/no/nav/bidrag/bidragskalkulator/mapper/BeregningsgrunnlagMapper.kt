package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class BeregningsgrunnlagMapper(private val personService: PersonService, private val grunnlagBuilder: BeregningsgrunnlagBuilder) {

    object Referanser {
        const val BIDRAGSMOTTAKER = "Person_Bidragsmottaker"
        const val BIDRAGSPLIKTIG = "Person_Bidragspliktig"
    }

    fun mapTilBeregningsgrunnlag(dto: BeregningRequestDto): List<GrunnlagOgBarnInformasjon> {

        val beregningsperiode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1))

        return dto.barn.mapIndexed { index, søknadsbarn ->
            val barnetsInformasjon = personService.hentPersoninformasjon(søknadsbarn.ident)
            val barnetsAlder = kalkulereAlder(søknadsbarn.ident.fødselsdato())
            val barnReferanse = "Person_Søknadsbarn_$index"

            GrunnlagOgBarnInformasjon(
                ident = søknadsbarn.ident,
                fulltNavn = barnetsInformasjon.visningsnavn,
                fornavn = barnetsInformasjon.fornavn ?: barnetsInformasjon.visningsnavn,
                alder = barnetsAlder,
                bidragsType = søknadsbarn.bidragstype,
                grunnlag = BeregnGrunnlag(
                    periode = beregningsperiode,
                    søknadsbarnReferanse = barnReferanse,
                    stønadstype = when {
                        barnetsAlder >= 18 -> Stønadstype.BIDRAG18AAR
                        else -> Stønadstype.BIDRAG
                    },
                    grunnlagListe = lagGrunnlagsliste(søknadsbarn,  dto, barnReferanse)
                )
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
            add(grunnlagBuilder.byggPersongrunnlag(Referanser.BIDRAGSMOTTAKER,Grunnlagstype.PERSON_BIDRAGSMOTTAKER))
            add(grunnlagBuilder.byggPersongrunnlag(Referanser.BIDRAGSPLIKTIG,Grunnlagstype.PERSON_BIDRAGSPLIKTIG))
            add(grunnlagBuilder.byggPersongrunnlag(barnReferanse,Grunnlagstype.PERSON_SØKNADSBARN, søknadsbarn.ident.fødselsdato()))
            addAll(grunnlagBuilder.byggInntektsgrunnlag(kontekst))
            addAll(grunnlagBuilder.byggBostatusgrunnlag(kontekst))
            add(grunnlagBuilder.byggSamværsgrunnlag(søknadsbarn, barnReferanse))
        }
    }
}

data class GrunnlagOgBarnInformasjon(
    val ident: Personident,
    val fulltNavn: String,
    val fornavn: String,
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)

data class Beregningskontekst(
    val dto: BeregningRequestDto,
    val barn: BarnDto,
    val barnReferanse: String,
)