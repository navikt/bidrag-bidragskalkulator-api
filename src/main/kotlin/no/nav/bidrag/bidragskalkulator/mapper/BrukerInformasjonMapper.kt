package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto


private var FORTROLIG_ADRESSE_DISKRESJONSKODER = listOf(
    Diskresjonskode.P19,
    Diskresjonskode.SPFO,
    Diskresjonskode.SPSF
);

object BrukerInformasjonMapper {

    fun tilBrukerInformasjonDto(
        motpartBarnRelasjondto: MotpartBarnRelasjonDto,
        inntektsGrunnlag: TransformerInntekterResponse?
    ): BrukerInformasjonDto {

        return BrukerInformasjonDto(
            person = motpartBarnRelasjondto.tilPersonInformasjonDto(),
            inntekt = inntektsGrunnlag?.toInntektResultatDto()?.inntektSiste12Mnd,
            barnerelasjoner = motpartBarnRelasjondto.personensMotpartBarnRelasjon
                .filterNot { it.motpart?.erDød() ?: false }
                .filterNot { it.motpart?.harFortroligAdresse() ?: false }
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart?.tilPersonInformasjonDto(),
                        fellesBarn = it.fellesBarn
                            .filterNot { barn -> barn.erDød() }
                            .filterNot { barn -> barn.harFortroligAdresse() }
                            .map { barn -> barn.tilPersonInformasjonDto() }
                            .sortedByDescending { barn -> barn.alder }
                    )
                }
                .filterNot { it.fellesBarn.isEmpty() }
        )
    }

    private fun PersonDto.erDød(): Boolean {
        return this.dødsdato != null
    }

    private fun PersonDto.harFortroligAdresse(): Boolean {
        return FORTROLIG_ADRESSE_DISKRESJONSKODER.contains(this.diskresjonskode)
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