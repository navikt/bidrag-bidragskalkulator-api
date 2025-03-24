package no.nav.bidrag.bidragskalkulator.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@OpenAPIDefinition(
    info = io.swagger.v3.oas.annotations.info.Info(title = "bidrag-bidragskalkulator-api", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@EnableJwtTokenValidation
@Configuration
@Import(BeregnBarnebidragApi::class)
class BeregnBarnebidragConfig {

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

        @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearer-key",
                        SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    )
            ).info(Info().title("bidrag-bidragskalkulator-api").version("v1"))
    }

}