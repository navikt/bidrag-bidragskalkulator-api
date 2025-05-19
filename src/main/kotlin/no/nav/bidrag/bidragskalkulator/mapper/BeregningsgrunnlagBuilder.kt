package no.nav.bidrag.bidragskalkulator.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper.Referanser
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
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
        innhold = fødselsdato?.let { objectMapper.valueToTree(Person(fødselsdato = it)) }
            ?: objectMapper.createObjectNode()
    )

    fun byggBostatusgrunnlag(data: Beregningskontekst): List<GrunnlagDto> {
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

        val boforhold = if (data.barn.bidragstype == BidragsType.PLIKTIG) data.dto.dittBoforhold else data.dto.medforelderBoforhold
        val BPBostatus = if(boforhold?.borMedAnnenVoksen == true) Bostatuskode.BOR_MED_ANDRE_VOKSNE else Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE

        val bostatusBarn = buildList {
            boforhold?.antallBarnBorFast?.takeIf { it > 0 }?.let {
                addAll(barnGrunnlag(it, "med_forelder", Bostatuskode.MED_FORELDER))
            }
            boforhold?.antallBarnDeltBosted?.takeIf { it > 0 }?.let {
                addAll(barnGrunnlag(it, "delt_bosted", Bostatuskode.DELT_BOSTED))
            }
        }

        return buildList {
            add(nyttBostatusgrunnlag("Bostatus_Bidragspliktig", BPBostatus, null, Referanser.BIDRAGSPLIKTIG))
            add(nyttBostatusgrunnlag("Bostatus_${data.barnReferanse}", Bostatuskode.IKKE_MED_FORELDER, data.barnReferanse, Referanser.BIDRAGSPLIKTIG))
            addAll(bostatusBarn)
        }
    }

    fun byggInntektsgrunnlag(data: Beregningskontekst): List<GrunnlagDto> {
        val erBidragspliktig = data.barn.bidragstype == BidragsType.PLIKTIG
        val lønnBidragsmottaker = if (erBidragspliktig) data.dto.inntektForelder2 else data.dto.inntektForelder1
        val lønnBidragspliktig = if (erBidragspliktig) data.dto.inntektForelder1 else data.dto.inntektForelder2

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

    fun byggSamværsgrunnlag(søknadsbarn: BarnDto, gjelderBarnReferanse: String): GrunnlagDto =
        GrunnlagDto(
            referanse = "Mottatt_Samværsperiode",
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = objectMapper.valueToTree(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now(), null),
                    samværsklasse = søknadsbarn.samværsklasse,
                    manueltRegistrert = true
                )
            ),
            gjelderBarnReferanse = gjelderBarnReferanse,
            gjelderReferanse = Referanser.BIDRAGSPLIKTIG
        )
}