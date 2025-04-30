package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto

object BrukerInformasjonMapper {

    fun tilBrukerInformasjonDto(
        motpartBarnRelasjondto: MotpartBarnRelasjonDto,
        inntektsGrunnlag: TransformerInntekterResponse?
    ): BrukerInformasjonDto {

        return BrukerInformasjonDto(
            person = motpartBarnRelasjondto.tilPersonInformasjonDto(),
            inntekt = inntektsGrunnlag?.toInntektResultatDto()?.inntektSiste12Mnd,
            barnerelasjoner = motpartBarnRelasjondto.personensMotpartBarnRelasjon
                .filter { it.motpart?.dødsdato == null }
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart?.tilPersonInformasjonDto(),
                        fellesBarn = it.fellesBarn
                            // Ekskluder døde barn
                            .filter { barn -> barn.dødsdato == null }
                            // Ekskluder barn med strengt fortrolig adresse
                            .filterNot { barn ->
                                listOf(
                                    Diskresjonskode.P19,
                                    Diskresjonskode.SPFO,
                                    Diskresjonskode.SPSF
                                ).contains(barn.diskresjonskode)
                            }
                            .map { barn -> barn.tilPersonInformasjonDto() }
                            .sortedByDescending { barn -> barn.alder }
                    )
                }
                .filter { it.fellesBarn.isNotEmpty() }
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

    private fun MotpartBarnRelasjonDto.tilPersonInformasjonDto(): PersonInformasjonDto {
        return PersonInformasjonDto(
            ident = this.person.ident,
            fornavn = this.person.fornavn ?: "",
            fulltNavn = this.person.visningsnavn,
            alder = this.person.fødselsdato?.let { kalkulereAlder(it) } ?: 0
        )
    }
}