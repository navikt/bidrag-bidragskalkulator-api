package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

object BrukerInformasjonMapper {

    fun tilBrukerInformasjonDto(
        motpartBarnRelasjondto: MotpartBarnRelasjonDto,
        inntektsGrunnlag: TransformerInntekterResponse
    ): BrukerInfomasjonDto {

        return BrukerInfomasjonDto(
            påloggetPerson = motpartBarnRelasjondto.tilPåloggetPersonDto(),
            barnRelasjon = motpartBarnRelasjondto.personensMotpartBarnRelasjon
                .filter { it.motpart?.dødsdato == null }
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart?.tilPersonInformasjonDto(),
                        fellesBarn = it.fellesBarn
                            // Ekskluder døde barn
                            .filter { barn -> barn.dødsdato == null }
                            // Ekskluder barn med strengt fortrolig adresse
                            .filterNot { barn -> listOf(Diskresjonskode.P19, Diskresjonskode.SPFO, Diskresjonskode.SPSF).contains(barn.diskresjonskode) }
                            .map { barn -> barn.tilPersonInformasjonDto() }
                            .sortedByDescending { barn -> barn.alder }
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

    private fun MotpartBarnRelasjonDto.tilPåloggetPersonDto(): PåloggetPersonDto {
        return PåloggetPersonDto(
            ident = this.person.ident,
            fornavn = this.person.fornavn ?: "",
            fulltNavn = this.person.visningsnavn,
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