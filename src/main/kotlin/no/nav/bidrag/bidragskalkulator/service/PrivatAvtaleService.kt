package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.domene.ident.Personident
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PrivatAvtaleService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer
) {
    val logger = LoggerFactory.getLogger(PrivatAvtaleService::class.java)

    @Cacheable(Cachenøkler.PRIVAT_AVTALE_INFORMASJON)
    fun hentInformasjonForPrivatAvtale(ident: String): PrivatAvtaleInformasjonDto {
        logger.info("Henter informasjon for privat avtale for en person")

        val personInformasjon = personConsumer.hentPerson(Personident(ident))
        return personInformasjon.tilPrivatAvtaleInformasjonDto()
    }
}