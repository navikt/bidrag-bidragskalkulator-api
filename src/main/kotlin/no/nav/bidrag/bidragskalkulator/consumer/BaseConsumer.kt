package no.nav.bidrag.bidragskalkulator.consumer

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.springframework.web.client.RestTemplate

abstract class BaseConsumer(restTemplate: RestTemplate, name: String) : AbstractRestClient(restTemplate, name) {
    protected fun <T : Any> medApplikasjonsKontekst(fn: () -> T): T {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            fn()
        }
    }
}
