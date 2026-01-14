package no.nav.bidrag.bidragskalkulator.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.IFellesBarnDto
import no.nav.bidrag.bidragskalkulator.dto.VoksneOver18Type
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper.Referanser
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

private const val KAPITALINNTEKT_TERSKEL = 10_000

internal object BeregningsgrunnlagKonstant {
    const val BOSTATUS_BIDRAGSPLIKTIG = "Bostatus_Bidragspliktig"
    const val BOSTATUS_BARN_PREFIX = "Bostatus_"
    const val BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST = "egne_barn_under18_bor_fast"
    const val BOSTATUS_EGNE_BARN_OVER18_VGS = "egne_barn_over18_vgs"
    const val INNTEKT_BIDRAGSPLIKTIG = "Inntekt_Bidragspliktig"
    const val INNTEKT_BIDRAGSMOTTAKER = "Inntekt_Bidragsmottaker"
    const val INNTEKT_PREFIX = "Inntekt_"
    const val SAMVAERSPERIODE = "Mottatt_Samværsperiode"
    const val FAKTISK_UTGIFT = "Mottatt_FaktiskUtgift"
    const val BARNETILSYN_PREFIX = "Mottatt_Barnetilsyn_"
}

@Component
class BeregningsgrunnlagBuilder(
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
) {

    fun byggPersongrunnlag(referanse: String, type: Grunnlagstype, fødselsdato: LocalDate? = null) = GrunnlagDto(
        referanse = referanse,
        type = type,
        innhold = fødselsdato?.let { objectMapper.valueToTree(Person(fødselsdato = fødselsdato)) }
            ?: objectMapper.createObjectNode()
    )

    fun byggBostatusgrunnlag(data: BeregningKontekst): List<GrunnlagDto> {
        fun nyttBostatusgrunnlag(referanse: String, bostatus: Bostatuskode, gjelderBarnReferanse: String?, gjelderReferanse: String? = null) =
            GrunnlagDto(
                referanse = referanse,
                type = Grunnlagstype.BOSTATUS_PERIODE,
                innhold = objectMapper.valueToTree(
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

        fun barnGrunnlag(antall: Int, prefix: String, kode: Bostatuskode) = List(antall) { index ->
            nyttBostatusgrunnlag(
                referanse = "${BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX}${prefix}_$index",
                bostatus = kode,
                gjelderBarnReferanse = "Barn_${prefix}_$index",
                gjelderReferanse = Referanser.BIDRAGSPLIKTIG
            )
        }

        val boforhold = if (data.bidragstype == BidragsType.PLIKTIG) data.dittBoforhold else data.medforelderBoforhold
        val borMedSamboerEllerEktefelle = boforhold?.voksneOver18Type?.contains(VoksneOver18Type.SAMBOER_ELLER_EKTEFELLE) == true
        val bostatusBidragspliktig = if(borMedSamboerEllerEktefelle) Bostatuskode.BOR_MED_ANDRE_VOKSNE else Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE

        val bostatusBarnUnder18 = buildList {
            boforhold?.antallBarnUnder18BorFast?.takeIf { it > 0 }?.let {
                addAll(barnGrunnlag(it, BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_UNDER18_BOR_FAST, Bostatuskode.MED_FORELDER))
            }
        }

        val bostatusBarnOver18Vgs = buildList {
            boforhold
                ?.takeIf { it.voksneOver18Type?.contains(VoksneOver18Type.EGNE_BARN_OVER_18) == true }
                ?.antallBarnOver18Vgs
                ?.takeIf { it > 0 }
                ?.let {
                    addAll(barnGrunnlag(it, BeregningsgrunnlagKonstant.BOSTATUS_EGNE_BARN_OVER18_VGS, Bostatuskode.DOKUMENTERT_SKOLEGANG))
                }
        }

        return buildList {
            add(nyttBostatusgrunnlag(
                BeregningsgrunnlagKonstant.BOSTATUS_BIDRAGSPLIKTIG,
                bostatusBidragspliktig,
                null,
                Referanser.BIDRAGSPLIKTIG
            ))
            add(nyttBostatusgrunnlag(
                "${BeregningsgrunnlagKonstant.BOSTATUS_BARN_PREFIX}${data.barnReferanse}",
                Bostatuskode.IKKE_MED_FORELDER,
                data.barnReferanse,
                Referanser.BIDRAGSPLIKTIG
            ))
            addAll(bostatusBarnUnder18)
            addAll(bostatusBarnOver18Vgs)
        }
    }

    fun byggInntektsgrunnlag(data: BeregningKontekst): List<GrunnlagDto> {
        val bidragspliktigInntekt = data.bidragspliktigInntekt
        val bidragsmottakerInntekt = data.bidragsmottakerInntekt

        val bidragsmottakerNettoPositivKapitalinntekt = (bidragsmottakerInntekt.nettoPositivKapitalinntekt - KAPITALINNTEKT_TERSKEL.toBigDecimal())
            .coerceAtLeast(BigDecimal.ZERO)

        val inntektBidragsmottaker = bidragsmottakerInntekt.inntekt + bidragsmottakerNettoPositivKapitalinntekt
        val kontantstøtte = data.bmTilleggÅrlig.kontantstøtteÅrlig
        val utvidetBarnetrygd = data.bmTilleggÅrlig.utvidetBarnetrygdÅrlig
        val småbarnstillegg = data.bmTilleggÅrlig.småbarnstilleggÅrlig

        val samletInntektBidragsmottaker = inntektBidragsmottaker + kontantstøtte + utvidetBarnetrygd + småbarnstillegg

        val bidragspliktigNettoPositivKapitalinntekt = (bidragspliktigInntekt.nettoPositivKapitalinntekt - KAPITALINNTEKT_TERSKEL.toBigDecimal())
            .coerceAtLeast(BigDecimal.ZERO)

        val inntektBidragspliktig = bidragspliktigInntekt.inntekt + bidragspliktigNettoPositivKapitalinntekt

        fun nyttInntektsgrunnlag(referanse: String, beløp: BigDecimal, eierReferanse: String) =
            GrunnlagDto(
                referanse = referanse,
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold = objectMapper.valueToTree(
                    InntektsrapporteringPeriode(
                        periode = ÅrMånedsperiode(YearMonth.now(), null),
                        inntektsrapportering = Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                        beløp = beløp,
                        manueltRegistrert = true,
                        valgt = true
                    )
                ),
                gjelderReferanse = eierReferanse
            )

        return listOf(
            nyttInntektsgrunnlag(BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSPLIKTIG, inntektBidragspliktig, Referanser.BIDRAGSPLIKTIG),
            nyttInntektsgrunnlag(BeregningsgrunnlagKonstant.INNTEKT_BIDRAGSMOTTAKER, samletInntektBidragsmottaker, Referanser.BIDRAGSMOTTAKER),
        )
    }

    fun byggBarnInntektsgrunnlag(barn: IFellesBarnDto, referanse: String): GrunnlagDto? {
        return barn.inntekt?.let { beløp ->
                GrunnlagDto(
                    referanse = "${BeregningsgrunnlagKonstant.INNTEKT_PREFIX}$referanse",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = objectMapper.valueToTree(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), null),
                            inntektsrapportering = Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                            beløp = beløp,
                            manueltRegistrert = true,
                            valgt = true
                        )
                    ),
                    gjelderReferanse = referanse
                )
            }

    }

    fun byggSamværsgrunnlag(samværsklasse: Samværsklasse, gjelderBarnReferanse: String): GrunnlagDto =
        GrunnlagDto(
            referanse = BeregningsgrunnlagKonstant.SAMVAERSPERIODE,
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = objectMapper.valueToTree(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), null),
                    samværsklasse = samværsklasse,
                    manueltRegistrert = true
                )
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = Referanser.BIDRAGSPLIKTIG
        )

    fun byggFellesBeregnGrunnlag(barnReferanse: String, fødselsdato: LocalDate, grunnlagListe: List<GrunnlagDto>): BeregnGrunnlag {
        val barnetsAlder = kalkulerAlder(fødselsdato)

        return BeregnGrunnlag(
            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            søknadsbarnReferanse = barnReferanse,
            stønadstype = when {
                barnetsAlder >= 18 -> Stønadstype.BIDRAG18AAR
                else -> Stønadstype.BIDRAG
            },
            grunnlagListe = grunnlagListe
        )
    }

    fun byggMottattFaktiskUtgift(barnFødselsdato: LocalDate, barnReferanse: String, barnetilsynsutgift: BigDecimal): GrunnlagDto = GrunnlagDto(
        referanse = BeregningsgrunnlagKonstant.FAKTISK_UTGIFT,
        type = Grunnlagstype.FAKTISK_UTGIFT_PERIODE,
        innhold = objectMapper.valueToTree(
            FaktiskUtgiftPeriode(
                periode =
                ÅrMånedsperiode(YearMonth.now(), null),
                fødselsdatoBarn = barnFødselsdato,
                faktiskUtgiftBeløp = barnetilsynsutgift,
                kostpengerBeløp = BigDecimal.ZERO,
                manueltRegistrert = true,
            )
        ),
        gjelderBarnReferanse = barnReferanse,
        gjelderReferanse = Referanser.BIDRAGSMOTTAKER
    )

    fun byggMottattBarnetilsyn(barnReferanse: String, plassType: Tilsynstype): GrunnlagDto = GrunnlagDto(
        referanse = "${BeregningsgrunnlagKonstant.BARNETILSYN_PREFIX}$barnReferanse",
        type = Grunnlagstype.BARNETILSYN_MED_STØNAD_PERIODE,
        innhold = objectMapper.valueToTree(
            BarnetilsynMedStønadPeriode(
                periode =
                    ÅrMånedsperiode(YearMonth.now(), null),
                tilsynstype = plassType,
                skolealder = Skolealder.UNDER,
                manueltRegistrert = true,
            )
        ),
        gjelderBarnReferanse = barnReferanse,
        gjelderReferanse = Referanser.BIDRAGSMOTTAKER
    )
}
