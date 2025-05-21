package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.service.BarnUnderholdskostnad
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import java.math.BigDecimal


private var FORTROLIG_ADRESSE_DISKRESJONSKODER = listOf(
    Diskresjonskode.P19,
    Diskresjonskode.SPFO,
    Diskresjonskode.SPSF
);

object BrukerInformasjonMapper {

    val logger = LoggerFactory.getLogger(BrukerInformasjonMapper::class.java)

    fun tilBrukerInformasjonDto(
        motpartBarnRelasjondto: MotpartBarnRelasjonDto,
        barnUnderholdkostnad:  List<BarnUnderholdskostnad>,
        inntektsGrunnlag: TransformerInntekterResponse?
    ): BrukerInformasjonDto {

        return BrukerInformasjonDto(
            person = motpartBarnRelasjondto.tilPersonInformasjonDto(),
            inntekt = inntektsGrunnlag?.toInntektResultatDto()?.inntektSiste12Mnd,
            barnerelasjoner = motpartBarnRelasjondto.personensMotpartBarnRelasjon
                .filterNot {
                    if (it.motpart == null) {
                        // logger for innhenting av statistikk
                        logger.info("Fjerner relasjon hvor motpart == null")
                        true
                    } else {
                        false
                    }
                }
                .filterNot { it.motpart?.erDød() ?: false }
                .filterNot { it.motpart?.harFortroligAdresse() ?: false }
                .map {
                    BarneRelasjonDto(
                        motpart = it.motpart?.tilPersonInformasjonDto(),
                        fellesBarn = it.fellesBarn
                            .filterNot { barn -> barn.erDød() }
                            .filterNot { barn -> barn.harFortroligAdresse() }
                            .map { barn ->
                                val barnetUnderholdskostnad = barnUnderholdkostnad.find { it.barnIdent == barn.ident}?.underholdskostnad
                                barn.tilBarnInformasjonDto(barnetUnderholdskostnad)
                            }.sortedByDescending { barn -> barn.alder }
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

    private fun PersonDto.tilBarnInformasjonDto(underholdskostnad: BigDecimal?): BarnInformasjonDto {
        return BarnInformasjonDto(
            ident = this.ident,
            fornavn = this.fornavn ?: "",
            fulltNavn = this.visningsnavn,
            underholdskostnad = underholdskostnad ?: BigDecimal.ZERO,
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