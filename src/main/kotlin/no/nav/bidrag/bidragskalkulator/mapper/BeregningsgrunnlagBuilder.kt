package no.nav.bidrag.bidragskalkulator.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.beregn.barnebidrag.bo.NettoTilsynsutgiftPeriode
import no.nav.bidrag.beregn.barnebidrag.bo.NettoTilsynsutgiftPeriodeGrunnlag
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper.Referanser
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
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
                referanse = "Bostatus_${prefix}_$index",
                bostatus = kode,
                gjelderBarnReferanse = "Barn_${prefix}_$index",
                gjelderReferanse = Referanser.BIDRAGSPLIKTIG
            )
        }

        val boforhold = if (data.bidragstype == BidragsType.PLIKTIG) data.dittBoforhold else data.medforelderBoforhold
        val bostatusBidragspliktig = if(boforhold?.borMedAnnenVoksen == true) Bostatuskode.BOR_MED_ANDRE_VOKSNE else Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE

        val bostatusBarn = buildList {
            boforhold?.antallBarnBorFast?.takeIf { it > 0 }?.let {
                addAll(barnGrunnlag(it, "med_forelder", Bostatuskode.MED_FORELDER))
            }
            boforhold?.antallBarnDeltBosted?.takeIf { it > 0 }?.let {
                addAll(barnGrunnlag(it, "delt_bosted", Bostatuskode.DELT_BOSTED))
            }
        }

        return buildList {
            add(nyttBostatusgrunnlag("Bostatus_Bidragspliktig", bostatusBidragspliktig, null, Referanser.BIDRAGSPLIKTIG))
            add(nyttBostatusgrunnlag("Bostatus_${data.barnReferanse}", Bostatuskode.IKKE_MED_FORELDER, data.barnReferanse, Referanser.BIDRAGSPLIKTIG))
            addAll(bostatusBarn)
        }
    }

    fun byggInntektsgrunnlag(data: BeregningKontekst): List<GrunnlagDto> {
        val erBidragspliktig = data.bidragstype == BidragsType.PLIKTIG
        val lønnBidragsmottaker = if (erBidragspliktig) data.inntektForelder2 else data.inntektForelder1
        val lønnBidragspliktig = if (erBidragspliktig) data.inntektForelder1 else data.inntektForelder2

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
            nyttInntektsgrunnlag("Inntekt_Bidragspliktig", lønnBidragspliktig.toBigDecimal(), Referanser.BIDRAGSPLIKTIG),
            nyttInntektsgrunnlag("Inntekt_Bidragsmottaker", lønnBidragsmottaker.toBigDecimal(), Referanser.BIDRAGSMOTTAKER),
            nyttInntektsgrunnlag("Inntekt_${data.barnReferanse}", BigDecimal.ZERO, data.barnReferanse)
        )
    }

    fun byggSamværsgrunnlag(samværsklasse: Samværsklasse, gjelderBarnReferanse: String): GrunnlagDto =
        GrunnlagDto(
            referanse = "Mottatt_Samværsperiode",
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
        referanse = "Mottatt_FaktiskUtgift",
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
}
