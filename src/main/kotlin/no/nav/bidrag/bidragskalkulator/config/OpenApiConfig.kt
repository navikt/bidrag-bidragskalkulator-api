import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.Components
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import no.nav.bidrag.domene.enums.beregning.Samværsklasse

@Configuration
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
}