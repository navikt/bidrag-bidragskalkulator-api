package no.nav.bidrag.bidragskalkulator.featureflag

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.bidragskalkulator.service.*
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestBeans {

    @Bean fun httpServletRequest(): HttpServletRequest =
        Mockito.mock(HttpServletRequest::class.java)

    @Bean fun beregningService(): BeregningService =
        Mockito.mock(BeregningService::class.java)

    @Bean fun bidragskalkulatorGrunnlagService(): BidragskalkulatorGrunnlagService =
        Mockito.mock(BidragskalkulatorGrunnlagService::class.java)

    @Bean fun privatAvtaleService(): PrivatAvtaleService =
        Mockito.mock(PrivatAvtaleService::class.java)

    @Bean fun privatAvtalePdfService(): PrivatAvtalePdfService =
        Mockito.mock(PrivatAvtalePdfService::class.java)

    @Bean fun safSelvbetjeningService(): SafSelvbetjeningService =
        Mockito.mock(SafSelvbetjeningService::class.java)

    @Bean fun innloggetBrukerUtils(): InnloggetBrukerUtils =
        Mockito.mock(InnloggetBrukerUtils::class.java)

    @Bean fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
