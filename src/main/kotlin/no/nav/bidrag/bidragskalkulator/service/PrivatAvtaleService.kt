package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.CacheConfig
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.domene.ident.Personident
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PrivatAvtaleService(
    private val personConsumer: BidragPersonConsumer
) {

    @Cacheable(CacheConfig.PRIVAT_AVTALE_INFORMASJON)
    fun hentInformasjonForPrivatAvtale(ident: String): PrivatAvtaleInformasjonDto {
        logger.info { "Henter informasjon for privat avtale for en person" }

        val personInformasjon = personConsumer.hentPerson(Personident(ident))
        return personInformasjon.tilPrivatAvtaleInformasjonDto()
    }
}
