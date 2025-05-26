package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import java.math.BigDecimal

fun PersonDto.tilPersonInformasjonDto(): PersonInformasjonDto =
    PersonInformasjonDto(
        ident = this.ident,
        fornavn = this.fornavn ?: "",
        fulltNavn = this.visningsnavn,
        alder = this.fødselsdato?.let { kalkulereAlder(it) } ?: 0
    )

fun PersonDto.tilBarnInformasjonDto(underholdskostnad: BigDecimal?): BarnInformasjonDto =
    BarnInformasjonDto(
        ident = this.ident,
        fornavn = this.fornavn ?: "",
        fulltNavn = this.visningsnavn,
        underholdskostnad = underholdskostnad ?: BigDecimal.ZERO,
        alder = this.fødselsdato?.let { kalkulereAlder(it) } ?: 0
    )

fun PersonDto.erLevendeOgIkkeSkjermet(): Boolean =
    !this.erDød() && !harFortroligAdresse()

fun PersonDto.erDød(): Boolean = this.dødsdato != null

fun PersonDto.harFortroligAdresse(): Boolean =
    this.diskresjonskode in FORTROLIG_ADRESSE_DISKRESJONSKODER

private val FORTROLIG_ADRESSE_DISKRESJONSKODER = listOf(
    Diskresjonskode.P19,
    Diskresjonskode.SPFO,
    Diskresjonskode.SPSF
)