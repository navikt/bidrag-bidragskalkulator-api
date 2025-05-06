package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    CorrelationIdFilter::class,
    DefaultCorsFilter::class
)
class DefaultConfig {
}