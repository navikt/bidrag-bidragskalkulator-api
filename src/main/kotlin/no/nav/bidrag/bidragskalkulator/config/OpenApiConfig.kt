import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

@Configuration
@EntityScan("no.nav.bidrag.bidragskalkulator.dto")
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP
)
@OpenAPIDefinition(
    info = io.swagger.v3.oas.annotations.info.Info(title = "bidrag-bidragskalkulator-api", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")]
)
@EnableSecurityConfiguration
@EnableRetry
class OpenApiConfig {

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