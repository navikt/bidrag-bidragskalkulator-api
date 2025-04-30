package no.nav.bidrag.bidragskalkulator.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Component
class BeregningsgrunnlagMapper(private val personService: PersonService) {

    companion object {
        private const val BIDRAGSMOTTAKER_REFERANSE = "Person_Bidragsmottaker"
        private const val BIDRAGSPLIKTIG_REFERANSE = "Person_Bidragspliktig"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }

    fun mapTilBeregningsgrunnlag(dto: BeregningRequestDto): List<GrunnlagOgBarnInformasjon> {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1))

        return dto.barn.mapIndexed { index, søknadsbarn ->
            val barnetsInformasjon = SikkerhetsKontekst.medApplikasjonKontekst {
                personService.hentNavnFoedselDoed(søknadsbarn.ident)
            }
            val barnetsAlder = kalkulereAlder(søknadsbarn.ident.fødselsdato())
            val søknadsbarnReferanse = "Person_Søknadsbarn_$index"

            GrunnlagOgBarnInformasjon(
                ident = søknadsbarn.ident,
                fulltNavn = barnetsInformasjon.navn,
                alder = barnetsAlder,
                bidragsType = søknadsbarn.bidragstype,
                grunnlag = BeregnGrunnlag(
                    periode = beregningsperiode,
                    søknadsbarnReferanse = søknadsbarnReferanse,
                    stønadstype = if(barnetsAlder >= 18) Stønadstype.BIDRAG18AAR else Stønadstype.BIDRAG,
                    grunnlagListe = lagGrunnlagsliste(søknadsbarn,  dto, søknadsbarnReferanse)
                )
            )
        }
    }

    private fun lagGrunnlagsliste(søknadsbarn: BarnDto, dto: BeregningRequestDto, søknadsbarnReferanse: String): List<GrunnlagDto> {
        val erBidragspliktig = søknadsbarn.bidragstype == BidragsType.PLIKTIG
        val lønnBidragsmottaker = if (erBidragspliktig) dto.inntektForelder2 else dto.inntektForelder1
        val lønnBidragspliktig = if (erBidragspliktig) dto.inntektForelder1 else dto.inntektForelder2

        return listOf(
            lagTomtGrunnlag(BIDRAGSMOTTAKER_REFERANSE, Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
            lagTomtGrunnlag(BIDRAGSPLIKTIG_REFERANSE, Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
            lagBostatusgrunnlag("Bostatus_Bidragspliktig", Bostatuskode.BOR_IKKE_MED_ANDRE_VOKSNE, null, BIDRAGSPLIKTIG_REFERANSE),
            lagSøknadsbarngrunnlag(søknadsbarnReferanse, søknadsbarn.ident.fødselsdato()),
            lagInntektsgrunnlag(
                "Inntekt_Bidragspliktig",
                lønnBidragspliktig.toBigDecimal(),
                BIDRAGSPLIKTIG_REFERANSE
            ),
            lagInntektsgrunnlag(
                "Inntekt_Bidragsmottaker",
                lønnBidragsmottaker.toBigDecimal(),
                BIDRAGSMOTTAKER_REFERANSE
            ),
            lagInntektsgrunnlag("Inntekt_$søknadsbarnReferanse", BigDecimal.ZERO, søknadsbarnReferanse),
            lagBostatusgrunnlag(
                "Bostatus_$søknadsbarnReferanse",
                Bostatuskode.IKKE_MED_FORELDER,
                søknadsbarnReferanse,
                BIDRAGSPLIKTIG_REFERANSE
            ),
            lagSamværsgrunnlag(søknadsbarn, søknadsbarnReferanse, BIDRAGSPLIKTIG_REFERANSE)
        )
    }

    private fun lagTomtGrunnlag(referanse: String, type: Grunnlagstype) =
        GrunnlagDto(referanse, type, objectMapper.createObjectNode())

    private fun lagSøknadsbarngrunnlag(søknadsbarnReferanse: String, søknadsbarnsfødselsdato: LocalDate) =
        GrunnlagDto(
            referanse = søknadsbarnReferanse,
            type = Grunnlagstype.PERSON_SØKNADSBARN,
            innhold = objectMapper.valueToTree(Person(fødselsdato = søknadsbarnsfødselsdato))
        )

    private fun lagInntektsgrunnlag(referanse: String, beløp: BigDecimal, eierReferanse: String) =
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

    private fun lagBostatusgrunnlag(referanse: String, bostatus: Bostatuskode, gjelderBarnReferanse: String?, gjelderReferanse: String? = null) =
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

    private fun lagSamværsgrunnlag(søknadsbarn: BarnDto, gjelderBarnReferanse: String, gjelderReferanse: String) =
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
            gjelderReferanse = gjelderReferanse
        )
}

data class GrunnlagOgBarnInformasjon(
    val ident: Personident,
    val fulltNavn: String,
    val alder: Int,
    val bidragsType: BidragsType,
    val grunnlag: BeregnGrunnlag
)