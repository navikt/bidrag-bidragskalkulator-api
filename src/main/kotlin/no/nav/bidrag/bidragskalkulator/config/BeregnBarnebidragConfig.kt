package no.nav.bidrag.bidragskalkulator.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.*
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention

@OpenAPIDefinition(
    info = io.swagger.v3.oas.annotations.info.Info(title = "bidrag-bidragskalkulator-api", version = "v1"),
    security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)],
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    bearerFormat = "JWT",
    name = SecurityConstants.BEARER_KEY,
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@EnableJwtTokenValidation
@Configuration
@EnableOAuth2Client(cacheEnabled = true)
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class, AppContext::class, BeregnBarnebidragApi::class)
class BeregnBarnebidragConfig {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    @Bean
    fun customOpenApi(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi: OpenAPI ->
            val samværsklasseSchema = Schema<String>()
                .description("Samværsklasse for barnet")
                .example(Samværsklasse.SAMVÆRSKLASSE_1.name)
                .apply {
                    enum = Samværsklasse.entries.map { it.name }
                }

            openApi.components = (openApi.components ?: Components())
                .addSchemas("Samværsklasse", samværsklasseSchema)
        }
    }
}

object SecurityConstants {
    const val BEARER_KEY = "bearer-key"
    const val TOKENX = "tokenx"
}
