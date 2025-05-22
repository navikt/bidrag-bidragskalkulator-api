package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BarnInformasjonDto
import no.nav.bidrag.bidragskalkulator.dto.BarneRelasjonDto
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.dto.PersonInformasjonDto
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PersonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer,
    ) {
    val logger = LoggerFactory.getLogger(PersonService::class.java)

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)

    fun hentGyldigFamilierelasjon(personIdent: String): BrukerInformasjonDto {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)
        val gyldigeRelasjoner = familierelasjon.personensMotpartBarnRelasjon.filtrerRelasjonerMedGyldigMotpartOgBarn()

        return BrukerInformasjonDto(
            person = familierelasjon.person.tilPersonInformasjonDto(),
            barnerelasjoner = gyldigeRelasjoner
        )

    }

    private fun List<MotpartBarnRelasjon>.filtrerRelasjonerMedGyldigMotpartOgBarn(): List<BarneRelasjonDto> =
        this.asSequence()
            .mapNotNull { relasjon ->
                val motpart = relasjon.motpart
                if(motpart == null) {
                    logger.info("Fjerner relasjon hvor motpart == null")
                    // Hopp over denne relasjonen og ikke inkluder dette elementet i resultatet. Fortsett med resten
                    return@mapNotNull null
                }

                if (motpart.erDød() || motpart.harFortroligAdresse()) return@mapNotNull null
                if(relasjon.fellesBarn.isEmpty()) return@mapNotNull null

                val filtrerteBarn = relasjon.fellesBarn.filter { it.erLevendeOgIkkeSkjermet() }
                if (filtrerteBarn.isEmpty()) return@mapNotNull null

                BarneRelasjonDto(motpart.tilPersonInformasjonDto(), filtrerteBarn.tilBarnInformasjonDto())
            }
            .toList()
}

private fun PersonDto.tilPersonInformasjonDto(): PersonInformasjonDto =
    PersonInformasjonDto(
        ident = this.ident,
        fornavn = this.fornavn ?: "",
        fulltNavn = this.visningsnavn,
        alder = this.fødselsdato?.let { kalkulereAlder(it) } ?: 0
    )

private fun List<PersonDto>.tilBarnInformasjonDto(): List<BarnInformasjonDto> =
    this.map {
        BarnInformasjonDto(
            ident = it.ident,
            fornavn = it.fornavn ?: "",
            fulltNavn = it.visningsnavn,
            underholdskostnad = BigDecimal.ZERO,
            alder = it.fødselsdato?.let { kalkulereAlder(it) } ?: 0
        )
    }.sortedByDescending { it.alder }

private fun PersonDto.erLevendeOgIkkeSkjermet(): Boolean =
    this.dødsdato == null && !FORTROLIG_ADRESSE_DISKRESJONSKODER.contains(this.diskresjonskode)

private fun PersonDto.erDød(): Boolean = this.dødsdato != null

private fun PersonDto.harFortroligAdresse(): Boolean =
    this.diskresjonskode in FORTROLIG_ADRESSE_DISKRESJONSKODER

private val FORTROLIG_ADRESSE_DISKRESJONSKODER = listOf(
    Diskresjonskode.P19,
    Diskresjonskode.SPFO,
    Diskresjonskode.SPSF
)