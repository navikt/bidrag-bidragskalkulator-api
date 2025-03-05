package no.nav.bidrag.bidragskalkulator.mapping

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.POJONode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.bidragskalkulator.controller.dto.EnkelBeregningRequestDto
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsperiodeGrunnlag
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.YearMonth

@Component
class BeregnGrunnlagMapper {
    companion object {
        const val BIDRAGSMOTTAKER_REFERANSE = "Person_Bidragsmottaker"
        const val BIDRAGSPLIKTIG_REFERANSE = "Person_Bidragspliktig"
    }

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val beregningsperiode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1))

    fun mapToBeregnGrunnlag(beregningRequestDto: EnkelBeregningRequestDto): List<BeregnGrunnlag> {
        return beregningRequestDto.barn.indices.map { index ->
            val søknadsbarnReferanse = "Person_Søknadsbarn_$index"

            BeregnGrunnlag(
                periode = beregningsperiode,
                søknadsbarnReferanse = søknadsbarnReferanse,
                opphørSistePeriode = false,
                stønadstype = Stønadstype.BIDRAG,
                grunnlagListe = createGrunnlagListe(beregningRequestDto, søknadsbarnReferanse)
            )
        }
    }

    private fun createGrunnlagListe(dto: EnkelBeregningRequestDto, søknadsbarnReferanse: String): List<GrunnlagDto> {
        return listOf(
            createEmptyGrunnlag(søknadsbarnReferanse, Grunnlagstype.PERSON_SØKNADSBARN),
            createEmptyGrunnlag(BIDRAGSMOTTAKER_REFERANSE, Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
            createEmptyGrunnlag(BIDRAGSPLIKTIG_REFERANSE, Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
            createInntektGrunnlag("Inntekt_Bidragspliktig", BigDecimal(dto.inntektForelder2), BIDRAGSPLIKTIG_REFERANSE),
            createInntektGrunnlag("Inntekt_Bidragsmottaker", BigDecimal(dto.inntektForelder1), BIDRAGSMOTTAKER_REFERANSE),
            createInntektGrunnlag("Inntekt_Barn", BigDecimal.ZERO, søknadsbarnReferanse),
//            TODO: bruk riktig verdi for gjelderReferanse som sier bosted til barn
            createBostatusGrunnlag("Bostatus_Søknadsbarn", Bostatuskode.IKKE_MED_FORELDER, søknadsbarnReferanse, BIDRAGSPLIKTIG_REFERANSE),
            createBostatusGrunnlag("Bostatus_Bidragspliktig", Bostatuskode.BOR_MED_ANDRE_VOKSNE, null,BIDRAGSPLIKTIG_REFERANSE),
            createSamværsgrunnlag(søknadsbarnReferanse, BIDRAGSPLIKTIG_REFERANSE)
        )
    }

    private fun createEmptyGrunnlag(referanse: String, type: Grunnlagstype): GrunnlagDto {
        return GrunnlagDto(referanse, type, objectMapper.createObjectNode())
    }

    private fun createInntektGrunnlag(referanse: String, beløp: BigDecimal, eierReferanse: String): GrunnlagDto {
        return GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            innhold = POJONode(
                InntektsrapporteringPeriode(
                    periode =  ÅrMånedsperiode(YearMonth.now(), null),
                    inntektsrapportering = Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                    beløp = beløp,
                    manueltRegistrert = true,
                    valgt = true
                )
            ),
            gjelderReferanse = eierReferanse
        )
    }

    private fun createBostatusGrunnlag(referanse: String, bostatus: Bostatuskode, gjelderBarnReferanse: String?, gjelderReferanse: String? = null): GrunnlagDto {
        return GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.BOSTATUS_PERIODE,
            innhold = POJONode(
                (gjelderReferanse ?: gjelderBarnReferanse)?.let {
                    BostatusPeriode(
                        bostatus = bostatus,
                        periode = ÅrMånedsperiode(YearMonth.now(), null),
                        relatertTilPart = it,
                        manueltRegistrert = true
                    )
                }
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = gjelderReferanse
        )
    }

    private fun createSamværsgrunnlag(gjelderBarnReferanse: String, gjelderReferanse: String): GrunnlagDto {
        return GrunnlagDto(
            referanse = "Mottatt_Samværsperiode",
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = POJONode(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), null),
//                    TODO: bruk samværsgrad i BarnDto
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
                    manueltRegistrert = true
                )
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = gjelderReferanse
        )
    }
}