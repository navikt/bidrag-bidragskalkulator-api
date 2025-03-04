package no.nav.bidrag.bidragskalkulator

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(BeregnBarnebidragApi::class)
class BidragBidragskalkulatorApiApplication

fun main(args: Array<String>) {
    runApplication<BidragBidragskalkulatorApiApplication>(*args)
}
