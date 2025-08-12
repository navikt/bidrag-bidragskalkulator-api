package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.*
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention

@EnableJwtTokenValidation
@Configuration
@EnableOAuth2Client(cacheEnabled = true)
@EnableSecurityConfiguration
@Import( BeregnBarnebidragApi::class)
class BeregnBarnebidragConfig {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()
}

object SecurityConstants {
    const val BEARER_KEY = "bearer-key"
    const val TOKENX = "tokenx"
}
