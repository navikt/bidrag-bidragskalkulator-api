package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


@Configuration
@Import(BeregnBarnebidragApi::class)
class BeregnBarnebidragConfig {
}