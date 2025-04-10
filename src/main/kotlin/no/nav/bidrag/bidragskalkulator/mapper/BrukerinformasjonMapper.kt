package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import java.time.LocalDate
import java.time.Period

object BrukerInformasjonMapper {

    fun tilBrukerInformasjonDto(dto: MotpartBarnRelasjonDto): BrukerInfomasjonDto {
        return BrukerInfomasjonDto(
            paaloggetPerson = dto.person.toPersonDto(),
            barnRelasjon = dto.personensMotpartBarnRelasjon
                .filter { it.motpart != null }
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart!!.toPersonDto(),
                        fellesBarn = it.fellesBarn.map { barn -> barn.toPersonDto() }
                    )
                }
        )
    }

    private fun no.nav.bidrag.transport.person.PersonDto.toPersonDto(): PersonDto {
        return PersonDto(
            ident = this.ident,
            fornavn = this.fornavn ?: "",
            visningsnavn = this.visningsnavn,
            alder = this.fødselsdato?.let { kalkulereAlder(it) } ?: 0
        )
    }

    private fun kalkulereAlder(fødselsdato: LocalDate): Int {
        return try {
            Period.between(fødselsdato, LocalDate.now()).years
        } catch (e: Exception) {
            secureLogger.warn(e) { "Feil ved kalkulering av alder for fødselsdato $fødselsdato" }
            0
        }
    }
}