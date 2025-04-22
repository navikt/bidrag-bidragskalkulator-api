package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersondetaljerDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

object BrukerInformasjonMapper {

    fun tilBrukerInformasjonDto(
        motpartBarnRelasjondto: MotpartBarnRelasjonDto,
        detaljertInformasjonDto: PersondetaljerDto,
        inntektsGrunnlag: TransformerInntekterResponse
    ): BrukerInfomasjonDto {
        return BrukerInfomasjonDto(
            påloggetPerson = detaljertInformasjonDto.tilPåloggetPersonDto(),
            barnRelasjon = motpartBarnRelasjondto.personensMotpartBarnRelasjon
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart?.tilPersonInformasjonDto(),
                        fellesBarn = it.fellesBarn.map { barn -> barn.tilPersonInformasjonDto() },
                    )
                },
            inntekt = inntektsGrunnlag.toInntektResultatDto() ?: InntektResultatDto(BigDecimal.ZERO, BigDecimal.ZERO)
        )
    }

    private fun PersonDto.tilPersonInformasjonDto(): PersonInformasjonDto {
        return PersonInformasjonDto(
            ident = this.ident,
            fornavn = this.fornavn ?: "",
            fulltNavn = this.visningsnavn,
            alder = this.fødselsdato?.let { kalkulereAlder(it) } ?: 0
        )
    }

    private fun PersondetaljerDto.tilPåloggetPersonDto(): PåloggetPersonDto {
        return PåloggetPersonDto(
            ident = this.person.ident,
            fornavn = this.person.fornavn ?: "",
            fulltNavn = this.person.visningsnavn,
            språkspreferanse = this.språk ?: Språk.NB.name
        )
    }

    private fun kalkulereAlder(fødselsdato: LocalDate): Int {
        return try {
            Period.between(fødselsdato, LocalDate.now()).years
        } catch (e: DateTimeParseException) {
            secureLogger.warn(e) { "Feil ved kalkulering av alder for fødselsdato $fødselsdato" }
            0
        }
    }
}